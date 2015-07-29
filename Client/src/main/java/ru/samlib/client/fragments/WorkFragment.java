package ru.samlib.client.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Browser;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.nd.android.sdp.im.common.widget.htmlview.view.HtmlView;
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import org.htmlcleaner.TagNode;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.R;
import ru.samlib.client.activity.AuthorActivity;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Chapter;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.domain.events.ChapterSelectedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<Element> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;

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
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(ChapterSelectedEvent event) {
        if(adapter.getItemCount() > event.chapter.getIndex()) {
            layoutManager.scrollToPositionWithOffset(event.chapter.getIndex(), 0);
        } else {
            loadElements(event.chapter.getIndex() + pageSize);
            new AsyncTask<Chapter, Void, Chapter>(){

                @Override
                protected Chapter doInBackground(Chapter... params) {
                    while (adapter.getItemCount() <= params[0].getIndex()) {
                        SystemClock.sleep(100);
                        if(!isLoading) {
                            break;
                        }
                    }
                    return params[0];
                }

                @Override
                protected void onPostExecute(Chapter chapter) {
                    layoutManager.scrollToPositionWithOffset(chapter.getIndex(), 0);
                }
            }.execute(event.chapter);

        }
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

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
           switch (holder.getItemViewType()){
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
                   break;
           }
        }
    }
}
