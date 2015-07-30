package ru.samlib.client.fragments;

import android.graphics.Color;
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
import ru.samlib.client.lister.Lister;

import java.util.List;
import java.util.Objects;

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
    protected int pageSize = 50;
    protected volatile boolean isLoading = false;
    protected volatile boolean isEnd = false;
    protected int absoluteCount = 0;
    protected int pastVisiblesItems = 0;
    protected ListerTask listerTask;
    protected FilterTask filterTask;
    protected String lastQuery;


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
        if(filterTask == null) {
            lastQuery = null;
            filterTask = new FilterTask(query);
            getActivity().runOnUiThread(filterTask);
        } else {
            lastQuery = query;
        }
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
            searchView.setOnCloseListener(() -> {
                lastQuery = null;
                adapter.exitFilteringMode();
                return false;
            });
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
        startLoading();
        if(lister != null) {
            listerTask = (ListerTask) new ListerTask().execute(absoluteCount, count);
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
        swipeRefresh.setOnRefreshListener(() -> {
            if(!isLoading) {
                refreshData();
            }
        });
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
            if (listerTask == null && lister != null) {
                isLoading = true;
                listerTask = (ListerTask) new ListerTask().execute(absoluteCount, pageSize);
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

        private int count = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<I> doInBackground(Integer... params) {
            List<I> items = lister.getItems(params[0], params[1]);
            if(items.size() == 0) {
                return items;
            }
            List<I> foundItems = adapter.find(adapter.getLastQuery(), items);
            while (params[1] > foundItems.size()) {
                foundItems = lister.getItems(params[0] + items.size(), params[1]);
                if(foundItems.size() == 0) {
                    break;
                }
                items.addAll(foundItems);
                foundItems = adapter.find(adapter.getLastQuery(), items);
            }
            return items;
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
                absoluteCount = adapter.getAbsoluteItemCount();
                stopLoading();
            }
        }
    }

    public class FilterTask implements Runnable {

        private final String query;

        public FilterTask(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            if(itemList != null) {
                itemList.scrollToPosition(adapter.getItemCount() - adapter.getItems().size());
                adapter.filter(query);
                filterTask = null;
                if (lastQuery != null) {
                    onQueryTextChange(lastQuery);
                } else {
                    if (adapter.getItemCount() < pageSize) {
                        loadElements(pageSize);
                    }
                }
            }
        }
    }
}
