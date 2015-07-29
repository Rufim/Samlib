package ru.samlib.client.fragments;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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
import ru.samlib.client.util.PicassoImageHandler;

import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<Element> implements TextToSpeech.OnInitListener {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Queue<Integer> searched = new ArrayDeque<>();
    private AsyncTask moveToIndex;
    private Integer lastIndex;
    private TextToSpeech tts;
    private int speakIndex = 0;
    private int phraseIndex = 0;
    private List<String> phrases;
    private int maxPhraseSize = 200;

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
        tts = new TextToSpeech(this.getActivity(), this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
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
    public boolean onQueryTextChange(String query) {
        searched.clear();
        searched.addAll(search(query));
        adapter.selectText(query, Color.RED);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
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
                            adapter.selectText(lastQuery, Color.RED);
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

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceCompletedListener(utteranceId -> {
                nextPhrase();
            });
            tts.setLanguage(Locale.getDefault());
            tts.setSpeechRate(1.3f);
        } else {
            tts = null;
            Toast.makeText(this.getActivity(), "Failed to initialize TTS engine.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startSpeak() {
        if(tts.isSpeaking() || speakIndex >= work.getRootElements().size()) {
            tts.stop();
        }
        phrases = new LinkedList<>(Arrays.asList(work.getRootElements().get(speakIndex).text().split("[.!?]")));
        for (int i = 0; i < phrases.size(); i++) {
            String phrase = phrases.get(i);
            if(phrase.length() >= maxPhraseSize) {
                List<String> newPhrases = new ArrayList<>();
                while (phrase.length() >= maxPhraseSize) {
                    newPhrases.add(phrase.substring(0, maxPhraseSize));
                    phrase = phrase.substring(maxPhraseSize);
                }
                phrases.remove(i);
                newPhrases.add(phrase);
                phrases.addAll(i,newPhrases);
            }
        }
        phraseIndex = 0;
        nextPhrase();
    }

    public void nextPhrase() {
        if(phraseIndex >= phrases.size()) {
            speakIndex++;
            startSpeak();
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId");
        tts.speak(phrases.get(phraseIndex), TextToSpeech.QUEUE_FLUSH, params);
        phraseIndex++;
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
            if (tts.isSpeaking()) {
                tts.stop();
            } else {
                speakIndex = position - firstIsHeader;
                startSpeak();
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
               /*    spanner.registerHandler("a", new TagNodeHandler() {
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
                    selectText(holder, lastQuery, Color.RED);
                    break;
            }
        }
    }
}
