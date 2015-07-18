package ru.samlib.client.adapter;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ru.samlib.client.R;
import ru.samlib.client.util.GuiUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 0shad on 13.07.2015.
 */
public abstract class MultiItemListAdapter<I> extends ItemListAdapter<I> {

    private static final String TAG = MultiItemListAdapter.class.getSimpleName();
    private static final int EMPTY_HEADER = -1;

    private final int[] layoutIds;
    private final boolean firstIsHeader;

    private int itemsSize = 0;

    public MultiItemListAdapter(boolean firstIsHeader, @LayoutRes int... layoutIds) {
        this.layoutIds = layoutIds;
        this.firstIsHeader = firstIsHeader;
    }

    public MultiItemListAdapter(List<I> items, boolean firstIsHeader, @LayoutRes int... layoutIds) {
        this(firstIsHeader, layoutIds);
        this.items.addAll(items);
    }

    // Create new views. This is invoked by the layout manager.
    @Override
    public ItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view by inflating the row item xml.
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType != 0) {
            if (Arrays.binarySearch(layoutIds, viewType) < 0) {
                Log.e(TAG, "Cannot resolve layout view type");
                return newHolder(inflater.inflate(R.layout.error, parent, false));
            } else {
                return newHolder(inflater.inflate(viewType, parent, false)).bindViews(MultiItemListAdapter.this);
            }
        } else {
            return newHolder(new View(parent.getContext()));
        }
    }

    public abstract
    @LayoutRes
    int getLayoutId(I item);

    public abstract List<I> getSubItems(I item);

    public abstract boolean hasSubItems(I item);

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return countItems();
    }

    @Override
    public void addItems(List<I> items) {
        super.addItems(items);
    }

    @Override
    public void addItem(I item) {
        super.addItem(item);
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return layoutIds[0];
        }
        I item = getItem(position);
        if (item != null) {
            return getLayoutId(item);
        }
        return 0;
    }

    public I getItem(int position) {
        return getItem(items, position - (hasHeader() ? 1 : 0), new AtomicInteger(0));
    }

    private I getItem(List<I> items, int position, AtomicInteger countWrapper) {
        int count = countWrapper.get();
        I result = null;
        for (I item : items) {
            if (position == count) {
                return item;
            }
            count++;
            if (hasSubItems(item)) {
                countWrapper.set(count);
                result = getItem(getSubItems(item), position, countWrapper);
                if (result != null) {
                    break;
                } else {
                    count = countWrapper.get();
                }
            }
        }
        countWrapper.set(count);
        return result;
    }

    private boolean hasHeader() {
        return firstIsHeader;
    }

    private int countItems() {
        return countItems(this.items) + (hasHeader() ? 1 : 0);
    }

    private int countItems(List<I> items) {
        int count = 0;
        if (items != null) {
            for (I item : items) {
                count++;
                if (hasSubItems(item)) {
                    count += countItems(getSubItems(item));
                }
            }
        }
        return count;
    }

}
