package ru.samlib.client.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import ru.kazantsev.template.util.SystemUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.parser.Parser;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.receiver.TTSNotificationBroadcast;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.util.TTSPlayer;
import ru.kazantsev.template.util.TextUtils;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by Dmitry on 01.09.2015.
 */
public class TTSService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = TTSService.class.getSimpleName();

    public enum Action {
        PLAY, STOP, PAUSE, POSITION, NEXT, PRE;
    }

    private TTSPlayer ttsp;
    private static final int NOTIFICATION_ID = 1111;
    public static final String NOTIFY_PREVIOUS = "ru.samlib.client.previous";
    public static final String NOTIFY_DELETE = "ru.samlib.client.delete";
    public static final String NOTIFY_PAUSE = "ru.samlib.client.pause";
    public static final String NOTIFY_STOP = "ru.samlib.client.stop";
    public static final String NOTIFY_PLAY = "ru.samlib.client.play";
    public static final String NOTIFY_POSITION = "ru.samlib.client.position";
    public static final String NOTIFY_NEXT = "ru.samlib.client.next";

    private ComponentName remoteComponentName;
    private RemoteControlClient remoteControlClient;
    private AudioManager audioManager;
    private static boolean currentVersionSupportBigNotification = false;
    private static boolean currentVersionSupportLockScreenControls = false;

    private static TTSService instance;

    private static TTSPlayer.OnIndexSpeakFinished indexSpeakFinished;
    private static TTSPlayer.OnNextPhraseListener nextPhraseListener;
    private static TTSPlayer.OnTTSPlayerStateChanged stateChanged;

    @Inject
    DatabaseService databaseService;

    public static TTSService getInstance() {
        return instance;
    }

    public static boolean isReady(Work work) {
        return instance != null
                && instance.getPlayer() != null
                && !instance.getPlayer().getState().equals(TTSPlayer.State.UNAVAILABLE)
                && instance.getPlayer().getWork() != null
                && ((work.isNotSamlib() && work.getTitle() != null && work.getTitle().equals(instance.getPlayer().getWork().getTitle()))
                || (!work.isNotSamlib() && work.getLink().equals(instance.getPlayer().getWork().getLink())));
    }

    public TTSPlayer.State getState() {
        if(ttsp != null) {
            return ttsp.getState();
        } else {
            return TTSPlayer.State.IDLE;
        }
    }

    public TTSPlayer getPlayer() {
        return ttsp;
    }

    public static void setNextPhraseListener(TTSPlayer.OnNextPhraseListener nextPhraseListener) {
        TTSService.nextPhraseListener = nextPhraseListener;
        if(instance != null && instance.getPlayer() != null) {
            instance.getPlayer().setOnNextPhraseListener(nextPhraseListener);
        }
    }

    public static void setIndexSpeakFinished(TTSPlayer.OnIndexSpeakFinished indexSpeakFinished) {
        TTSService.indexSpeakFinished = indexSpeakFinished;
        if(instance != null && instance.getPlayer() != null) {
            instance.getPlayer().setOnIndexSpeakFinished(indexSpeakFinished);
        }
    }

    public static void setOnPlayerStateChanged(TTSPlayer.OnTTSPlayerStateChanged stateChanged) {
        TTSService.stateChanged = stateChanged;
        if(instance != null && instance.getPlayer() != null) {
            instance.getPlayer().setOnTTSPlayerStateChanged(stateChanged);
        }
    }

    public void setPlayer(TTSPlayer ttsp) {
        this.ttsp = ttsp;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        instance = this;
        App.getInstance().getComponent().inject(this);
        if(ttsp == null) {
            ttsp = new TTSPlayer(this);
        }
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        currentVersionSupportBigNotification = AndroidSystemUtils.currentVersionSupportBigNotification();
        currentVersionSupportLockScreenControls = AndroidSystemUtils.currentVersionSupportLockScreenControls();
        super.onCreate();
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            final String link =  intent.getStringExtra(Constants.ArgsName.LINK);
            Work work = WorkParser.getCachedWork(link);
            if(!work.isParsed()) {
                if (work.getRawContent() == null) {
                    File cached;
                    if(work.isNotSamlib()) {
                        cached = new File(link);
                    } else {
                        cached = HtmlClient.getCachedFile(getBaseContext(), link);
                    }
                    if(cached.exists()) {
                        work.setRawContent(SystemUtils.readFile(cached, "CP1251"));
                    }
                }
                if (!TextUtils.isEmpty(work.getRawContent())) {
                    WorkParser.processChapters(work);
                } else {
                    return START_STICKY_COMPATIBILITY;
                }
            }
            if (currentVersionSupportLockScreenControls) {
                RegisterRemoteClient();
            }
            ttsp.setOnIndexSpeakFinished(indexSpeakFinished);
            ttsp.setOnNextPhraseListener(nextPhraseListener);
            ttsp.setOnTTSPlayerStateChanged(stateChanged);
            ttsp.setSpeechRate(intent.getFloatExtra(Constants.ArgsName.TTS_SPEECH_RATE, 1.3f));
            ttsp.setPitch(intent.getFloatExtra(Constants.ArgsName.TTS_PITCH, 1f));
            if(intent.hasExtra(Constants.ArgsName.TTS_LANGUAGE)) {
                ttsp.setLanguage(intent.getStringExtra(Constants.ArgsName.TTS_LANGUAGE));
            }
            playWork(work, intent.getStringExtra(Constants.ArgsName.TTS_PLAY_POSITION));
            newNotification(work.isNotSamlib() ? "file://" + link : link);
            TTSPlayer.TTS_HANDLER = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (ttsp == null)
                        return false;
                    Action action = Action.values()[msg.what];
                    switch (action) {
                        case PLAY:
                            if (currentVersionSupportLockScreenControls) {
                                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                            }
                            ttsp.resume();
                            break;
                        case STOP:
                            if (currentVersionSupportLockScreenControls) {
                                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                            }
                            ttsp.stop();
                            break;
                        case PAUSE:
                            if (currentVersionSupportLockScreenControls) {
                                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                            }
                            ttsp.pause();
                            break;
                        case POSITION:
                            if (currentVersionSupportLockScreenControls) {
                                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                            }
                            String [] pos = ((String) msg.obj).split(":");
                            ttsp.startSpeak(Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
                            break;
                        case NEXT:
                            if (ttsp.isOver()) {
                                //TODO next work
                            } else {
                                ttsp.next();
                            }
                            break;
                        case PRE:
                            ttsp.pre();
                            break;
                    }
                    newNotification(link);
                    Log.d(TAG, "TAG Pressed: " + action);
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Unexpeted error in TTS service", e);
        }
        return START_REDELIVER_INTENT;
    }

    /**
     * Notification
     * Custom Bignotification is available from API 16
     */
    @SuppressLint("NewApi")
    private void newNotification(String link) {
        String title = ttsp.getWork().getTitle();
        String shortName = ttsp.getWork().getAuthor().getShortName();
        RemoteViews simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.tts_notification);
        Intent launch = new Intent(getApplicationContext(), SectionActivity.class);
        launch.setData(Uri.parse(link));
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_action_book)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, launch, 0))
                .setCustomContentView(simpleContentView)
                .setContentTitle(title).build();

        setListeners(simpleContentView);

        if (ttsp.getState().equals(TTSPlayer.State.PAUSE) || ttsp.getState().equals(TTSPlayer.State.STOPPED)) {
            notification.contentView.setViewVisibility(R.id.btnPause, View.GONE);
            notification.contentView.setViewVisibility(R.id.btnPlay, View.VISIBLE);
        } else {
            notification.contentView.setViewVisibility(R.id.btnPause, View.VISIBLE);
            notification.contentView.setViewVisibility(R.id.btnPlay, View.GONE);
        }

        notification.contentView.setTextViewText(R.id.notification_work_title, title);
        notification.contentView.setTextViewText(R.id.notification_work_author, shortName);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Notification click listeners
     *
     * @param view
     */
    public void setListeners(RemoteViews view) {
        Intent previous = new Intent(NOTIFY_PREVIOUS);
        Intent delete = new Intent(NOTIFY_DELETE);
        Intent pause = new Intent(NOTIFY_PAUSE);
        Intent next = new Intent(NOTIFY_NEXT);
        Intent play = new Intent(NOTIFY_PLAY);

        PendingIntent pPrevious = PendingIntent.getBroadcast(getApplicationContext(), 0, previous, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPrevious, pPrevious);

        PendingIntent pDelete = PendingIntent.getBroadcast(getApplicationContext(), 0, delete, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnStop, pDelete);

        PendingIntent pPause = PendingIntent.getBroadcast(getApplicationContext(), 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPause, pPause);

        PendingIntent pNext = PendingIntent.getBroadcast(getApplicationContext(), 0, next, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnNext, pNext);

        PendingIntent pPlay = PendingIntent.getBroadcast(getApplicationContext(), 0, play, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPlay, pPlay);
    }

    @Override
    public void onDestroy() {
        if (ttsp != null) {
            ttsp.onStop();
            ttsp = null;
        }
        super.onDestroy();
    }

    /**
     * Play song, Update Lockscreen fields
     *
     * @param work
     */
    @SuppressLint("NewApi")
    private void playWork(Work work, String position) {
        if (currentVersionSupportLockScreenControls) {
            UpdateMetadata(work);
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
        ttsp.onStop();
        String[] pos = position.split(":");
        ttsp.playOnStart(work, Integer.valueOf(pos[0]), Integer.valueOf(pos[1]));
        ttsp.onStart();
    }

    @SuppressLint("NewApi")
    private void RegisterRemoteClient() {
        remoteComponentName = new ComponentName(getApplicationContext(), new TTSNotificationBroadcast().ComponentName());
        try {
            if (remoteControlClient == null) {
                audioManager.registerMediaButtonEventReceiver(remoteComponentName);
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(remoteComponentName);
                PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                audioManager.registerRemoteControlClient(remoteControlClient);
            }
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
        } catch (Exception ex) {
        }
    }

    @SuppressLint("NewApi")
    private void UpdateMetadata(Work work) {
        if (remoteControlClient == null)
            return;
        RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
        if(work.getCategory() != null) {
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, work.getCategory().getTitle());
        } else {
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "");
        }
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, work.getAuthor().getFullName());
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, work.getTitle());
        metadataEditor.apply();
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
    }

}
