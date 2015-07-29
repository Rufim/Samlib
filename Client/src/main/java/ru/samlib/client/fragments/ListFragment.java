package ru.samlib.client.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Filterable;
import ru.samlib.client.lister.Lister;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 17.01.2015.
 */
public abstract class ListFragment<I> extends BaseFragment implements SearchView.OnQueryTextListener {

    @Bind(R.id.load_progress)
    protected ProgressBar progressBar;
    @Bind(R.id.load_more)
    protected ProgressBar loadMoreBar;
    @Bind(R.id.loading_text)
    protected TextView loadingText;
    @Bind(R.id.items)
    protected RecyclerView itemList;
    @Bind(R.id.refresh)
    protected SwipeRefreshLayout swipeRefresh;
    protected ItemListAdapter<I> adapter;
    protected LinearLayoutManager layoutManager;
    protected Lister<I> savedLister;
    protected Lister<I> lister;

    //
    protected int pageSize = 30;
    protected volatile boolean isLoading = false;
    protected volatile boolean isEnd = false;
    protected int absoluteCount = 0;
    protected int pastVisiblesItems = 0;
    protected ListerTask task;

    public ListFragment() {
    }

    public ListFragment(Lister<I> lister) {
        this.lister = lister;
    }
    
    public void setLister(Lister<I> lister) {
        this.lister = lister;
    }

    public void saveLister() {
        savedLister = lister;
    }

    public boolean restoreLister() {
        if(savedLister != null) {
            lister = savedLister;
            refreshData();
            savedLister = null;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        adapter.enterFilteringMode();
        itemList.scrollToPosition(adapter.filter(query));
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.search);
        if (searchItem != null) {
            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnQueryTextListener(this);
        }
    }

    public void startLoading() {
        isLoading = true;
        if (loadMoreBar != null) {
            loadMoreBar.setVisibility(View.VISIBLE);
        }
    }

    public void stopLoading() {
        isLoading = false;
        if (loadMoreBar != null) {
            loadMoreBar.setVisibility(View.INVISIBLE);
        }
        itemList.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    protected void loadElements(int count) {
        if (isLoading || isEnd) {
            return;
        }
        adapter.exitFilteringMode();
        startLoading();
        if(lister != null) {
            task = (ListerTask) new ListerTask().execute(absoluteCount, count);
        }
    }

    protected abstract ItemListAdapter<I> getAdapter();

    public void refreshData(){
        absoluteCount = 0;
        pastVisiblesItems = 0;
        isEnd = false;
        adapter.getItems().clear();
        adapter.notifyDataSetChanged();
        loadElements(pageSize);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_loading_list, container,
                false);
        ButterKnife.bind(this, rootView);
        swipeRefresh.setOnRefreshListener(() -> refreshData());
        if (adapter == null) {
            adapter = getAdapter();
        }
        layoutManager = new LinearLayoutManager(rootView.getContext());
        itemList.setLayoutManager(layoutManager);
        itemList.setAdapter(adapter);
        itemList.setItemAnimator(new DefaultItemAnimator());
        itemList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int visibleItemCount, totalItemCount;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager mLayoutManager = itemList.getLayoutManager();
                visibleItemCount = mLayoutManager.getChildCount();
                totalItemCount = mLayoutManager.getItemCount();
                pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();
                if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                    loadElements(pageSize);
                }
            }
        });

        if (adapter != null) {
            if (task == null && lister != null) {
                task = (ListerTask) new ListerTask().execute(absoluteCount, pageSize);
            } else {
                stopLoading();
                layoutManager.scrollToPositionWithOffset(pastVisiblesItems, 0);
            }
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public class ListerTask extends AsyncTask<Integer, Void, List<I>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<I> doInBackground(Integer... params) {
            return lister.getItems(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(List<I> result) {
            super.onPostExecute(result);
            if(itemList != null) {
                if (result.size() == 0) {
                    isEnd = true;
                } else {
                    adapter.addItems(result);
                }
                absoluteCount = adapter.getItemCount();
                stopLoading();
            }
        }
    }
}
