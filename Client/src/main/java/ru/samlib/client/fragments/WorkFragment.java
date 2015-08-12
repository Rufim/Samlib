package ru.samlib.client.fragments;

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
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;
import ru.samlib.client.util.TTSPlayer;

import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<Element> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Queue<Integer> searched = new ArrayDeque<>();
    private AsyncTask moveToIndex;
    private Integer lastIndex;
    private TTSPlayer ttsPlayer;
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
            return Stream.of(work.getRootElements())
                    .skip(skip)
                    .limit(size)
                    .collect(Collectors.toList());
        }));
    }

    @Override
    public void onStart() {
        super.onStart();
        ttsPlayer.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        ttsPlayer.onStop();
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
            searchView.setQueryHint(getString(R.string.search_hint));
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searched.clear();
        adapter.selectText(query, colorFoundedText);
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
            moveToIndex(index);
        }
        return true;
    }

    private void moveToIndex(int index) {
        if (adapter.getItemCount() > index) {
            lastIndex = null;
            layoutManager.scrollToPositionWithOffset(index, 0);
            adapter.selectText(lastQuery, Color.RED);
        } else {
            if (moveToIndex == null) {
                lastIndex = null;
                loadElements(index + pageSize);
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
                            adapter.selectText(lastQuery, colorFoundedText);
                        }
                        moveToIndex = null;
                        if (lastIndex != null) {
                            moveToIndex(lastIndex);
                        }
                    }
                }.execute(index);
            } else {
                lastIndex = index;
            }
        }
    }

    public void onEvent(ChapterSelectedEvent event) {
        moveToIndex(event.chapter.getIndex());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        if (work == null || !work.getLink().equals(link)) {
            work = new Work(link);
            if (ttsPlayer != null) {
                ttsPlayer.onStop();
            }
            ttsPlayer = new TTSPlayer(work, getActivity());
            ttsPlayer.setNextPhraseListener((speakIndex, phraseIndex, phrase) -> {
                lastIndent = speakIndex;
                ItemListAdapter.ViewHolder holder = adapter.getHolder(speakIndex);
                if (holder != null) {
                    GuiUtils.selectText(holder.getView(R.id.work_text_indent), phrase, colorSpeakingText);
                }

            });
            ttsPlayer.setIndexSpeakFinished(speakIndex -> {
                ItemListAdapter.ViewHolder holder = adapter.getHolder(speakIndex);
                if (holder != null) {
                    GuiUtils.selectText(holder.getView(R.id.work_text_indent), null, colorSpeakingText);
                }
            });
            colorFoundedText = getResources().getColor(R.color.red_dark);
            colorSpeakingText = getResources().getColor(R.color.DeepSkyBlue);
        } else {
            EventBus.getDefault().post(new WorkParsedEvent(work));
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected ItemListAdapter<Element> getAdapter() {
        return new WorkFragmentAdaptor();
    }

    public Work getWork() {
        return work;
    }

    private class WorkFragmentAdaptor extends MultiItemListAdapter<Element> {

        public WorkFragmentAdaptor() {
            super(true, R.layout.work_list_header, R.layout.indent_item);
        }

        @Override
        public int getLayoutId(Element item) {
            return R.layout.indent_item;
        }

        @Override
        public void onClick(View view, int position) {
            if (ttsPlayer.isSpeaking()) {
                if(lastIndent != position - firstIsHeader) {
                    ttsPlayer.stop();
                } else {
                    ttsPlayer.pause();
                }
            } else {
                if (lastIndent == position - firstIsHeader && ttsPlayer.getState() == TTSPlayer.STATE.PAUSE) {
                    ttsPlayer.resume();
                } else {
                    ttsPlayer.startSpeak(position - firstIsHeader);
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
                    Element indent = getItem(position);
                    TextView view = holder.getView(R.id.work_text_indent);
                    HtmlSpanner spanner = new HtmlSpanner();
                    spanner.registerHandler("img", new PicassoImageHandler(view));
               /*   spanner.registerHandler("a", new TagNodeHandler() {
                       @Override
                       public void handleTagNode(TagNode node, SpannableStringBuilder builder, int start, int end) {
                           final String href = node.getAttributeByName("href");
                           builder.setSpan(new ClickableSpan() {
                               @Override
                               public void onClick(View widget) {
                                   Uri uri = Uri.parse(href);
                                   Context context = widget.getContext();
                                   Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                   intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                                   context.startActivity(intent);
                               }
                           }, start, builder.length(), 33);
                       }
                   }); */

                    view.setMovementMethod(LinkMovementMethod.getInstance());
                    view.setText(spanner.fromHtml(indent.outerHtml()));
                    break;
            }
            selectText(holder, WorkFragment.this.lastQuery, colorFoundedText);
        }
    }
}
