package ru.samlib.client.fragments;

import android.graphics.PointF;
import android.os.*;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import de.greenrobot.event.EventBus;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.TextUtils;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

import java.io.IOException;
import java.util.List;

/**
 * Created by Rufim on 17.01.2015.
 */
public abstract class ListFragment<I> extends BaseFragment implements SearchView.OnQueryTextListener {

    private static final String TAG = ListFragment.class.getSimpleName();

    private final int filteringCooldown = 300;

    @Bind(R.id.load_progress)
    protected ProgressBar progressBar;
    @Bind(R.id.loading_text)
    protected TextView loadingText;
    @Bind(R.id.load_more)
    protected ProgressBar loadMoreBar;
    @Bind(R.id.items)
    protected RecyclerView itemList;
    @Bind(R.id.refresh)
    protected SwipeRefreshLayout swipeRefresh;

    protected SearchView searchView;
    protected ItemListAdapter<I> adapter;
    protected LinearLayoutManager layoutManager;
    protected VerticalRecyclerViewFastScroller scroller;
    protected DataSource<I> savedDataSource;
    protected DataSource<I> dataSource;

    //
    protected int pageSize = 50;
    protected volatile boolean isLoading = false;
    protected volatile boolean isEnd = false;
    protected int currentCount = 0;
    protected int pastVisibleItems = 0;
    protected DataTask dataTask;
    protected FilterTask filterTask;
    protected MoveTask moveToIndex;
    protected FilterEvent lastSearchQuery;
    protected boolean enableFiltering = false;
    protected boolean enableScrollbar = true;
    protected long lastFilteringTime = 0;

    public ListFragment() {
    }

    public ListFragment(DataSource<I> dataSource) {
        this.dataSource = dataSource;
    }

    public void setDataSource(DataSource<I> dataSource) {
        this.dataSource = dataSource;
    }

    public void saveLister() {
        savedDataSource = dataSource;
    }

    public boolean restoreLister() {
        if (savedDataSource != null) {
            dataSource = savedDataSource;
            refreshData(true);
            savedDataSource = null;
            return true;
        } else {
            return false;
        }
    }

