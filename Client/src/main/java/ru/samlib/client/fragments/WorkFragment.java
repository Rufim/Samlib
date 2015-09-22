package ru.samlib.client.fragments;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
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
import ru.samlib.client.util.AndroidSystemUtils;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;
import ru.samlib.client.util.TTSPlayer;

import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<String> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Queue<Integer> searched = new ArrayDeque<>();
    private AsyncTask moveToIndex;
    private Integer lastIndex;
    private Integer lastIndent = 0;
    private int colorSpeakingText;
    private int colorFoundedText;

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
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searched.clear();
        adapter.selectText(query, true, colorFoundedText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
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
        if (adapter.getItemCount() > index) {
            lastIndex = null;
            layoutManager.scrollToPositionWithOffset(index, 0);
            adapter.selectText(lastQuery, false, Color.RED);
        } else {
            if (moveToIndex == null) {
                lastIndex = null;
                loadElements(index + pageSize, true);
                moveToIndex = new AsyncTask<Integer, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Integer... params) {
                        while (adapter.getItemCount() <= params[0]) {
                            SystemClock.sleep(100);
                            if (!isLoading) {
                                break;
                            }
                        }
                        return params[0];
                    }

                    @Override
                    protected void onPostExecute(Integer index) {
                        layoutManager.scrollToPositionWithOffset(index, 0);
                        if (lastQuery != null) {
                            adapter.selectText(lastQuery, false, colorFoundedText);
                        }
                        moveToIndex = null;
                        if (lastIndex != null) {
                            scrollToIndex(lastIndex);
                        }
                    }
                }.execute(index);
            } else {
                lastIndex = index;
            }
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
                lastIndent = speakIndex;
                selectText(speakIndex, phrases.get(phraseIndex));
                scrollToVisible(speakIndex);
            });
            TTSService.setIndexSpeakFinished(speakIndex -> {
                selectText(speakIndex, null);
            });
            colorFoundedText = getResources().getColor(R.color.red_dark);
            colorSpeakingText = getResources().getColor(R.color.DeepSkyBlue);
        } else {
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void scrollToVisible(int index) {
        index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
        int first = findFirstVisibleItemPosition(true) ;
        int last = findLastVisibleItemPosition(true);
        if(first > index || index > last) {
            scrollToIndex(index);
        }
    }

    private void selectText(int index, String text) {
        ItemListAdapter.ViewHolder holder = adapter.getHolder(index);
        if (holder != null) {
            GuiUtils.selectText(holder.getView(R.id.work_text_indent), true, text, colorSpeakingText);
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

        public WorkFragmentAdaptor() {
            super(true, R.layout.work_list_header, R.layout.indent_item);
        }

        @Override
        public int getLayoutId(String item) {
            return R.layout.indent_item;
        }

        @Override
        public void onClick(View view, int position) {
            position -= firstIsHeader;
            if(!TTSService.isReady(work)) {
                WorkFragment.this.selectText(lastIndent, null);
                Intent i = new Intent(getActivity(), TTSService.class);
                i.putExtra(Constants.ArgsName.WORK, work);
                i.putExtra(Constants.ArgsName.TTS_PLAY_POSITION, position);
                getActivity().startService(i);
            } else {
                if (TTSService.getInstance().getPlayer().isSpeaking()) {
                    if (lastIndent != position) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.STOP);
                    } else {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PAUSE);
                    }
                } else {
                    if (lastIndent == position && TTSService.getInstance().getState() == TTSPlayer.State.PAUSE) {
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.PLAY);
                    } else {
                        WorkFragment.this.selectText(lastIndent, null);
                        TTSNotificationBroadcast.sendMessage(TTSService.Action.POSITION, position);
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
