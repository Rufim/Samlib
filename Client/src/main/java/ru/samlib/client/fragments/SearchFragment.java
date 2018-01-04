package ru.samlib.client.fragments;

import android.os.Bundle;


import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.vrallev.android.cat.Cat;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.dialog.FilterDialog;
import ru.samlib.client.dialog.SearchFilterDialog;
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

    public static SearchFragment show(BaseFragment fragment, String query) {
        return show(fragment, SearchFragment.class, Constants.ArgsName.SEARCH_QUERY, query);
    }

    public SearchFragment() {
        enableSearch = true;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search_filter:
                SearchFilterDialog dialog = (SearchFilterDialog) getFragmentManager().findFragmentByTag(SearchFilterDialog.class.getSimpleName());
                if (dialog == null) {
                    dialog = new SearchFilterDialog();
                    dialog.setState(statParser);
                    dialog.show(getFragmentManager(), SearchFilterDialog.class.getSimpleName());
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        statParser.setQuery(query);
        refreshData(true);
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
        if(query != null) {
            statParser.setQuery(query);
        }
        pageSize = 20;
        return new DataSource<Work>() {
            @Override
            public List<Work> getItems(int skip, int size) throws Exception {
                List<Work> works =  statParser.getPage((skip/pageSize) + 1);
                if(works.size() < pageSize) {
                    isEnd = true;
                }
                return works;
            }
        };
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        Cat.e(ex);
        ErrorFragment.show(this, R.string.stat_server_not_available);
    }

    @Override
    protected void firstLoad(boolean scroll) {
        if(query != null) {
            super.firstLoad(scroll);
        } else {
            stopLoading();
        }
    }

    @Override
    public void refreshData(boolean showProgress) {
        super.refreshData(showProgress);
    }

    protected class SearchArrayAdapter extends ItemListAdapter<Work> {


        public SearchArrayAdapter() {
            super(R.layout.item_search);
            bindRoot = true;
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
