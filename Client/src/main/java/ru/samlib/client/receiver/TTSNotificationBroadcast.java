package ru.samlib.client.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import ru.samlib.client.R;
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.service.TTSService;
import ru.samlib.client.util.AndroidSystemUtils;
import ru.samlib.client.util.TTSPlayer;

/**
 * Created by Dmitry on 02.09.2015.
 */
public class TTSNotificationBroadcast extends BroadcastReceiver {

    private static final String TAG = TTSNotificationBroadcast.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (!TTSService.getInstance().getState().equals(TTSPlayer.State.PAUSE)) {
                        sendMessage(TTSService.Action.PAUSE);
                    } else {
                        sendMessage(TTSService.Action.PLAY);
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    Log.d(TAG, "TAG: KEYCODE_MEDIA_NEXT");
                    sendMessage(TTSService.Action.NEXT);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    Log.d(TAG, "TAG: KEYCODE_MEDIA_PREVIOUS");
                    sendMessage(TTSService.Action.PRE);
                    break;
            }
        } else {
            if (intent.getAction().equals(TTSService.NOTIFY_PLAY)) {
                sendMessage(TTSService.Action.PLAY);
            } else if (intent.getAction().equals(TTSService.NOTIFY_POSITION)) {
                sendMessage(TTSService.Action.POSITION, intent.getIntExtra(Constants.ArgsName.TTS_PLAY_POSITION, 0));
            } else if (intent.getAction().equals(TTSService.NOTIFY_STOP)) {
                sendMessage(TTSService.Action.STOP);
            } else if (intent.getAction().equals(TTSService.NOTIFY_PAUSE)) {
                sendMessage(TTSService.Action.PAUSE);
            } else if (intent.getAction().equals(TTSService.NOTIFY_NEXT)) {
                sendMessage(TTSService.Action.NEXT);
            } else if (intent.getAction().equals(TTSService.NOTIFY_DELETE)) {
                Intent i = new Intent(context, TTSService.class);
                context.stopService(i);
            } else if (intent.getAction().equals(TTSService.NOTIFY_PREVIOUS)) {
                sendMessage(TTSService.Action.PRE);
            }
        }
    }

    public String ComponentName() {
        return this.getClass().getName();
    }


    public static void sendMessage(TTSService.Action action) {
        try {
            if (TTSPlayer.TTS_HANDLER != null) {
                TTSPlayer.TTS_HANDLER.sendMessage(TTSPlayer.TTS_HANDLER.obtainMessage(action.ordinal(), ""));
            }
        } catch (Exception e) {
        }
    }

    public static void sendMessage(TTSService.Action action, Object data) {
        try {
            if (TTSPlayer.TTS_HANDLER != null) {
                TTSPlayer.TTS_HANDLER.sendMessage(TTSPlayer.TTS_HANDLER.obtainMessage(action.ordinal(), data));
            }
        } catch (Exception e) {
        }
    }
}
