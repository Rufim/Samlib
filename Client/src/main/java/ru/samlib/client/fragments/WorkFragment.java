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
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.jsoup.nodes.Element;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.WorkParser;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class WorkFragment extends ListFragment<Element> {

    private static final String TAG = WorkFragment.class.getSimpleName();

    private Work work;
    private HtmlSpanner spanner;

    public WorkFragment() {
        spanner = new HtmlSpanner();
        setLister(((skip, size) -> {
            while (work == null) {
                SystemClock.sleep(10);
            }
            if (!work.isParsed()) {
                try {
                    work = new WorkParser(work).parse();
                    postEvent(new WorkParsedEvent(work));
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            return Stream.of(work.getParsedContent().select("dd"))
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


    private class WorkFragmentAdaptor extends MultiItemListAdapter<Element> {

        public WorkFragmentAdaptor() {
            super(true, R.layout.work_header, R.layout.indent_item);
        }

        @Override
        public int getLayoutId(Element item) {
            return R.layout.indent_item;
        }

        @Override
        public List<Element> getSubItems(Element item) {
            return null;
        }

        @Override
        public boolean hasSubItems(Element item) {
            return false;
        }

        @Override
        public void onClick(View view, int position) {

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
           switch (holder.getItemViewType()){
               case R.layout.work_header:
                   break;
               case R.layout.indent_item:
                   Element indent = getItem(position);
                   TextView view = holder.getView(R.id.work_text_indent);
                   spanner.registerHandler("img", new PicassoImageHandler(view));
                   GuiUtils.setTextOrHide(holder.getView(R.id.work_text_indent), "  " + spanner.fromHtml(indent.html()));
                   break;
           }
        }
    }
}
