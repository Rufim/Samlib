package ru.samlib.client.fragments;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;

import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<Element> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private Elements dds;

    public WorkFragment() {
        setLister(((skip, size) -> {
            while (work == null) {
                SystemClock.sleep(10);
            }
            if (!work.isParsed()) {
                try {
                    work = new WorkParser(work).parse();
                    postEvent(new WorkParsedEvent(work));
                    dds = work.getParsedContent().select("body > dd,pre,div,font");
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            return Stream.of(dds)
                    .skip(skip)
                    .limit(size)
                    .collect(Collectors.toList());
        }));
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
                   view.setText(spanner.fromHtml(indent.outerHtml()));
                   break;
           }
        }
    }
}
