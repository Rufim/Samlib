package ru.samlib.client.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.*;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.*;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.nd.android.sdp.im.common.widget.htmlview.view.HtmlView;
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.samlib.client.dialog.DirectoryChooserDialog;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.ChapterSelectedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.receiver.TTSNotificationBroadcast;
import ru.samlib.client.service.TTSService;
import ru.samlib.client.util.*;

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

    private enum Mode {
        SEARCH, SPEAK, NORMAL
    }

    public static void show(FragmentBuilder builder, @IdRes int container, String link) {
        show(builder, container, WorkFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(FragmentBuilder builder, @IdRes int container, Work work) {
        show(builder, container, WorkFragment.class, Constants.ArgsName.WORK, work);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, WorkFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Work work) {
        show(fragment, WorkFragment.class, Constants.ArgsName.WORK, work);
    }

    public WorkFragment() {
        pageSize = 250;
        setDataSource(((skip, size) -> {
            while (work == null) {
                SystemClock.sleep(10);
            }
            if (!work.isParsed()) {
                try {
                    work = new WorkParser(work).parse(false);
                    postEvent(new WorkParsedEvent(work));
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            if (work.isParsed()) {
                pageSize += pageSize;
                return Stream.of(work.getIndents())
                        .skip(skip)
                        .limit(size)
                        .collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }

        }));
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
            lastSearchQuery = query;
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
            adapter.selectText(lastSearchQuery, false, colorFoundedText);
        }
    }

    public void onEvent(ChapterSelectedEvent event) {
        scrollToIndex(event.chapter.getIndex());
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
                clearData();
            }
        }
        if (work.isParsed()) {
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
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
    protected ItemListAdapter<String> getAdapter() {
        return new WorkFragmentAdaptor();
    }

    public Work getWork() {
        return work;
    }

    private class WorkFragmentAdaptor extends MultiItemListAdapter<String> {

        private int lastOffset = 0;

        public WorkFragmentAdaptor() {
            super(true, R.layout.header_work_list, R.layout.item_indent);
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
                        if (!TTSService.isReady(work)) {
                            WorkFragment.this.selectText(lastIndent, null);
                            Intent i = new Intent(getActivity(), TTSService.class);
                            i.putExtra(Constants.ArgsName.WORK, work);
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
                            span.onClick(textView);
                        }
                        break;
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case R.layout.header_work_list:
                    HtmlView htmlView = holder.getView(R.id.work_annotation_header);
                    htmlView.loadHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold)));
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
                    HtmlSpanner spanner = new HtmlSpanner();
                    spanner.registerHandler("img", new PicassoImageHandler(view));
                    spanner.registerHandler("a", new LinkHandler(view));
                    view.setText(spanner.fromHtml(indent));
                    break;
            }
            selectText(holder, true, WorkFragment.this.lastSearchQuery, colorFoundedText);
        }

    }
}
