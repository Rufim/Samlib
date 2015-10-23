package ru.samlib.client.fragments;

import android.os.*;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.lister.Lister;
import ru.samlib.client.util.GuiUtils;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

import java.util.List;

/**
 * Created by Rufim on 17.01.2015.
 */
public abstract class ListFragment<I> extends BaseFragment implements SearchView.OnQueryTextListener {

    private static final String TAG = ListFragment.class.getSimpleName();

    private final int filteringCooldown = 300;

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

    protected SearchView searchView;
    protected ItemListAdapter<I> adapter;
    protected LinearLayoutManager layoutManager;
    protected VerticalRecyclerViewFastScroller scroller;
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
    protected boolean enableFiltering = false;
    protected long lastFilteringTime = 0;


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
        if (savedLister != null) {
            lister = savedLister;
            refreshData(true);
            savedLister = null;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.isEmpty() && !searchView.isIconified() && searchView.hasFocus()) {
            searchView.clearFocus();
            ;
            onSearchViewClose(searchView);
        }
        return enableFiltering;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (enableFiltering) {
            adapter.enterFilteringMode();
            if (filterTask == null) {
                lastQuery = null;
                filterTask = new FilterTask(query);
                getActivity().runOnUiThread(filterTask);
            } else {
                lastQuery = query;
            }
            return true;
        } else {
            return false;
        }
    }

    protected void onSearchViewClose(SearchView searchView) {
        if (searchView != null) {
            if (enableFiltering) {
                searchView.setOnCloseListener(() -> {
                    lastQuery = null;
                    adapter.exitFilteringMode();
                    return false;
                });
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem searchItem = menu.findItem(R.id.search);
        if (searchItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            if (enableFiltering) {
                searchView.setQueryHint(getString(R.string.filter_hint));
                searchView.setSuggestionsAdapter(null);
            }
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(() -> {
                onSearchViewClose(searchView);
                return false;
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

    protected void loadElements(int count, boolean showProgress, AsyncTask onElementsLoadedTask, Object... params) {
        if (isLoading || isEnd) {
            return;
        }
        if (showProgress) {
            startLoading();
        }
        if (lister != null) {
            ListerTask listerTask = new ListerTask(count, onElementsLoadedTask, params);
            if (this.listerTask == null) {
                listerTask.execute();
            }
            this.listerTask = listerTask;
        }
    }

    protected void loadElements(int count, boolean showProgress) {
        loadElements(count, showProgress, null, null);
    }

    protected abstract ItemListAdapter<I> getAdapter();


    protected void clearData() {
        absoluteCount = 0;
        pastVisiblesItems = 0;
        isEnd = false;
        if(adapter != null) {
            adapter.getItems().clear();
            adapter.notifyDataSetChanged();
        }
    }

    public void refreshData(boolean showProgress) {
        clearData();
        loadElements(pageSize, showProgress);
    }


    public int findFirstVisibleItemPosition(boolean completelyVisible) {
        final View child = findOneVisibleChild(0, layoutManager.getChildCount(), completelyVisible, !completelyVisible);
        return child == null ? RecyclerView.NO_POSITION : itemList.getChildAdapterPosition(child);
    }

    public int findLastVisibleItemPosition(boolean completelyVisible) {
        final View child = findOneVisibleChild(layoutManager.getChildCount() - 1, -1, completelyVisible, !completelyVisible);
        return child == null ? RecyclerView.NO_POSITION : itemList.getChildAdapterPosition(child);
    }

    protected View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible,
                                       boolean acceptPartiallyVisible) {
        OrientationHelper helper;
        if (layoutManager.canScrollVertically()) {
            helper = OrientationHelper.createVerticalHelper(layoutManager);
        } else {
            helper = OrientationHelper.createHorizontalHelper(layoutManager);
        }

        final int start = helper.getStartAfterPadding();
        final int end = helper.getEndAfterPadding();
        final int next = toIndex > fromIndex ? 1 : -1;
        View partiallyVisible = null;
        for (int i = fromIndex; i != toIndex; i += next) {
            final View child = layoutManager.getChildAt(i);
            final int childStart = helper.getDecoratedStart(child);
            final int childEnd = helper.getDecoratedEnd(child);
            if (childStart < end && childEnd > start) {
                if (completelyVisible) {
                    if (childStart >= start && childEnd <= end) {
                        return child;
                    } else if (acceptPartiallyVisible && partiallyVisible == null) {
                        partiallyVisible = child;
                    }
                } else {
                    return child;
                }
            }
        }
        return partiallyVisible;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_loading_list, container,
                false);
        ButterKnife.bind(this, rootView);
        swipeRefresh.setOnRefreshListener(() -> {
            if (!isLoading) {
                refreshData(false);
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
                    loadElements(pageSize, true);
                }
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            scroller = (VerticalRecyclerViewFastScroller) rootView.findViewById(R.id.fast_scroller);
            scroller.setRecyclerView(itemList);
            GuiUtils.fadeOut(scroller, 0, 100);
            itemList.addOnScrollListener(scroller.getOnScrollListener());
            itemList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    GuiUtils.fadeIn(scroller, 0, 100);
                    GuiUtils.fadeOut(scroller, 2000, 1000);
                }
            });
        }
        if (adapter != null) {
            if (lister != null) {
                isLoading = true;
                if (listerTask != null) {
                    listerTask.cancel(true);
                }
                listerTask = new ListerTask(pageSize);
                listerTask.execute();
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

    public class ListerTask extends AsyncTask<Void, Void, List<I>> {

        private int count = 0;
        private AsyncTask onElementsLoadedTask;
        private Object[] LoadedTaskParams;

        public ListerTask(int count) {
            this.count = count;
        }

        public ListerTask(int count, AsyncTask onElementsLoadedTask, Object[] LoadedTaskParams) {
            this.count = count;
            this.onElementsLoadedTask = onElementsLoadedTask;
            this.LoadedTaskParams = LoadedTaskParams;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<I> doInBackground(Void... params) {
            List<I> items = lister.getItems(absoluteCount, count);
            if (items.size() == 0) {
                return items;
            }
            List<I> foundItems = adapter.find(adapter.getLastQuery(), items);
            while (count > foundItems.size()) {
                foundItems = lister.getItems(absoluteCount + items.size(), count);
                if (foundItems.size() == 0) {
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
            if (itemList != null) {
                if (result.size() == 0) {
                    isEnd = true;
                } else {
                    adapter.addItems(result);
                }
                absoluteCount = adapter.getAbsoluteItemCount();
                stopLoading();
                if (onElementsLoadedTask != null) {
                    onElementsLoadedTask.execute(LoadedTaskParams);
                }
                if (this != listerTask) {
                    listerTask.execute();
                }
                listerTask = null;
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
            if (itemList != null) {
                itemList.scrollToPosition(adapter.getItemCount() - adapter.getItems().size());
                adapter.filter(query);
                filterTask = null;
                if (lastQuery != null) {
                    long current = SystemClock.currentThreadTimeMillis();
                    if (current - lastFilteringTime < filteringCooldown) {
                        Handler mainHandler = new Handler(getActivity().getMainLooper());
                        mainHandler.postDelayed(() -> onQueryTextChange(lastQuery), current - lastFilteringTime);
                    } else {
                        lastFilteringTime = current;
                        onQueryTextChange(lastQuery);
                    }
                } else if (adapter.getItemCount() < pageSize) {
                    loadElements(pageSize, true);
                }
            }
        }
    }
}