    protected FilterEvent getNewFilterEvent(String query) {
        return new FilterEvent(query);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.isEmpty() && !searchView.isIconified() && searchView.hasFocus()) {
            searchView.clearFocus();
            onSearchViewClose(searchView);
        }
        return enableFiltering;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(lastSearchQuery == null) {
            lastSearchQuery = getNewFilterEvent(query);
        } else {
            lastSearchQuery.query = query;
        }
        if (enableFiltering) {
            filter(lastSearchQuery);
            return true;
        } else {
            return false;
        }
    }

    public void filter(FilterEvent filterEvent) {
        adapter.enterFilteringMode();
        if (filterTask == null) {
            lastSearchQuery = null;
            filterTask = new FilterTask(filterEvent);
            getActivity().runOnUiThread(filterTask);
        }
    }

    protected void onSearchViewClose(SearchView searchView) {
        if (searchView != null) {
            if (enableFiltering) {
                lastSearchQuery = null;
                adapter.exitFilteringMode();
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

    public void startLoading(boolean showProgress) {
        isLoading = true;
        if (loadMoreBar != null && showProgress && progressBar.getVisibility() != View.VISIBLE) {
            loadMoreBar.setVisibility(View.VISIBLE);
        }
    }

    public void stopLoading() {
        isLoading = false;
        if (loadMoreBar != null) {
            loadMoreBar.setVisibility(View.GONE);
        }
        itemList.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    protected void loadItems(int count, boolean showProgress, AsyncTask onElementsLoadedTask, Object... params) {
        if (isLoading || isEnd) {
            return;
        }
        if (dataSource != null && dataTask == null) {
            startLoading(showProgress);
            DataTask dataTask = new DataTask(count, onElementsLoadedTask, params);
            dataTask.execute();
        }
    }

    protected void loadItems(boolean showProgress) {
        loadItems(pageSize, showProgress, null, null);
    }

    protected void loadItems(boolean showProgress, AsyncTask onElementsLoadedTask, Object... params) {
        loadItems(pageSize, showProgress, onElementsLoadedTask, params);
    }


    protected void loadItems(int count, boolean showProgress) {
        loadItems(count, showProgress, null, null);
    }

    protected abstract ItemListAdapter<I> getAdapter();

    protected DataSource<I> getDataSource() throws Exception {
        return dataSource;
    }

    protected void clearData() {
        currentCount = 0;
        pastVisibleItems = 0;
        isEnd = false;
        if (adapter != null) {
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
    }

    public void refreshData(boolean showProgress) {
        if (dataSource != null) {
            clearData();
            loadItems(showProgress);
        }
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

    private boolean isItemVisible(int index) {
        index += ((MultiItemListAdapter) adapter).getFirstIsHeader();
        int first = findFirstVisibleItemPosition(true);
        int last = findLastVisibleItemPosition(true);
        if (first > index || index > last) {
            return false;
        }
        return true;
    }

    public void scrollToIndex(int index) {
        scrollToIndex(index, 0);
    }

    public void toIndex(int index, int offset) {
        layoutManager.scrollToPositionWithOffset(index, offset);
    }

    public void scrollToIndex(int index, int textOffset) {
        if (adapter.getItemCount() > index) {
            toIndex(index, textOffset);
        } else {
            moveToIndex = new MoveTask();
            loadItems(index + pageSize, true, moveToIndex, index, textOffset);
        }
    }

    public void smoothScrollToIndex(int position, int offset) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(itemList.getContext()) {

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                PointF calculate = layoutManager.computeScrollVectorForPosition(targetPosition);
                calculate.y += offset;
                return calculate;
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 25f / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, displayMetrics.densityDpi, displayMetrics);
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(linearSmoothScroller);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_loading_list, container,
                false);
        bind(rootView);
        swipeRefresh.setOnRefreshListener(() -> {
            if (!isLoading) {
                refreshData(false);
            }
        });
        if (adapter == null) {
            adapter = getAdapter();
        }
        try {
            setDataSource(getDataSource());
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception", e);
            ErrorFragment.show(ListFragment.this, R.string.error);
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
                pastVisibleItems = layoutManager.findFirstVisibleItemPosition();
                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                    loadItems(true);
                }
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && enableScrollbar) {
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
        } else {
            ((RelativeLayout)rootView).removeView(rootView.findViewById(R.id.fast_scroller));
        }
        if (adapter != null) {
           firstLoad(true);
        }
        return rootView;
    }

    protected void firstLoad(boolean scroll) {
        if (dataSource != null && !isEnd && adapter.getItems().isEmpty()) {
            loadMoreBar.setVisibility(View.GONE);
            loadItems(false);
        } else {
            stopLoading();
        }
        if (pastVisibleItems > 0 && scroll) {
            layoutManager.scrollToPositionWithOffset(pastVisibleItems, 0);
        }
    }

    public class DataTask extends AsyncTask<Void, Void, List<I>> {

        private int count = 0;
        private AsyncTask onElementsLoadedTask;
        private Object[] LoadedTaskParams;

        public DataTask(int count) {
            this.count = count;
        }

        public DataTask(int count, AsyncTask onElementsLoadedTask, Object[] LoadedTaskParams) {
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
            List<I> items = null;
            try {
                items = dataSource.getItems(currentCount, count);
                if (items.size() == 0) {
                    return items;
                }
            } catch (IOException e) {
                Log.e(TAG, "Cant get new Items", e);
                ErrorFragment.show(ListFragment.this, R.string.error_network);
            }
            return items;
        }

        @Override
        protected void onPostExecute(List<I> result) {
            super.onPostExecute(result);
            if (itemList != null) {
                int needMore = 0;
                if (result.size() == 0) {
                    isEnd = true;
                } else {
                    needMore = count - adapter.addItems(result).size();
                }
                currentCount = adapter.getAbsoluteItemCount();
                isLoading = false;
                dataTask = null;
                if(needMore <= 0) {
                    if (onElementsLoadedTask != null) {
                        onElementsLoadedTask.execute(LoadedTaskParams);
                    }
                    stopLoading();
                } else {
                    loadItems(needMore, true, onElementsLoadedTask, LoadedTaskParams);
                }
            }
        }
    }

    public class MoveTask extends AsyncTask<Object, Void, Void> {
        int index = 0;
        int offsetLines = 0;

        @Override
        protected Void doInBackground(Object... params) {
            index = (int) params[0];
            offsetLines = (int) params[1];
            return null;
        }

        @Override
        protected void onPostExecute(Void empty) {
            if (this == moveToIndex) {
                toIndex(index, offsetLines);
            }
        }
    }

    public class FilterTask implements Runnable {

        private final Object query;

        public FilterTask(Object query) {
            this.query = query;
        }

        @Override
        public void run() {
            if (itemList != null) {
                itemList.scrollToPosition(adapter.getItemCount() - adapter.getItems().size());
                adapter.filter(query);
                filterTask = null;
                if (lastSearchQuery != null) {
                    long current = SystemClock.elapsedRealtime();
                    if (current - lastFilteringTime < filteringCooldown) {
                        Handler mainHandler = new Handler(getActivity().getMainLooper());
                        mainHandler.postDelayed(() -> filter(lastSearchQuery), current - lastFilteringTime);
                    } else {
                        lastFilteringTime = current;
                        filter(lastSearchQuery);
                    }
                } else {
                    loadItems(true);
                }
            }
        }
    }

    public static class FilterEvent implements Event {
        public String query;

        public FilterEvent(String query) {
            this.query = query;
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(query);
        }

        @Override
        public String toString() {
            return query;
        }
    }
}


