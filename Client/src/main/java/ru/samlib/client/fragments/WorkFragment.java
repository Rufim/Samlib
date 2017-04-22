package ru.samlib.client.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.IdRes;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.TextView;
import com.snappydb.SnappydbException;
import org.greenrobot.eventbus.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.util.*;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.samlib.client.database.SnappyHelper;
import ru.samlib.client.dialog.DirectoryChooserDialog;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.entity.WorkEntity;
import ru.samlib.client.domain.events.ChapterSelectedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.receiver.TTSNotificationBroadcast;
import ru.samlib.client.service.DatabaseService;
import ru.samlib.client.service.TTSService;
import ru.samlib.client.util.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<String> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Queue<Pair<Integer, Integer>> searched = new ArrayDeque<>();
    private Integer lastIndent = 0;
    private int colorSpeakingText;
    private int colorFoundedText;
    private PowerManager.WakeLock screenLock;
    private Mode mode = Mode.NORMAL;
    private boolean ownTTSService = false;

    @Inject
    DatabaseService databaseService;

    private enum Mode {
        SEARCH, SPEAK, NORMAL
    }

    public static WorkFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, WorkFragment.class);
    }

    public static WorkFragment show(FragmentBuilder builder, @IdRes int container, Work work) {
        return show(builder.putArg( Constants.ArgsName.WORK, work), container, WorkFragment.class);
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
            if(skip != 0) return null;
            while (work == null) {
                SystemClock.sleep(100);
            }
            if (!work.isParsed()) {
                try {
                    work = new WorkParser(work).parse(true, false);
                    work.setCachedDate(new Date());
                    if(work.isChanged() && work.getId() != null) {
                        work.setChanged(false);
                        work.setSizeDiff(null);
                    }
                    work = databaseService.insertOrUpdateWork(work);
                    if(!work.isParsed()) {
                        WorkParser.processChapters(work);
                    }
                    postEvent(new WorkParsedEvent(work));
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            if(work.isParsed()){
                return work.getIndents();
            } else {
                return new ArrayList<>();
            }
        }));
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void firstLoad(boolean scroll) {
        try {
            Bookmark bookmark = work.getBookmark();
            if (dataSource != null && !isEnd && adapter.getItems().isEmpty()) {
                loadMoreBar.setVisibility(View.GONE);
                if (bookmark != null && scroll) {
                    scrollToIndex(bookmark.getIndentIndex());
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
    public void onPause() {
        super.onPause();
        try {
            int indexLast = findLastVisibleItemPosition(false);
            int index = findFirstVisibleItemPosition(false);
            int size = adapter.getItems().size();
            if(size > index && index > 0) {
                String indent = adapter.getItems().get(index);
                Bookmark bookmark = work.getBookmark();
                if(bookmark == null) {
                    bookmark = new Bookmark(indent);
                } else {
                    bookmark.setIndent(indent);
                }
                bookmark.setIndentIndex(indexLast - 1);
                work.setBookmark(bookmark.createEntry());
                databaseService.insertOrUpdateWork(work);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
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
        inflater.inflate(R.menu.work, menu);
        searchView.setQueryHint(getString(R.string.search_hint));
    }

    @Override
    public void onSearchViewClose(SearchView searchView) {
        lastSearchQuery = null;
        mode = Mode.NORMAL;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_work_speaking:
                if (item.isChecked()) {
                    mode = Mode.NORMAL;
                    item.setChecked(false);
                    if (TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                    }
                    selectText(lastIndent, null);
                } else {
                    mode = Mode.SPEAK;
                    item.setChecked(true);
                    if (!TTSService.isReady(work)) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                    }
                    TTSService.setNextPhraseListener((speakIndex, phraseIndex, phrases) -> {
                        if (getActivity() != null && TTSService.isReady(work)) {
                            TTSPlayer.Phrase phrase = phrases.get(phraseIndex);
                            lastIndent = speakIndex;
                            TextView textView = getTextViewIndent(speakIndex);
                            if (textView != null) {
                                int visibleLines = getVisibleLines(textView);
                                Layout layout = textView.getLayout();
                                if (visibleLines < layout.getLineForOffset(phrase.end) + 1) {
                                    scrollToIndex(speakIndex, phrase.start);
                                }
                                selectText(speakIndex, phrase.start, phrase.end);
                            }
                        }
                    });
                    TTSService.setIndexSpeakFinished(speakIndex -> {
                        if (getActivity() != null && TTSService.isReady(work)) {
                            selectText(speakIndex, null);
                        }
                    });
                }
                return true;
            case R.id.action_work_to_author:
                AuthorFragment.show(this, work.getAuthor());
                return true;
            case R.id.action_work_share:
                AndroidSystemUtils.shareText(getActivity(), work.getAuthor().getShortName(), work.getTitle(), work.getFullLink(), "text/plain");
                return true;
            case R.id.action_work_save:
                DirectoryChooserDialog chooserDialog = new DirectoryChooserDialog(getActivity(), Environment.getExternalStorageDirectory().getAbsolutePath(), false);
                chooserDialog.setTitle("Сохранить в...");
                chooserDialog.setIcon(android.R.drawable.ic_menu_save);
                chooserDialog.setAllowRootDir(true);
                chooserDialog.setOnChooseFileListener(chosenFile -> {
                    try {
                        SystemUtils.copy(work.getCachedResponse(), new File(chosenFile, work.getTitle() + ".html"));
                    } catch (IOException e) {
                        Log.e(TAG, "Unknown exception", e);
                    }
                });
                chooserDialog.show();
                return true;
            case R.id.action_work_open_with:
                try {
                    AndroidSystemUtils.openFileInExtApp(getActivity(), work.getCachedResponse());
                } catch (IOException e) {
                    Log.e(TAG, "Unknown exception", e);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
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
            if(lastSearchQuery != null)  {
                lastSearchQuery.query = query;
            } else {
                lastSearchQuery = newFilterEvent(query);
            }
            scrollToIndex(index.first, index.second);
        }
        return true;
    }

    @Override
    public void toIndex(int index, int textOffset) {
        TextView textView = getTextViewIndent(index);
        index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
        if (textView != null) {
            Layout layout = textView.getLayout();
            layoutManager.scrollToPositionWithOffset(index, -(layout.getLineForOffset(textOffset)) * textView.getLineHeight());
        } else {
            layoutManager.scrollToPosition(index);
        }
        if (Mode.SEARCH == mode) {
            adapter.selectText(lastSearchQuery.toString(), false, colorFoundedText);
        }
    }

    @Subscribe
    public void onEvent(ChapterSelectedEvent event) {
        scrollToIndex(event.bookmark.getIndentIndex());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (incomingWork != null) {
            if (!incomingWork.equals(work)) {
                work = incomingWork;
                clearData();
            }
        } else if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                WorkEntity entity;
                if((entity = databaseService.getWork(work.getLink())) != null) {
                   work = entity;
                }
                clearData();
            }
        }
        if (work.isParsed()) {
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
        ownTTSService = false;
        colorFoundedText = getResources().getColor(R.color.red_dark);
        colorSpeakingText = getResources().getColor(R.color.DeepSkyBlue);
        screenLock = ((PowerManager) getActivity().getSystemService(Activity.POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        screenLock.acquire();
        return super.onCreateView(inflater, container, savedInstanceState);
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
    protected ItemListAdapter<String> newAdapter() {
        return new WorkFragmentAdaptor();
    }

    public Work getWork() {
        return work;
    }

    private class WorkFragmentAdaptor extends MultiItemListAdapter<String> {

        private int lastOffset = 0;

        public WorkFragmentAdaptor() {
            super(true, false, R.layout.header_work_list, R.layout.item_indent);
        }

        @Override
        public int getLayoutId(String item) {
            return R.layout.item_indent;
        }

        @Override
        public void onClick(View view, int position) {
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                switch (mode) {
                    case SPEAK:
                        position -= firstIsHeader;
                        if (!TTSService.isReady(work) || !ownTTSService) {
                            ownTTSService = true;
                            WorkFragment.this.selectText(lastIndent, null);
                            Intent i = new Intent(getActivity(), TTSService.class);
                            i.putExtra(Constants.ArgsName.LINK, work.getLink());
                            i.putExtra(Constants.ArgsName.TTS_PLAY_POSITION, position + ":" + lastOffset);
                            getActivity().startService(i);
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
                            if(span instanceof URLSpan) {
                                String url = ((URLSpan) span).getURL();
                                if(url.startsWith("/")){
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
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HtmlSpanner spanner = new HtmlSpanner();
            switch (holder.getItemViewType()) {
                case R.layout.header_work_list:
                    TextView annotationView = holder.getView(R.id.work_annotation_header);
                    spanner.registerHandler("img", new PicassoImageHandler(annotationView));
                    spanner.registerHandler("a", new LinkHandler(annotationView));
                    annotationView.setText(spanner.fromHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold))));
                    break;
                case R.layout.item_indent:
                    String indent = getItem(position);
                    TextView view = holder.getView(R.id.work_text_indent);
                    view.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            TextView textView = ((TextView) v);
                            int x = (int) event.getX();
                            int y = (int) event.getY();

                            x -= textView.getTotalPaddingLeft();
                            y -= textView.getTotalPaddingTop();

                            x += textView.getScrollX();
                            y += textView.getScrollY();

                            Layout layout = textView.getLayout();

                            if (layout != null) {
                                int line = layout.getLineForVertical(y);
                                lastOffset = layout.getOffsetForHorizontal(line, x);
                            }

                            v.performClick();
                        }
                        return true;
                    });
                    spanner.registerHandler("img", new PicassoImageHandler(view));
                    spanner.registerHandler("a", new LinkHandler(view));
                    view.setText(spanner.fromHtml(indent));
                    break;
            }
            selectText(holder, true, lastSearchQuery == null ? null : lastSearchQuery.query, colorFoundedText);
        }

    }
}
