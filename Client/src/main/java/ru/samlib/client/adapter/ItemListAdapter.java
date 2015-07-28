package ru.samlib.client.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ru.samlib.client.util.GuiUtils;

import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public abstract class ItemListAdapter<I> extends RecyclerView.Adapter<ItemListAdapter.ViewHolder> implements View.OnClickListener,
        View.OnLongClickListener {

    protected final List<I> items;
    protected List<I> originalItems = null;
    protected final int layoutId;

    // Adapter's Constructor
    public ItemListAdapter() {
        this.items = new ArrayList<>();
        this.layoutId = -1;
    }

    // Adapter's Constructor
    public ItemListAdapter(int layoutId) {
        this.items = new ArrayList<>();
        this.layoutId = layoutId;
    }

    // Adapter's Constructor
    public ItemListAdapter(List<I> items, int layoutId) {
        this.items = items;
        this.layoutId = layoutId;
    }

    // Create new views. This is invoked by the layout manager.
    @Override
    public ItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view by inflating the row item xml.
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        ViewHolder holder = newHolder(itemView);
        holder.bindViews(ItemListAdapter.this);
        return holder;
    }

    protected ViewHolder newHolder(View item) {
        return new ViewHolder(item) {
            @Override
            public List<View> getViews(View itemView) {
                return GuiUtils.getAllChildren(itemView);
            }
        };
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.items != null ? this.items.size() : 0;
    }


    public List<I> getItems() {
        return this.items;
    }

    public void addItems(List<I> items) {
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    public void addItem(I item) {
        this.items.add(item);
        notifyDataSetChanged();
    }


    public void addItem(int position, I model) {
        items.add(position, model);
        notifyItemInserted(position);
    }

    public I removeItem(int position) {
        final I item = this.items.remove(position);
        notifyItemRemoved(position);
        return item;
    }

    public void moveItem(int fromPosition, int toPosition) {
        final I item = this.items.remove(fromPosition);
        items.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void changeTo(List<I> items) {
        applyAndAnimateRemovals(items);
        applyAndAnimateAdditions(items);
        applyAndAnimateMovedItems(items);
    }

    private void applyAndAnimateRemovals(List<I> newItems) {
        for (int i = items.size() - 1; i >= 0; i--) {
            final I model = this.items.get(i);
            if (!newItems.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<I> newItems) {
        for (int i = 0, count = newItems.size(); i < count; i++) {
            final I model = newItems.get(i);
            if (!this.items.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<I> newItems) {
        for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
            final I model = newItems.get(toPosition);
            final int fromPosition = this.items.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    @Override
    public void onClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null) {
            onClick(view, holder.getPosition());
        }
    }

    public abstract void onClick(View view, int position);

    // Implement OnLongClick listener.
    @Override
    public boolean onLongClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null) {
            onLongClick(view, holder.getPosition());
        }
        return false;
    }

    public void onLongClick(View view, int position) {}


    // Create the ViewHolder class to keep references to your views
    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        private static final String TAG = ViewHolder.class.getSimpleName();

        protected HashMap<Integer, View> views;

        /**
         * Constructor
         *
         * @param v The container view which holds the elements from the row item xml
         */
        public ViewHolder(View v) {
            super(v);
            cacheViews(v);
        }

        private void cacheViews(View itemView) {
            List<View> views = getViews(itemView);
            this.views = new HashMap<>(views.size());
            for (View view : views) {
                this.views.put(view.getId(), view);
            }
            onCreateHolder(itemView);
        }

        public abstract List<View> getViews(View itemView);

        public void onCreateHolder(View itemView){}

        protected ViewHolder bindViews(ItemListAdapter adapter) {
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                View view = viewEntry.getValue();
                view.setOnClickListener(adapter);
                view.setOnLongClickListener(adapter);
                view.setTag(ViewHolder.this);
            }
            return this;
        }

        public <V extends View> V getView(int id) {
            return (V) views.get(id);
        }

        public void removeView(int id) {
            GuiUtils.removeView(views.get(id));
        }

        public View replaceView(int id, View newView) {
           return views.put(id, newView);
        }

        public View getItemView() {
            return super.itemView;
        }
    }
}