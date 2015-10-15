package ru.samlib.client.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.nd.android.sdp.im.common.widget.htmlview.view.HtmlView;
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.ChapterSelectedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.receiver.TTSNotificationBroadcast;
import ru.samlib.client.service.TTSService;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;
import ru.samlib.client.util.TTSPlayer;

import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<String> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Queue<Integer> searched = new ArrayDeque<>();
    private AsyncTask moveToIndex;
    private Integer lastIndent = 0;
    private int colorSpeakingText;
    private int colorFoundedText;
    private PowerManager.WakeLock screenLock;
    private Mode mode = Mode.NORMAL;

    private enum Mode {
        SEARCH, SPEAK, NORMAL
    }

    public WorkFragment() {
        pageSize = 100;
        setLister(((skip, size) -> {
            while (work == null) {
                SystemClock.sleep(10);
            }
            if (!work.isParsed()) {
                try {
                    work = new WorkParser(work).parse();
                    work.processChapters();
                    postEvent(new WorkParsedEvent(work));
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            return Stream.of(work.getIndents())
                    .skip(skip)
                    .limit(size)
                    .collect(Collectors.toList());
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
        screenLock.release();
    }

    @Override
    public boolean allowBackPress() {
        new FragmentBuilder(getFragmentManager())
                .putArg(Constants.ArgsName.LINK, work.getAuthor().getLink())
                .replaceFragment(WorkFragment.this, SectionFragment.class);
        return false;
    }

    public List<Integer> search(String query) {
        List<Integer> indexes = new ArrayList<>();
        Elements items = work.getRootElements();
        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            final String text = item.text().toLowerCase();
            if (text.contains(query)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.search);
        if (searchItem != null) {
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(() -> {
                lastQuery = null;
                mode = Mode.NORMAL;
                return false;
            });
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searched.clear();
        adapter.selectText(query, true, colorFoundedText);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mode = Mode.SEARCH;
        if (searched.isEmpty()) {
            searched.addAll(search(query));
        }
        Integer index = searched.poll();
        if (index != null) {
            lastQuery = query;
            scrollToIndex(index);
        }
        return true;
    }

    private void scrollToIndex(int index) {
        scrollToIndex(index, 0);
    }

    private void toIndex(int index, int offsetLines) {
        TextView textView = getTextViewIndent(index);
        index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
        if (textView != null) {
            layoutManager.scrollToPositionWithOffset(index, - offsetLines * textView.getLineHeight());
        } else {
            layoutManager.scrollToPosition(index);
        }
        if (Mode.SEARCH == mode) {
            adapter.selectText(lastQuery, false, colorFoundedText);
        }
    }

    //TODO: smoothscrool
    public void smoothScrollToPosition(int position, int offset) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(itemList.getContext()) {
            public PointF computeScrollVectorForPosition(int targetPosition) {
                PointF calculate = layoutManager.computeScrollVectorForPosition(targetPosition);
                calculate.y += offset;
                return calculate;
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(linearSmoothScroller);
    }

    private void scrollToIndex(int index, int offsetLines) {
        if (adapter.getItemCount() > index) {
            toIndex(index, offsetLines);
        } else {
            moveToIndex = new AsyncTask<Integer, Void, Void>() {

                int index = 0;
                int offsetLines = 0;

                @Override
                protected Void doInBackground(Integer... params) {
                    index = params[0];
                    offsetLines = params[1];
                    return null;
                }

                @Override
                protected void onPostExecute(Void empty) {
                    if (this == moveToIndex) {
                        toIndex(index, offsetLines);
                    }
                }
            };
            loadElements(index + pageSize, true, moveToIndex, index, offsetLines);
        }
    }

    public void onEvent(ChapterSelectedEvent event) {
        scrollToIndex(event.chapter.getIndex());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        if (work == null || !work.getLink().equals(link)) {
            work = new Work(link);
            TTSService.setNextPhraseListener((speakIndex, phraseIndex, phrases) -> {
                if (getActivity() != null) {
                    TTSPlayer.Phrase phrase = phrases.get(phraseIndex);
                    lastIndent = speakIndex;
                    TextView textView = getTextViewIndent(speakIndex);
                    if (textView != null) {
                        int visibleLines = getVisibleLines(textView);
                        Layout layout = textView.getLayout();
                        if (visibleLines < layout.getLineForOffset(phrase.end) + 1) {
                            scrollToIndex(speakIndex, layout.getLineForOffset(phrase.start));
                        }
                        selectText(speakIndex, phrase.start, phrase.end);
                    }
                }
            });
            TTSService.setIndexSpeakFinished(speakIndex -> {
                selectText(speakIndex, null);
            });
            colorFoundedText = getResources().getColor(R.color.red_dark);
            colorSpeakingText = getResources().getColor(R.color.DeepSkyBlue);
        } else {
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
        screenLock = ((PowerManager)getActivity().getSystemService(Activity.POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
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
        Layout layout = textView.getLayout();
        int scrollY = textView.getScrollY();
        int visibleLines;
        if (difY >= 0) {
            visibleLines = textView.getLineCount();
        } else {
            visibleLines = (height + difY) / lineHeight;
        }
        return visibleLines;
    }

    private boolean isIndentVisible(int index) {
        index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
        int first = findFirstVisibleItemPosition(true);
        int last = findLastVisibleItemPosition(true);
        if (first > index || index > last) {
            return false;
        }
        return true;
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
            super(true, R.layout.work_list_header, R.layout.indent_item);
        }

        @Override
        public int getLayoutId(String item) {
            return R.layout.indent_item;
        }

        @Override
        public void onClick(View view, int position) {
            if (mode == Mode.SPEAK) {
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
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case R.layout.work_list_header:
                    HtmlView htmlView = holder.getView(R.id.work_annotation_header);
                    htmlView.loadHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold)));
                    break;
                case R.layout.indent_item:
                    String indent = getItem(position);
                    TextView view = holder.getView(R.id.work_text_indent);
                    view.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            TextView textView = ((TextView) v);
                            Layout layout = textView.getLayout();
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            if (layout != null) {
                                int line = layout.getLineForVertical(y);
                                lastOffset = layout.getOffsetForHorizontal(line, x);
                                v.performClick();
                            }
                        }
                        return true;
                    });
                    HtmlSpanner spanner = new HtmlSpanner();
                    spanner.registerHandler("img", new PicassoImageHandler(view));
                    view.setMovementMethod(LinkMovementMethod.getInstance());
                    view.setText(spanner.fromHtml(indent));
                    break;
            }
            selectText(holder, true, WorkFragment.this.lastQuery, colorFoundedText);
        }

    }
}
