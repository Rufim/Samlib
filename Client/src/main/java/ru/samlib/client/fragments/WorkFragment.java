package ru.samlib.client.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.support.annotation.IdRes;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.*;
import android.widget.SeekBar;
import android.widget.TextView;
import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.FontResolver;
import net.nightwhistler.htmlspanner.SystemFontResolver;
import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.activity.BaseActivity;
import ru.kazantsev.template.dialog.DirectoryChooserDialog;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.*;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.samlib.client.dialog.EditListPreferenceDialog;
import ru.samlib.client.dialog.ListChooseDialog;
import ru.samlib.client.dialog.OnCommit;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.ChapterSelectedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.parser.Parser;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.receiver.TTSNotificationBroadcast;
import ru.samlib.client.service.DatabaseService;
import ru.samlib.client.service.TTSService;
import ru.samlib.client.util.*;
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<String> implements View.OnClickListener {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private ExternalWork externalWork;
    private Queue<Pair<Integer, Integer>> searched = new ArrayDeque<>();
    private Integer lastIndent = 0;
    private int colorSpeakingText;
    private int colorFoundedText;
    private Mode mode = Mode.NORMAL;
    private boolean ownTTSService = false;
    private boolean isDownloaded = false;
    private boolean isSeekBarVisible = false;
    private boolean isWaitingForSkipStart = false;
    private boolean isWaitingPlayerCallback = false;
    private boolean isFullscreen = false;
    private boolean isBack = false;
    private boolean isStopped = true;
    private SeekBar autoScrollSpeed;
    private SeekBar pitch;
    private SeekBar speechRate;
    private ViewGroup speedLayout;
    private ViewGroup speakLayout;
    private CountDownTimer autoScroller;

    private View decorView;

    private long timer = 0;
    private long pressed = 0;
    private boolean second = false;

    private int lastOffset = 0;

    @Inject
    DatabaseService databaseService;

    private enum Mode {
        SEARCH, SPEAK, NORMAL, AUTO_SCROLL
    }

    public static WorkFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, WorkFragment.class);
    }

    public static WorkFragment showFile(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.FILE_PATH, link), container, WorkFragment.class);
    }

    public static WorkFragment showContent(FragmentBuilder builder, @IdRes int container, Uri uri) {
        return show(builder.putArg(Constants.ArgsName.CONTENT_URI, uri), container, WorkFragment.class);
    }

    public static WorkFragment show(FragmentBuilder builder, @IdRes int container, Work work) {
        return show(builder.putArg(Constants.ArgsName.WORK, work), container, WorkFragment.class);
    }

    public static WorkFragment show(BaseFragment fragment, String link) {
        return show(fragment, WorkFragment.class, Constants.ArgsName.LINK, link);
    }

    public static WorkFragment show(BaseFragment fragment, Work work) {
        return show(fragment, WorkFragment.class, Constants.ArgsName.WORK, work);
    }

    public WorkFragment() {
        enableSearch = true;
        enableScrollbar = true;
        setDataSource(((skip, size) -> {
            if (skip != 0) return null;
            TTSPlayer.getAvailableLanguages(getContext());
            while (work == null) {
                SystemClock.sleep(100);
            }
            if (!work.isParsed()) {
                if (externalWork != null) {
                    if (externalWork.getContentUri() != null) {
                        FileDescriptor descriptor = getContext().getContentResolver().openFileDescriptor(externalWork.getContentUri(), "r").getFileDescriptor();
                        File cachedFile = new File(externalWork.getFilePath());
                        if (cachedFile.exists()) {
                            cachedFile.delete();
                        }
                        if (cachedFile.getParentFile().mkdirs() || cachedFile.createNewFile()) {
                            FileInputStream in = null;
                            FileOutputStream out = null;
                            try {
                                in = new FileInputStream(descriptor);
                                out = new FileOutputStream(cachedFile);
                                SystemUtils.copy(in, out);
                            } finally {
                                SystemUtils.close(in);
                                SystemUtils.close(out);
                            }
                            SavedHtml savedHtml = new SavedHtml();
                            savedHtml.setFilePath(cachedFile.getAbsolutePath());
                            savedHtml.setSize(cachedFile.length());
                            savedHtml.setUpdated(new Date());
                            databaseService.insertOrUpdateSavedHtml(savedHtml);
                            externalWork.setContentUri(null);
                        } else {
                            throw new IOException("Cant create content file on path:" + cachedFile.getAbsolutePath());
                        }
                    }
                    File externalFile = new File(externalWork.getFilePath());
                    AtomicInteger gained = new AtomicInteger(-1);
                    if (!externalFile.canRead()) {
                        getBaseActivity().doActionWithPermission(Manifest.permission.READ_EXTERNAL_STORAGE, new BaseActivity.PermissionAction() {
                            @Override
                            public void doAction(boolean permissionGained) {
                                gained.set(permissionGained ? 1 : 0);
                            }
                        });
                    } else {
                        gained.set(1);
                    }
                    while (gained.get() == -1) {
                        SystemUtils.sleepQuietly(100);
                    }
                    if (gained.get() == 0 || !externalFile.exists()) {
                        throw new IOException();
                    }
                    work = WorkParser.parse(new File(externalWork.getFilePath()), "CP1251", work, true);
                    if (work.getBookmark() == null) {
                        setBookmark(work, "", 0);
                    }
                } else {
                    work = new WorkParser(work).parse(true, false);
                    work.setCachedDate(new Date());
                    if (work.getBookmark() == null) {
                        setBookmark(work, "", 0);
                    }
                    databaseService.insertOrUpdateBookmark(work.getBookmark());
                    Work workEntity = databaseService.getWork(work.getLink());
                    if (workEntity != null) {
                        workEntity.setSizeDiff(0);
                        workEntity.setChanged(false);
                        databaseService.doAction(DatabaseService.Action.UPDATE, workEntity);
                    }
                    if (isAdded()) {
                        GuiUtils.runInUI(getContext(), (v) -> progressBarText.setText(R.string.work_parse));
                        WorkParser.processChapters(work, false);
                        GuiUtils.runInUI(getContext(), (v) -> {
                            if (searchView != null) searchView.setEnabled(true);
                        });
                    } else {
                        return new ArrayList<>();
                    }
                }
            }
            if (work.isParsed()) {
                postEvent(new WorkParsedEvent(work));
                return work.getIndents();
            } else {
                return new ArrayList<>();
            }
        }));
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        if (ex instanceof IOException) {
            if (Parser.isCachedMode() || externalWork != null) {
                if (externalWork == null) {
                    ErrorFragment.show(this, R.string.work_not_in_cache, 0, ex);
                } else {
                    ErrorFragment.show(this, R.string.work_cant_open, 0, ex);
                }
            } else {
                ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
            }
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            if (work != null && work.getLink() != null) {
                ACRA.getErrorReporter().handleException(new Exception("Unhandled exception occurred while parse author by url: " + work.getLink(), ex));
            } else {
                ACRA.getErrorReporter().handleException(ex);
            }
        }
    }

    @Override
    public void startLoading(boolean showProgress) {
        progressBarText.setText(R.string.loading);
        super.startLoading(showProgress);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        if (work != null && getContext() != null && (AndroidSystemUtils.getMemory(getContext()) / Math.pow(1024, 2)) - 100 < 100) {
            work.setParsed(false);
            work.getIndents().clear();
            work.setRawContent(null);
        }
        super.onDestroyView();
    }

    @Override
    protected void firstLoad(boolean scroll) {
        try {
            Bookmark bookmark = databaseService.getBookmark(work.isNotSamlib() ? externalWork.getFilePath() : work.getFullLink());
            work.setBookmark(bookmark);
            if (dataSource != null && !isEnd && adapter.getItems().isEmpty()) {
                loadMoreBar.setVisibility(View.GONE);
                if (bookmark != null && scroll && bookmark.getIndentIndex() != 0) {
                    scrollToIndex(bookmark.getIndentIndex(), Integer.MIN_VALUE);
                } else {
                    loadItems(false);
                }
            } else {
                stopLoading();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
        }
    }

    private void setBookmark(Work work, String indent, Integer index) {
        Bookmark bookmark = work.getBookmark();
        if (bookmark == null) {
            bookmark = new Bookmark(indent);
        } else {
            bookmark.setIndent(indent);
        }
        bookmark.setIndentIndex(index);
        if (externalWork == null || externalWork.getWorkUrl() != null) {
            bookmark.setWorkUrl(work.getFullLink());
            bookmark.setAuthorUrl(work.getAuthor().getFullLink());
        } else {
            bookmark.setWorkUrl(externalWork.getFilePath());
        }
        bookmark.setAuthorShortName(work.getAuthor().getShortName());
        bookmark.setGenres(work.printGenres());
        bookmark.setWorkTitle(work.getTitle());
        work.setBookmark(bookmark);
    }


    @Override
    public void onStart() {
        super.onStart();
        isStopped = false;
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPostLoadItems() {
        if (work != null) {
            isDownloaded = work.isParsed();
        }
        safeInvalidateOptionsMenu();
        PreferenceMaster master = new PreferenceMaster(getContext());
        boolean firstTime = master.getValue(R.string.preferenceNavigationWork, true);
        if (firstTime) {
            getBaseActivity().openDrawer();
            master.putValue(R.string.preferenceNavigationWork, false);
        }
    }

    private void saveCurrentPosition(int index) {
        if (work != null && work.isParsed()) {
            try {
                int size = adapter.getItems().size();
                if (size > index) {
                    setBookmark(work, adapter.getItems().get(index), index);
                    databaseService.insertOrUpdateBookmark(work.getBookmark());
                }
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception", e);
            }
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        isStopped = true;
        saveCurrentPosition(findFirstVisibleItemPosition(false));
        // sanity check for null as this is a public method
        if (isAdded()) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
        editor.putInt(getString(R.string.preferenceWorkSpeechRate), speechRate.getProgress());
        editor.putInt(getString(R.string.preferenceWorkAutoScrollSpeed), autoScrollSpeed.getProgress());
        editor.putInt(getString(R.string.preferenceWorkPitch), pitch.getProgress());
        editor.putString(getString(R.string.preferenceLastWork), isBack ? "" : work.isNotSamlib() ? "file://" + externalWork.getFilePath() : work.getLink());
        editor.apply();
        stopAutoScroll();
        stopFullscreen();
        isBack = false;
    }


    @Override
    public boolean allowBackPress() {
        if (isFullscreen) {
            stopFullscreen();
            return false;
        }
        isBack = true;
        return super.allowBackPress();
    }

    @Override
    public void refreshData(boolean showProgress) {
        work.setParsed(false);
        ((WorkFragmentAdaptor) adapter).refreshSettings(getContext());
        super.refreshData(showProgress);
    }

    public List<Pair<Integer, Integer>> search(String query) {
        query = query.toLowerCase();
        List<Pair<Integer, Integer>> indexes = new ArrayList<>();
        for (int i = findFirstVisibleItemPosition(false); i < adapter.getItems().size(); i++) {
            final String text = adapter.getItems().get(i).toLowerCase();
            if (text.contains(query)) {
                for (TextUtils.Piece piece : TextUtils.searchAll(text, query)) {
                    indexes.add(new Pair(i, piece.start));
                }
            }
        }
        return indexes;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isDownloaded) {
            inflater.inflate(R.menu.work, menu);
            if (!work.isHasComments()) {
                menu.removeItem(R.id.action_work_comments);
            }
            if (!work.isHasRate()) {
                menu.removeItem(R.id.action_work_rate);
            }
            if (work.isNotSamlib()) {
                menu.removeItem(R.id.action_work_to_author);
                menu.removeItem(R.id.action_work_share);
            }
            if (externalWork != null) {
                menu.removeItem(R.id.action_work_save);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                menu.removeItem(R.id.action_work_fullscreen);
            }
            if (mode.equals(Mode.SPEAK)) {
                speakLayout.setVisibility(VISIBLE);
                safeCheckMenuItem(R.id.action_work_speaking, true);
            } else {
                safeCheckMenuItem(R.id.action_work_speaking, false);
                speakLayout.setVisibility(GONE);
            }
            if (mode.equals(Mode.AUTO_SCROLL)) {
                speedLayout.setVisibility(VISIBLE);
                safeCheckMenuItem(R.id.action_work_auto_scroll, true);
            } else {
                safeCheckMenuItem(R.id.action_work_auto_scroll, false);
                speedLayout.setVisibility(GONE);
            }
            if (TTSPlayer.getAvailableLanguages(getContext()).isEmpty()) {
                menu.removeItem(R.id.action_work_speaking);
                menu.removeItem(R.id.action_work_speaking_language);
            }
            if (work.getBookmark().isUserBookmark()) {
                safeCheckMenuItem(R.id.action_work_lock_bookmark, true);
            }
        } else {
            menu.clear();
        }
        if (!work.isParsed()) {
            searchView.setEnabled(false);
        }
        searchView.setQueryHint(getString(R.string.search_hint));
    }

    @Override
    public void onSearchViewClose(SearchView searchView) {
        adapter.setLastQuery(null);
        clearSelection();
        mode = Mode.NORMAL;
    }


    private float getRate(SeekBar seekBar) {
        return ((float) seekBar.getProgress() / 100f);
    }

    private void startAutoScroll() {
        final long totalScrollTime = Long.MAX_VALUE; //total scroll time. I think that 300 000 000 years is close enouth to infinity. if not enought you can restart timer in onFinish()
        final int scrollPeriod = 20; // every 20 ms scoll will happened. smaller values for smoother
        final int heightToScroll = 20; // will be scrolled to 20 px every time. smaller values for smoother scrolling
        if (autoScroller != null) autoScroller.cancel();
        autoScroller = new CountDownTimer(totalScrollTime, scrollPeriod) {
            public void onTick(long millisUntilFinished) {
                itemList.scrollBy(0, (int) (heightToScroll * (getRate(autoScrollSpeed) * 0.27)));
            }

            public void onFinish() {
                //you can add code for restarting timer here
            }
        };
        itemList.post(() -> {
            autoScroller.start();
        });
        lockOrientation();
    }

    private void stopAutoScroll() {
        if (autoScroller != null) {
            autoScroller.cancel();
        }
        autoScroller = null;
        releaseOrientation();
    }

    public void playAutoScroll() {
        startAutoScroll();
        GuiUtils.setVisibility(VISIBLE, speedLayout, R.id.btnPause);
        GuiUtils.setVisibility(GONE, speedLayout, R.id.btnPlay);
    }

    public void pauseAutoScroll() {
        stopAutoScroll();
        GuiUtils.setVisibility(GONE, speedLayout, R.id.btnPause);
        GuiUtils.setVisibility(VISIBLE, speedLayout, R.id.btnPlay);
    }

    public void cancelAutoScroll() {
        pauseAutoScroll();
        speedLayout.setVisibility(GONE);
        safeCheckMenuItem(R.id.action_work_auto_scroll, false);
    }


    public void stopSpeak(boolean stopService) {
        if (TTSService.isReady(work)) {
            TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
        }
        clearSelection();
        if (isAdded() && stopService) {
            safeCheckMenuItem(R.id.action_work_speaking, false);
            Intent i = new Intent(getContext(), TTSService.class);
            getContext().stopService(i);
        }
        speakLayout.setVisibility(View.GONE);
        GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPlay);
        GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPause);
        releaseOrientation();
    }

    public void startSpeak(int position, int offset) {
        ownTTSService = true;
        WorkFragment.this.selectText(lastIndent, null);
        Intent i = new Intent(getActivity(), TTSService.class);
        i.putExtra(Constants.ArgsName.LINK, work.isNotSamlib() ? externalWork.getFilePath() : work.getLink());
        i.putExtra(Constants.ArgsName.TTS_PLAY_POSITION, position + ":" + offset);
        i.putExtra(Constants.ArgsName.TTS_SPEECH_RATE, getRate(speechRate));
        i.putExtra(Constants.ArgsName.TTS_PITCH, getRate(pitch));
        i.putExtra(Constants.ArgsName.TTS_LANGUAGE, AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceVoiceLanguage, "ru"));
        if (isAdded()) {
            getActivity().startService(i);
            lockOrientation();
        }
    }

    public void clearSelection() {
        adapter.selectText(null, true, 0);
    }


    public boolean isPaused() {
        return speakLayout.findViewById(R.id.btnPlay).getVisibility() == VISIBLE;
    }

    private void syncState(TTSPlayer.State state) {
        isWaitingPlayerCallback = false;
        switch (state) {
            case SPEAKING:
                if (isPaused()) {
                    lockOrientation();
                }
                GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPlay);
                GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPause);
                break;
            case END:
            case UNAVAILABLE:
                safeCheckMenuItem(R.id.action_work_speaking, false);
                stopSpeak(false);
            case STOPPED:
            case PAUSE:
                GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPlay);
                GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPause);
                saveCurrentPosition(lastIndent);
                break;
        }
    }

    private void initFragmentForSpeak() {
        if (!TTSService.isReady(work)) {
            TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
        }
        TTSService.setNextPhraseListener(new TTSPlayer.OnNextPhraseListener() {
            @Override
            public synchronized void onNextPhrase(int speakIndex, int phraseIndex, List<TTSPlayer.Phrase> phrases) {
                if (WorkFragment.this.getActivity() != null && TTSService.isReady(work)) {
                    TTSPlayer.Phrase phrase = phrases.get(phraseIndex);
                    lastIndent = speakIndex;
                    TextView textView = WorkFragment.this.getTextViewIndent(speakIndex);
                    if (phrase == null) return;
                    lastOffset = phrase.start;
                    if (textView != null) {
                        int visibleLines = WorkFragment.this.getVisibleLines(textView);
                        Layout layout = textView.getLayout();
                        if (layout == null || visibleLines < layout.getLineForOffset(phrase.end) + 1) {
                            WorkFragment.this.scrollToIndex(speakIndex, phrase.start);
                        }
                        WorkFragment.this.selectText(speakIndex, phrase.start, phrase.end);
                        isWaitingForSkipStart = false;
                    } else if (isAdded() && !isStopped) {
                        clearSelection();
                        WorkFragment.this.scrollToIndex(speakIndex, Integer.MIN_VALUE);
                        itemList.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onNextPhrase(speakIndex, phraseIndex, phrases);
                            }
                        }, 200);
                    }
                }
            }
        });
        TTSService.setIndexSpeakFinished(speakIndex -> {
            if (getActivity() != null && TTSService.isReady(work)) {
                selectText(speakIndex, null);
            }
        });
        TTSService.setOnPlayerStateChanged(state -> {
            itemList.post(() -> {
                syncState(state);
            });
        });
        speakLayout.setVisibility(VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_work_auto_scroll:
                if (item.isChecked()) {
                    mode = Mode.NORMAL;
                    cancelAutoScroll();
                } else {
                    if (Mode.SPEAK.equals(mode)) {
                        stopSpeak(true);
                    }
                    clearSelection();
                    mode = Mode.AUTO_SCROLL;
                    item.setChecked(true);
                    speedLayout.setVisibility(VISIBLE);
                }
                return true;
            case R.id.action_work_speaking:
                if (item.isChecked()) {
                    mode = Mode.NORMAL;
                    stopSpeak(true);
                } else {
                    if (Mode.AUTO_SCROLL.equals(mode)) {
                        cancelAutoScroll();
                    }
                    mode = Mode.SPEAK;
                    item.setChecked(true);
                    clearSelection();
                    initFragmentForSpeak();
                }
                return true;
            case R.id.action_work_speaking_language:
                EditListPreferenceDialog editListPreferenceDialog = new EditListPreferenceDialog();
                SettingsFragment.Preference preference = new SettingsFragment.Preference(getContext(), R.string.preferenceVoiceLanguage, "ru");
                preference.keyValue = TTSPlayer.getAvailableLanguages(getContext());
                editListPreferenceDialog.setPreference(preference);
                editListPreferenceDialog.setOnCommit((value, d) -> {
                    safeInvalidateOptionsMenu();
                    return true;
                });
                editListPreferenceDialog.show(getFragmentManager(), editListPreferenceDialog.getClass().getSimpleName());
                return true;
            case R.id.action_work_to_author:
                AuthorFragment.show(new FragmentBuilder(getFragmentManager()), getBaseActivity().getContainer().getId(), work.getAuthor());
            case R.id.action_work_lock_bookmark:
                boolean checked = !item.isChecked();
                item.setChecked(checked);
                work.getBookmark().setUserBookmark(checked);
                work.getBookmark().save();
                return true;
            case R.id.action_work_share:
                AndroidSystemUtils.shareText(getActivity(), work.getAuthor().getShortName(), work.getTitle(), work.getFullLink(), "text/plain");
                return true;
            case R.id.action_work_save:
                if (isAdded()) {
                    getBaseActivity().doActionWithPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, permissionGained -> {
                        if (permissionGained) {
                            String path = AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceLastSavedWorkPath, Environment.getExternalStorageDirectory().getAbsolutePath());
                            DirectoryChooserDialog chooserDialog = new DirectoryChooserDialog(getActivity());
                            chooserDialog.setSourceDirectory(path);
                            chooserDialog.setTitle("Сохранить в...");
                            chooserDialog.setIcon(android.R.drawable.ic_menu_save);
                            chooserDialog.setAllowRootDir(true);
                            chooserDialog.setFileTypes("html");
                            chooserDialog.setOnChooseFileListener(chosenFile -> {
                                if (chosenFile != null) {
                                    try {
                                        File file = new File(chosenFile, work.getTitle() + ".html");
                                        File original;
                                        if (work.getCachedResponse() != null) {
                                            original = work.getCachedResponse();
                                        } else {
                                            original = HtmlClient.getCachedFile(getContext(), work.getLink());
                                        }
                                        SystemUtils.copy(original, file);
                                        SharedPreferences.Editor preferences = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                                        preferences.putString(getString(R.string.preferenceLastSavedWorkPath), file.getParent());
                                        preferences.apply();
                                        databaseService.saveExternalWork(work, file.getAbsolutePath());
                                    } catch (Exception e) {
                                        Log.e(TAG, "Unknown exception", e);
                                    }
                                }
                            });
                            chooserDialog.show();
                        }
                    });
                }
                return true;
            case R.id.action_work_open_with:
                try {
                    File workFile;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Activity activity = getActivity();
                    if (externalWork != null) {
                        workFile = new File(externalWork.getFilePath());
                    } else if (work.getCachedResponse() != null) {
                        workFile = work.getCachedResponse();
                    } else {
                        workFile = HtmlClient.getCachedFile(getContext(), work.getLink());
                    }
                    Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", workFile);
                    intent.setDataAndType(uri, workFile.getName().endsWith("html") ? "text/html" : "text/plain");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Unknown exception", e);
                }
                return true;
            case R.id.action_work_comments:
                CommentsPagerFragment.show(newFragmentBuilder()
                        .addToBackStack()
                        .setAnimation(R.anim.slide_in_left, R.anim.slide_out_right)
                        .setPopupAnimation(R.anim.slide_in_right, R.anim.slide_out_left), getId(), work.getLink());
                return true;
            case R.id.action_work_rate:
                ListChooseDialog<Integer> chooseDialog = new ListChooseDialog<>();
                LinkedHashMap<Integer, String> map = new LinkedHashMap();
                String[] ratingVals = getResources().getStringArray(R.array.work_rating);
                for (int i = 0; i < ratingVals.length; i++) {
                    map.put(i, ratingVals[i]);
                }
                chooseDialog.setSelected(0);
                chooseDialog.setValues(map);
                chooseDialog.setOnCommit(new OnCommit<Integer, ListChooseDialog>() {
                    @Override
                    public boolean onCommit(Integer value, ListChooseDialog dialog) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (WorkParser.sendRate(work, value)) {
                                    if (isAdded()) {
                                        PreferenceMaster master = new PreferenceMaster(getContext());
                                        String vote = master.getValue(R.string.preferenceVoteCoockie, "0");
                                        if (!vote.equals(Parser.getVoteCookie())) {
                                            master.putValue(R.string.preferenceVoteCoockie, Parser.getVoteCookie());
                                        }
                                    }
                                }
                            }
                        }).start();
                        return true;
                    }
                });
                chooseDialog.show(getFragmentManager(), ListChooseDialog.class.getName());
                return true;
            case R.id.action_work_fullscreen:
                if (isAdded()) {
                    enableFullscreen();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void enableFullscreen() {
        isFullscreen = true;
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        decorView.setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    // Note that system bars will only be "visible" if none of the
                    // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        // TODO: The system bars are visible. Make any desired
                        isFullscreen = false;
                        if (isAdded()) {
                            getBaseActivity().getSupportActionBar().show();
                        }
                    } else {
                        isFullscreen = true;
                        // TODO: The system bars are NOT visible. Make any desired
                        if (isAdded()) {
                            getBaseActivity().getSupportActionBar().hide();
                        }
                    }
                });
        getBaseActivity().getToolbarShadow().setVisibility(GONE);
        speakLayout.findViewById(R.id.btnFullscreen).setVisibility(GONE);
        speakLayout.findViewById(R.id.btnFullscreenExit).setVisibility(VISIBLE);
        speedLayout.findViewById(R.id.btnFullscreen).setVisibility(GONE);
        speedLayout.findViewById(R.id.btnFullscreenExit).setVisibility(VISIBLE);
    }

    public void stopFullscreen() {
        if (isFullscreen) {
            getBaseActivity().getToolbarShadow().setVisibility(VISIBLE);
            speakLayout.findViewById(R.id.btnFullscreen).setVisibility(VISIBLE);
            speakLayout.findViewById(R.id.btnFullscreenExit).setVisibility(GONE);
            speedLayout.findViewById(R.id.btnFullscreen).setVisibility(VISIBLE);
            speedLayout.findViewById(R.id.btnFullscreenExit).setVisibility(GONE);
            decorView.setSystemUiVisibility(0);
            isFullscreen = false;
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!TextUtils.isEmpty(query)) {
            if (mode.equals(Mode.SPEAK)) {
                safeCheckMenuItem(R.id.action_work_speaking, false);
                stopSpeak(true);
            }
            if (mode.equals(Mode.AUTO_SCROLL)) {
                safeCheckMenuItem(R.id.action_work_auto_scroll, false);
                cancelAutoScroll();
            }
            mode = Mode.SEARCH;
            searched.clear();
            adapter.selectText(query, true, colorFoundedText);

        }
        super.onQueryTextChange(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (searched.isEmpty()) {
            searched.addAll(search(query));
        }
        Pair<Integer, Integer> index = searched.poll();
        if (index != null) {
            adapter.setLastQuery(newFilterEvent(query));
            TextView textView = WorkFragment.this.getTextViewIndent(index.first);
            if (textView != null) {
                int visibleLines = WorkFragment.this.getVisibleLines(textView);
                Layout layout = textView.getLayout();
                scrollToIndex(index.first, index.second);
                isWaitingForSkipStart = false;
            } else if (isAdded()) {
                scrollToIndex(index.first, Integer.MIN_VALUE);
                itemList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollToIndex(index.first, index.second);
                    }
                }, 200);
            }
        }
        return true;
    }

    @Override
    public void toIndex(int index, int textOffset) {
        if (Integer.MIN_VALUE == textOffset) {
            super.toIndex(index, 0);
        } else {
            TextView textView = getTextViewIndent(index);
            index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
            Layout layout;
            if (textView != null && (layout = textView.getLayout()) != null) {
                layoutManager.scrollToPositionWithOffset(index, -(layout.getLineForOffset(textOffset)) * textView.getLineHeight());
            } else {
                layoutManager.scrollToPosition(index);
            }
            if (Mode.SEARCH == mode) {
                adapter.selectText(adapter.getLastQuery().toString(), false, colorFoundedText);
            }
        }
    }

    @Subscribe
    public void onEvent(ChapterSelectedEvent event) {
        scrollToIndex(event.bookmark.getIndentIndex(), Integer.MIN_VALUE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        String filePath = getArguments().getString(Constants.ArgsName.FILE_PATH);
        Uri contentUri = getArguments().getParcelable(Constants.ArgsName.CONTENT_URI);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (filePath != null || contentUri != null) {
            if (externalWork == null || (filePath != null && !filePath.equals(externalWork.getFilePath())) || (contentUri != null && !contentUri.equals(externalWork.getContentUri()))) {
                if (contentUri != null) {
                    filePath = HtmlClient.getCachedFile(getContext(), contentUri.getPath()).getAbsolutePath();
                }
                this.externalWork = databaseService.getExternalWork(filePath);
                if (externalWork == null) {
                    externalWork = new ExternalWork();
                    externalWork.setFilePath(filePath);
                    externalWork.setWorkTitle(new File(filePath).getName());
                    externalWork.setGenres("");
                }
                externalWork.setContentUri(contentUri);
                databaseService.insertOrUpdateExternalWork(externalWork);
                work = new Work(externalWork.getWorkUrl());
                File external = new File(externalWork.getFilePath());
                work.setTitle(external.getName());
                Author author = new Author(external.getParent());
                author.setShortName(new File(external.getParent()).getName());
                work.setAuthor(author);
            }
        } else if (incomingWork != null) {
            if (!incomingWork.equals(work)) {
                work = incomingWork;
                clearData();
            }
        } else if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                Work cached;
                if ((cached = WorkParser.getCachedWork(work.getLink())) != null) {
                    work = cached;
                }
                clearData();
            }
        }
        if (work.isParsed()) {
            isDownloaded = true;
            safeInvalidateOptionsMenu();
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
        ownTTSService = false;
        colorFoundedText = getResources().getColor(R.color.red_dark);
        colorSpeakingText = getResources().getColor(R.color.DeepSkyBlue);
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        speakLayout = (ViewGroup) inflater.inflate(R.layout.footer_work_tts_controls, root, false);
        speedLayout = (ViewGroup) inflater.inflate(R.layout.footer_work_auto_scroll, root, false);
        root.addView(speedLayout);
        root.addView(speakLayout);
        speakLayout.bringToFront();
        GuiUtils.getView(speakLayout, R.id.btnPlay).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnPause).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnStop).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnNext).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnPrevious).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnFullscreen).setOnClickListener(this);
        GuiUtils.getView(speakLayout, R.id.btnFullscreenExit).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnPlay).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnPause).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnStop).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnFullscreen).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnFullscreenExit).setOnClickListener(this);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        autoScrollSpeed = GuiUtils.getView(root, R.id.footer_work_speed);
        speechRate = GuiUtils.getView(root, R.id.footer_work_speech_rate);
        pitch = GuiUtils.getView(root, R.id.footer_work_pitch);
        autoScrollSpeed.setProgress(preferences.getInt(getString(R.string.preferenceWorkAutoScrollSpeed), 30));
        speechRate.setProgress(preferences.getInt(getString(R.string.preferenceWorkSpeechRate), 130));
        pitch.setProgress(preferences.getInt(getString(R.string.preferenceWorkPitch), 100));
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mode.equals(Mode.SPEAK) && !isWaitingPlayerCallback) {
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                        startSpeak(lastIndent, lastOffset);
                    }
                    isWaitingPlayerCallback = true;
                }
            }
        };
        speechRate.setOnSeekBarChangeListener(listener);
        pitch.setOnSeekBarChangeListener(listener);
        decorView = getActivity().getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            GuiUtils.getView(speakLayout, R.id.btnFullscreen).setVisibility(GONE);
            GuiUtils.getView(speedLayout, R.id.btnFullscreen).setVisibility(GONE);
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (TTSService.isReady(work)) {
            TTSPlayer.State state = TTSService.getInstance().getState();
            syncState(state);
            if (state.equals(TTSPlayer.State.PAUSE) || state.equals(TTSPlayer.State.SPEAKING)) {
                mode = Mode.SPEAK;
            }
            if (mode.equals(Mode.SPEAK)) {
                initFragmentForSpeak();
            }
        } else {
            syncState(TTSPlayer.State.END);
        }
    }

    @Override
    public void onClick(View v) {
        if ((!isWaitingPlayerCallback || v.getId() == R.id.btnStop) && mode.equals(Mode.SPEAK)) {
            switch (v.getId()) {
                case R.id.btnPlay:
                    if (!TTSService.isReady(work) || !ownTTSService) {
                        startSpeak(findFirstVisibleItemPosition(false), 0);
                    } else {
                        WorkFragment.this.selectText(lastIndent, null);
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.POSITION, lastIndent + ":" + lastOffset);
                    }
                    isWaitingPlayerCallback = true;
                    break;
                case R.id.btnPause:
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PAUSE);
                        isWaitingPlayerCallback = true;
                    }
                    break;
                case R.id.btnNext:
                    if (!isWaitingForSkipStart && TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.NEXT);
                        clearSelection();
                        isWaitingForSkipStart = true;
                    }
                    break;
                case R.id.btnPrevious:
                    if (!isWaitingForSkipStart && TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PRE);
                        clearSelection();
                        isWaitingForSkipStart = true;
                    }
                    break;
                case R.id.btnStop:
                    if (TTSService.isReady(work)) {
                        mode = Mode.NORMAL;
                        stopSpeak(true);
                        isWaitingPlayerCallback = true;
                    } else {
                        safeCheckMenuItem(R.id.action_work_speaking, false);
                        stopSpeak(false);
                    }
                    break;
            }
        }
        if (mode.equals(Mode.AUTO_SCROLL)) {
            switch (v.getId()) {
                case R.id.btnPlay:
                    playAutoScroll();
                    break;
                case R.id.btnPause:
                    pauseAutoScroll();
                    break;
                case R.id.btnStop:
                    cancelAutoScroll();
                    safeCheckMenuItem(R.id.action_work_auto_scroll, false);
                    mode = Mode.NORMAL;
                    break;
            }
        }
        switch (v.getId()) {
            case R.id.btnFullscreen:
                enableFullscreen();
                break;
            case R.id.btnFullscreenExit:
                stopFullscreen();
                break;
        }
    }

    private int getVisibleLines(TextView textView) {
        int screenHeight = GuiUtils.getScreenSize(itemList.getContext()).y;
        int scrolled = itemList.getScrollY();
        int totalHeight = screenHeight + scrolled;
        int lineHeight = textView.getLineHeight();
        int[] location = new int[2];
        textView.getLocationInWindow(location);
        int height = textView.getHeight();
        int difY = (int) (totalHeight - location[1] - height - lineHeight / 2d);  // + 1 lineHeight to save space
        int visibleLines;
        if (difY >= 0) {
            visibleLines = textView.getLineCount();
        } else {
            visibleLines = (height + difY) / lineHeight;
        }
        return visibleLines;
    }

    private TextView getTextViewIndent(int index) {
        ItemListAdapter.ViewHolder holder = adapter.getHolder(index);
        if (holder != null) {
            return holder.getView(R.id.work_text_indent);
        } else {
            return null;
        }
    }

    private void selectText(int index, String text) {
        TextView textView = getTextViewIndent(index);
        if (textView != null) {
            GuiUtils.selectText(textView, true, text, colorSpeakingText);
        }
    }

    private void selectText(int index, int start, int end) {
        ItemListAdapter.ViewHolder holder = adapter.getHolder(index);
        if (holder != null) {
            GuiUtils.selectText(holder.getView(R.id.work_text_indent), true, start, end, colorSpeakingText);
        }
    }

    private void lockOrientation() {
        if (isAdded()) {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }
    }

    private void releaseOrientation() {
        if (isAdded()) {
            getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }


    @Override
    protected ItemListAdapter<String> newAdapter() {
        return new WorkFragmentAdaptor();
    }

    public Work getWork() {
        return work;
    }

    public ExternalWork getExternalWork() {
        return externalWork;
    }

    private class WorkFragmentAdaptor extends MultiItemListAdapter<String> {

        private float fontSize;
        private int fontColor;
        private int backgroundColor;
        private Font.Type defaultType;
        private Font font;
        private FontResolver fontResolver;
        private String fontPath;

        public WorkFragmentAdaptor() {
            super(false, R.layout.header_work_list, R.layout.item_indent);
            bindOnlyRootViews = false;
            refreshSettings(getContext());
        }

        public void refreshSettings(Context context) {
            backgroundColor = AndroidSystemUtils.getStringResPreference(context, R.string.preferenceColorBackgroundReader, context.getResources().getColor(R.color.transparent));
            fontSize = AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontSizeReader, 16f);
            fontColor = AndroidSystemUtils.getStringResPreference(context, R.string.preferenceColorFontReader, GuiUtils.getThemeColor(context, android.R.attr.textColor));
            font = Font.mapFonts(getContext().getAssets()).get(AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontReader, Font.getDefFont().getName()));
            defaultType = Font.Type.valueOf(AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontStyleReader, Font.Type.PLAIN.name()));
            fontResolver = new WorkFontResolver(getContext().getAssets(), font, defaultType);
            fontPath = Font.getFontPath(getContext(), font.getName(), defaultType);
        }

        @Override
        public int getLayoutId(String item) {
            return R.layout.item_indent;
        }

        @Override
        public boolean onClick(View view, int position) {
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                switch (mode) {
                    case SPEAK:
                        position -= firstIsHeader;
                        if (!TTSService.isReady(work) || !ownTTSService) {
                            startSpeak(position, lastOffset);
                        } else {
                            if (TTSService.getInstance().getPlayer().isSpeaking()) {
                                TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                            } else {
                                WorkFragment.this.selectText(lastIndent, null);
                                TTSNotificationBroadcast.sendMessage(TTSService.Action.POSITION, position + ":" + lastOffset);
                            }
                        }
                        break;
                    case SEARCH:
                    case NORMAL:
                        ClickableSpan[] link = new SpannableString(textView.getText()).getSpans(lastOffset, lastOffset, ClickableSpan.class);
                        if (link.length != 0) {
                            ClickableSpan span = link[0];
                            if (span instanceof URLSpan) {
                                String url = ((URLSpan) span).getURL();
                                if (url.startsWith("/")) {
                                    Intent intent = new Intent(getActivity(), SectionActivity.class);
                                    intent.setData(Uri.parse(Constants.Net.BASE_DOMAIN + url));
                                    startActivity(intent);
                                }
                            } else {
                                span.onClick(textView);
                            }
                        }
                        break;
                }
            }
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HtmlSpanner spanner = new HtmlSpanner();
            switch (holder.getItemViewType()) {
                case R.layout.header_work_list:
                    TextView annotationView = holder.getView(R.id.work_annotation_header);
                    spanner.registerHandler("img", new PicassoImageHandler(annotationView));
                    spanner.registerHandler("a", new LinkHandler(annotationView));
                    annotationView.setMovementMethod(LinkMovementMethod.getInstance());
                    annotationView.setText(spanner.fromHtml(work.processAnnotationBloks(GuiUtils.getThemeColor(getContext(), R.attr.textColorAnnotations))));
                    holder.getItemView().setBackgroundColor(backgroundColor);
                    break;
                case R.layout.item_indent:
                    String indent = getItem(position);
                    TextView view = holder.getView(R.id.work_text_indent);
                    view.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            pressed = System.currentTimeMillis();
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            TextView textView = ((TextView) v);
                            int offset = 0;
                            int x = (int) event.getX();
                            int y = (int) event.getY();

                            x -= textView.getTotalPaddingLeft();
                            y -= textView.getTotalPaddingTop();

                            x += textView.getScrollX();
                            y += textView.getScrollY();

                            Layout layout = textView.getLayout();

                            if (layout != null) {
                                int line = layout.getLineForVertical(y);
                                offset = layout.getOffsetForHorizontal(line, x);
                            }

                            if (mode.equals(Mode.SPEAK) && isPaused()) {
                                clearSelection();
                                lastIndent = firstIsHeader + ((ViewHolder) view.getTag()).getLayoutPosition();
                                lastOffset = offset;
                                v.performClick();
                                return true;
                            }

                            if (textView.getText() instanceof Spanned && !mode.equals(Mode.SPEAK)) {
                                Spanned spannableString = (Spanned) textView.getText();
                                URLSpanNoUnderline url[] = spannableString.getSpans(offset, spannableString.length(), URLSpanNoUnderline.class);
                                if (url.length > 0) {
                                    String surl = url[url.length - 1].getURL();
                                    if (!surl.contains("/") && surl.endsWith(".shtml") && work.getAuthor() != null & work.getLink() != null) {
                                        SectionActivity.launchActivity(getContext(), work.getAuthor().getLink() + surl);
                                    } else {
                                        url[url.length - 1].onClick(textView);
                                    }
                                    return true;
                                }
                            }
                            if (mode.equals(Mode.AUTO_SCROLL)) {
                                if (speedLayout.getVisibility() == GONE) {
                                    speedLayout.setVisibility(VISIBLE);
                                } else {
                                    speedLayout.setVisibility(GONE);
                                }
                                return true;
                            }
                            if (mode.equals(Mode.SPEAK) && !isPaused()) {
                                if (speakLayout.getVisibility() == GONE) {
                                    speakLayout.setVisibility(VISIBLE);
                                } else {
                                    speakLayout.setVisibility(GONE);
                                }
                                return true;
                            }
                            if ((mode.equals(Mode.NORMAL) || mode.equals(Mode.SEARCH)) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                if (second && timer > 0 && System.currentTimeMillis() - timer < 2000) {
                                    if (isFullscreen) {
                                        stopFullscreen();
                                    } else {
                                        enableFullscreen();
                                    }
                                    second = false;
                                    return true;
                                } else {
                                    second = true;
                                    timer = System.currentTimeMillis();
                                }
                            }
                            if(System.currentTimeMillis() - pressed > 3000) {
                                v.performLongClick();
                                return true;
                            }
                            return false;
                        }
                        return false;
                    });
                    holder.getItemView().invalidate();
                    spanner.registerHandler("img", new PicassoImageHandler(view));
                    spanner.registerHandler("a", new LinkHandler(view));
                    spanner.setFontResolver(fontResolver);
                    view.setText(spanner.fromHtml(indent), TextView.BufferType.SPANNABLE);
                    // fix wrong height when use image spans
                    view.setTextSize(20);
                    initPreference(view);
                    // end
                    break;
            }
            selectText(holder, true, adapter.getLastQuery() == null ? null : adapter.getLastQuery().query, colorFoundedText);
        }

        private void initPreference(TextView textView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            textView.setTextColor(fontColor);
            CalligraphyUtils.applyFontToTextView(getContext(), textView, fontPath);
            ((ViewGroup) textView.getParent()).setBackgroundColor(backgroundColor);
        }

        private class WorkFontResolver extends SystemFontResolver {

            FontFamily defaultFont;

            public WorkFontResolver(AssetManager manager, Font font, Font.Type type) {
                Typeface typefaceDefault = font.getTypes().containsKey(type) ? TypefaceUtils.load(manager, font.getTypes().get(type)) : Typeface.DEFAULT;
                Typeface typefaceItalic = font.getTypes().containsKey(Font.Type.ITALIC) ? TypefaceUtils.load(manager, font.getTypes().get(Font.Type.ITALIC)) : null;
                Typeface typefaceBold = font.getTypes().containsKey(Font.Type.BOLD) ? TypefaceUtils.load(manager, font.getTypes().get(Font.Type.BOLD)) : null;
                Typeface typefaceItalicBold = font.getTypes().containsKey(Font.Type.BOLD_ITALIC) ? TypefaceUtils.load(manager, font.getTypes().get(Font.Type.BOLD_ITALIC)) : null;
                FontFamily fontFamily = new FontFamily(font.getName(), typefaceDefault);
                fontFamily.setBoldTypeface(typefaceBold);
                fontFamily.setItalicTypeface(typefaceItalic);
                fontFamily.setBoldItalicTypeface(typefaceItalicBold);
                this.defaultFont = fontFamily;
            }

            @Override
            public FontFamily getDefaultFont() {
                return defaultFont;
            }
        }

        ;
    }
}
