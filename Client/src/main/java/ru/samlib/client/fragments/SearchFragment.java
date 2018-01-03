package ru.samlib.client.fragments;

import android.os.Bundle;


import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;

import ru.samlib.client.parser.SearchStatParser;
import ru.samlib.client.util.LinkHandler;
import ru.samlib.client.util.PicassoImageHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class SearchFragment extends ListFragment<Work> {

    private String query;

    SearchStatParser statParser;

    public static SearchFragment newInstance(String query) {
        Bundle args = new Bundle();
        args.putString(Constants.ArgsName.SEARCH_QUERY, query);
        return newInstance(SearchFragment.class, args);
    }

    public static SearchFragment show(BaseFragment fragment, String quaery) {
        return show(fragment, SearchFragment.class, Constants.ArgsName.SEARCH_QUERY, quaery);
    }

    public SearchFragment() {
        enableSearch = true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(ru.kazantsev.template.util.TextUtils.notEmpty(query)) {
            statParser.setQuery(query);  
            refreshData(true);
        }
        return true;
    }

    @Override
    protected ItemListAdapter<Work> newAdapter() {
        return new SearchArrayAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.search);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Work> newDataSource() throws Exception {
        query = getArguments().getString(Constants.ArgsName.SEARCH_QUERY);
        statParser = new SearchStatParser();
        pageSize = 10;
        return new DataSource<Work>() {
            @Override
            public List<Work> getItems(int skip, int size) throws Exception {
                return statParser.getPage(skip/pageSize + 1);
            }
        };
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.search);
    }

    protected class SearchArrayAdapter extends ItemListAdapter<Work> {


        public SearchArrayAdapter() {
            super(R.layout.item_search);
        }

        @Override
        public boolean onClick(View view, int position) {
            Linkable linkable = getItems().get(position);
            String link = linkable.getFullLink();
            SectionActivity.launchActivity(getContext(), link + ".shtml");
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.search_item_autor);
            TextView titleTextView = holder.getView(R.id.search_item_title);
            TextView subtitleTextView = holder.getView(R.id.search_item_subtitle);
            Work work = getItems().get(position);
            authorTextView.setText(work.getWorkAuthorName());
            titleTextView.setText("«" + work.getTitle() + "»");
            List<String> subtitle = new ArrayList<>();
            if (work.getType() != Type.OTHER) {
                subtitle.add(getString(R.string.item_form_label));
                subtitle.add(work.getType().getTitle());
            }
            if (work.getGenres() != null && work.getGenres().size() > 0) {
                subtitle.add(getString(R.string.item_genres_label));
                subtitle.add(work.printGenres());
            }
            subtitle.add(work.getSize().toString() + "k");
            HtmlSpanner spanner = new HtmlSpanner();
            spanner.registerHandler("img", new PicassoImageHandler(subtitleTextView));
            spanner.registerHandler("a", new LinkHandler(subtitleTextView));
            subtitleTextView.setText(TextUtils.join(" ", subtitle));
        }
    }
}
