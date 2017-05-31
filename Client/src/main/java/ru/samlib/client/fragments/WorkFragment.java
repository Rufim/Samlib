package ru.samlib.client.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.*;
import android.support.annotation.IdRes;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.*;
import android.widget.SeekBar;
import android.widget.TextView;
import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.dialog.DirectoryChooserDialog;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.util.*;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.util.*;

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
    private PowerManager.WakeLock screenLock;
    private Mode mode = Mode.NORMAL;
    private boolean ownTTSService = false;
    private boolean isDownloaded = false;
    private boolean isSeekBarVisible = false;
    private boolean isWaitingPlayerCallback = false;
    private boolean isFullscreen = false;
    private boolean isBack = false;
    private SeekBar autoScrollSpeed;
    private SeekBar pitch;
    private SeekBar speechRate;
    private ViewGroup speedLayout;
    private ViewGroup speakLayout;
    private CountDownTimer autoScroller;

    private View decorView;

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
            while (work == null) {
                SystemClock.sleep(100);
            }
            if (!work.isParsed()) {
                if (externalWork != null) {
                    work = WorkParser.parse(new File(externalWork.getFilePath()), "CP1251", work, true);
                    work.setParsed(true);
                    isDownloaded = true;
                    safeInvalidateOptionsMenu();
                } else {
                    work = new WorkParser(work).parse(true, Parser.isCachedMode());
                    if (!Parser.isCachedMode()) {
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
                        GuiUtils.runInUI(getContext(), (v) -> progressBarText.setText(R.string.work_parse));
                        isDownloaded = true;
                        safeInvalidateOptionsMenu();
                        WorkParser.processChapters(work);
                        GuiUtils.runInUI(getContext(), (v) -> {
                            if (searchView != null) searchView.setEnabled(true);
                        });
                    }
                }
            }
            if (work.isParsed()) {
                isDownloaded = true;
                safeInvalidateOptionsMenu();
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
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
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
    protected void firstLoad(boolean scroll) {
        try {
            Bookmark bookmark = databaseService.getBookmark(work.isNotSamlib() ? externalWork.getFilePath() : work.getFullLink());
            if (dataSource != null && !isEnd && adapter.getItems().isEmpty()) {
                loadMoreBar.setVisibility(View.GONE);
                if (bookmark != null && scroll) {
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
        if(externalWork == null || externalWork.getWorkUrl() != null) {
            bookmark.setWorkUrl(work.getFullLink());
            bookmark.setAuthorUrl(work.getAuthor().getFullLink());
        } else {
            bookmark.setWorkUrl(externalWork.getFilePath());
        }
        bookmark.setAuthorShortName(work.getAuthor().getShortName());
        bookmark.setGenres(work.printGenres());
        bookmark.setWorkTitle(work.getTitle());
        work.setBookmark(bookmark.createEntity());
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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
    public void onPause() {
        super.onPause();
        if (work != null && work.isParsed()) {
            try {
                int index = findFirstVisibleItemPosition(false);
                int size = adapter.getItems().size();
                if (size > index) {
                    setBookmark(work, adapter.getItems().get(index), index);
                    databaseService.insertOrUpdateBookmark(work.getBookmark());
                }
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception", e);
            }
        }
        // sanity check for null as this is a public method
        if (screenLock != null) {
            Log.v(TAG, "Releasing wakelock");
            try {
                screenLock.release();
            } catch (Throwable th) {
                // ignoring this exception, probably wakeLock was already released
            }
        } else {
            // should never happen during normal workflow
            Log.e(TAG, "Wakelock reference is null");
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
    public void refreshData(boolean showProgress) {
        work.setParsed(false);
        super.refreshData(showProgress);
    }

    public List<Pair<Integer, Integer>> search(String query) {
        List<Pair<Integer, Integer>> indexes = new ArrayList<>();
        for (int i = 0; i < adapter.getItems().size(); i++) {
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
            if(work.isNotSamlib()) {
                menu.removeItem(R.id.action_work_to_author);
                menu.removeItem(R.id.action_work_share);
            }
            if (externalWork != null) {
                menu.removeItem(R.id.action_work_save);
            }
        } else {
            menu.clear();
        }
        if (!work.isParsed()) {
            searchView.setEnabled(false);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.removeItem(R.id.action_work_fullscreen);
        }
        if(mode.equals(Mode.SPEAK)) {
            speakLayout.setVisibility(VISIBLE);
            safeCheckMenuItem(R.id.action_work_speaking, true);

        } else {
            safeCheckMenuItem(R.id.action_work_speaking, false);
            speakLayout.setVisibility(GONE);
        }
        if(mode.equals(Mode.AUTO_SCROLL)) {
            speedLayout.setVisibility(VISIBLE);
            safeCheckMenuItem(R.id.action_work_auto_scroll, true);
        } else {
            safeCheckMenuItem(R.id.action_work_auto_scroll, false);
            speedLayout.setVisibility(GONE);
        }
        searchView.setQueryHint(getString(R.string.search_hint));
    }

    @Override
    public void onSearchViewClose(SearchView searchView) {
        adapter.setLastQuery(null);
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
                itemList.scrollBy(0, (int) (heightToScroll * (getRate(autoScrollSpeed) * 0.35)));
            }

            public void onFinish() {
                //you can add code for restarting timer here
            }
        };
        itemList.post(() -> {
            autoScroller.start();
        });
        getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }

    private void stopAutoScroll() {
        if (autoScroller != null) {
            autoScroller.cancel();
        }
        autoScroller = null;
        getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
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
    }


    public void stopSpeak() {
        if (TTSService.isReady(work)) {
            TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
        }
        clearSelection();
        if (isAdded()) {
            safeCheckMenuItem(R.id.action_work_speaking, false);
            Intent i = new Intent(getContext(), TTSService.class);
            getContext().stopService(i);
        }
        speakLayout.setVisibility(View.GONE);
        GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPlay);
        GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPause);
    }

    private void safeCheckMenuItem(@IdRes int id, boolean state) {
        if (getBaseActivity() != null && getBaseActivity().getToolbar() != null) {
            MenuItem item = getBaseActivity().getToolbar().getMenu().findItem(id);
            if (item != null) {
                item.setChecked(state);
            }
        }
    }

    public void startSpeak(int position, int offset) {
        ownTTSService = true;
        WorkFragment.this.selectText(lastIndent, null);
        Intent i = new Intent(getActivity(), TTSService.class);
        i.putExtra(Constants.ArgsName.LINK, work.isNotSamlib() ? externalWork.getFilePath() : work.getLink());
        i.putExtra(Constants.ArgsName.TTS_PLAY_POSITION, position + ":" + offset);
        i.putExtra(Constants.ArgsName.TTS_SPEECH_RATE, getRate(speechRate));
        i.putExtra(Constants.ArgsName.TTS_PITCH, getRate(pitch));
        if (isAdded()) {
            getActivity().startService(i);
            getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    public void clearSelection() {
        selectText(lastIndent, null);
    }

    void showSpeedBar() {
        if (!isSeekBarVisible && mode.equals(Mode.AUTO_SCROLL)) {
            speakLayout.setVisibility(VISIBLE);
            isSeekBarVisible = true;
        }
    }

    void hideSpeedBar() {
        if (isSeekBarVisible) {
            speakLayout.setVisibility(GONE);
            isSeekBarVisible = false;
        }
    }

    private void syncState(TTSPlayer.State state) {
        isWaitingPlayerCallback = false;
        switch (state) {
            case SPEAKING:
                if(speedLayout.findViewById(R.id.btnPlay).getVisibility() == VISIBLE) {
                    getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                }
                GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPlay);
                GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPause);
                break;
            case END:
                if (isAdded()) {
                    safeCheckMenuItem(R.id.action_work_speaking, false);
                }
                speakLayout.setVisibility(View.GONE);
                getBaseActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            case STOPPED:
            case PAUSE:
                GuiUtils.setVisibility(VISIBLE, speakLayout, R.id.btnPlay);
                GuiUtils.setVisibility(GONE, speakLayout, R.id.btnPause);
                break;
        }
    }

    private void initFragmentForSpeak() {
        if (!TTSService.isReady(work)) {
            TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
        }
        TTSService.setNextPhraseListener(new TTSPlayer.OnNextPhraseListener() {
            @Override
            public void onNextPhrase(int speakIndex, int phraseIndex, List<TTSPlayer.Phrase> phrases) {
                if (WorkFragment.this.getActivity() != null && TTSService.isReady(work)) {
                    TTSPlayer.Phrase phrase = phrases.get(phraseIndex);
                    lastIndent = speakIndex;
                    TextView textView = WorkFragment.this.getTextViewIndent(speakIndex);
                    if (textView != null) {
                        int visibleLines = WorkFragment.this.getVisibleLines(textView);
                        Layout layout = textView.getLayout();
                        if (visibleLines < layout.getLineForOffset(phrase.end) + 1) {
                            WorkFragment.this.scrollToIndex(speakIndex, phrase.start);
                        }
                        WorkFragment.this.selectText(speakIndex, phrase.start, phrase.end);
                    } else {
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
                    stopAutoScroll();
                    item.setChecked(false);
                } else {
                    if (Mode.SPEAK.equals(mode)) {
                        stopSpeak();
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
                    stopSpeak();
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
            case R.id.action_work_to_author:
                AuthorFragment.show(this, work.getAuthor());
                return true;
            case R.id.action_work_share:
                AndroidSystemUtils.shareText(getActivity(), work.getAuthor().getShortName(), work.getTitle(), work.getFullLink(), "text/plain");
                return true;
            case R.id.action_work_save:
                if (isAdded()) {
                    getBaseActivity().doActionWithPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, permissionGained -> {
                        if (permissionGained) {
                            String path = AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceLastSavedWorkPath, Environment.getExternalStorageDirectory().getAbsolutePath());
                            DirectoryChooserDialog chooserDialog = new DirectoryChooserDialog(getActivity(), path, false);
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
                    if (externalWork != null) {
                        AndroidSystemUtils.openFileInExtApp(getActivity(), new File(externalWork.getFilePath()));
                    } else if (work.getCachedResponse() != null) {
                        AndroidSystemUtils.openFileInExtApp(getActivity(), work.getCachedResponse());
                    } else {
                        AndroidSystemUtils.openFileInExtApp(getActivity(), HtmlClient.getCachedFile(getContext(), work.getLink()));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Unknown exception", e);
                }
                return true;
            case R.id.action_work_comments:
                CommentsPagerFragment.show(newFragmentBuilder()
                        .addToBackStack()
                        .setAnimation(R.anim.slide_in_left, R.anim.slide_out_right)
                        .setPopupAnimation(R.anim.slide_in_right, R.anim.slide_out_left), getId(), work);
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
    }

    public void stopFullscreen() {
        if (isFullscreen) {
            decorView.setSystemUiVisibility(0);
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searched.clear();
        adapter.selectText(query, true, colorFoundedText);
        super.onQueryTextChange(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mode = Mode.SEARCH;
        if (searched.isEmpty()) {
            searched.addAll(search(query));
        }
        Pair<Integer, Integer> index = searched.poll();
        if (index != null) {
            adapter.setLastQuery(newFilterEvent(query));
            scrollToIndex(index.first, index.second);
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
            if (textView != null) {
                Layout layout = textView.getLayout();
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
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        this.externalWork = null;
        if (filePath != null) {
            this.externalWork = databaseService.getExternalWork(filePath);
            if(externalWork == null) {
                externalWork = new ExternalWork();
                externalWork.setFilePath(filePath);
                externalWork.setWorkTitle(new File(filePath).getName());
                externalWork.setGenres("");
                externalWork.setSavedDate(new Date());
                databaseService.insertOrUpdateExternalWork(externalWork);
            }
            work = new Work(externalWork.getWorkUrl());
            File external =  new File(externalWork.getFilePath());
            work.setTitle(external.getName());
            Author author = new Author(external.getParent());
            author.setShortName(new File(external.getParent()).getName());
            work.setAuthor(author);
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
        GuiUtils.getView(speedLayout, R.id.btnPlay).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnPause).setOnClickListener(this);
        GuiUtils.getView(speedLayout, R.id.btnStop).setOnClickListener(this);
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
                if (mode.equals(Mode.SPEAK)) {
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                        startSpeak(lastIndent, lastOffset);
                    }
                }
            }
        };
        speechRate.setOnSeekBarChangeListener(listener);
        pitch.setOnSeekBarChangeListener(listener);
        decorView = getActivity().getWindow().getDecorView();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        screenLock = ((PowerManager) getActivity().getSystemService(Activity.POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        screenLock.acquire();
        if(TTSService.isReady(work)) {
            syncState(TTSService.getInstance().getState());
            if(mode.equals(Mode.SPEAK)) {
                initFragmentForSpeak();
            }
        } else {
            syncState(TTSPlayer.State.END);
        }
    }

    @Override
    public void onClick(View v) {
        if (!isWaitingPlayerCallback & mode.equals(Mode.SPEAK)) {
            switch (v.getId()) {
                case R.id.btnPlay:
                    startSpeak(findFirstVisibleItemPosition(false), 0);
                    isWaitingPlayerCallback = true;
                    break;
                case R.id.btnPause:
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PAUSE);
                        isWaitingPlayerCallback = true;
                    }
                    break;
                case R.id.btnNext:
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.NEXT);
                        clearSelection();
                    }
                    break;
                case R.id.btnPrevious:
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PRE);
                        clearSelection();
                    }
                    break;
                case R.id.btnStop:
                    if (TTSService.isReady(work)) {
                        mode = Mode.NORMAL;
                        stopSpeak();
                        isWaitingPlayerCallback = true;
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

    @Override
    protected ItemListAdapter<String> newAdaptor() {
        return new WorkFragmentAdaptor();
    }

    public Work getWork() {
        return work;
    }

    private class WorkFragmentAdaptor extends MultiItemListAdapter<String> {

        public WorkFragmentAdaptor() {
            super(true, false, R.layout.header_work_list, R.layout.item_indent);
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
                    annotationView.setText(spanner.fromHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold))));
                    break;
                case R.layout.item_indent:
                    String indent = getItem(position);
                    TextView view = holder.getView(R.id.work_text_indent);
                    view.setOnTouchListener((v, event) -> {
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

                            if (mode.equals(Mode.SPEAK)) {
                                lastOffset = offset;
                            }

                            if (textView.getText() instanceof SpannedString && !mode.equals(Mode.SPEAK)) {
                                SpannedString spannableString = (SpannedString) textView.getText();
                                URLSpanNoUnderline url[] = spannableString.getSpans(offset, spannableString.length(), URLSpanNoUnderline.class);
                                if (url.length > 0) {
                                    url[0].onClick(textView);
                                    return true;
                                }
                            }
                            if (mode.equals(Mode.AUTO_SCROLL)) {
                                if (speedLayout.getVisibility() == GONE) {
                                    speedLayout.setVisibility(VISIBLE);
                                } else {
                                    speedLayout.setVisibility(GONE);
                                }
                            }
                            if ((mode.equals(Mode.NORMAL) || mode.equals(Mode.SEARCH)) && isFullscreen) {
                                stopFullscreen();
                            }
                            v.performClick();
                        }
                        return true;
                    });
                    holder.getItemView().invalidate();
                    spanner.registerHandler("img", new PicassoImageHandler(view));
                    spanner.registerHandler("a", new LinkHandler(view));
                    view.setText(spanner.fromHtml(indent));
                    // fix wrong height when use image spans
                    view.setTextSize(20);
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    // end
                    break;
            }
            selectText(holder, true, adapter.getLastQuery() == null ? null : adapter.getLastQuery().query, colorFoundedText);
        }

    }
}
