package ru.samlib.client.adapter;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.util.GuiUtils;

import java.util.*;

/**
 * Created by Dmitry on 23.06.2015.
 */
public abstract class ItemListAdapter<I> extends RecyclerView.Adapter<ItemListAdapter.ViewHolder> implements View.OnClickListener,
        View.OnLongClickListener {

    protected List<I> items = new ArrayList<>();
    protected List<I> originalItems = null;
    protected Set<ViewHolder> currentHolders = Collections.newSetFromMap(new WeakHashMap<>());
    protected final  int layoutId;
    protected Object lastQuery;
    protected boolean bindViews = true;
    protected boolean bindClicks = true;

    // Adapter's Constructor
    protected ItemListAdapter() {
        this.layoutId = -1;
    }

    // Adapter's Constructor
    public ItemListAdapter(@LayoutRes int layoutId) {
        this.layoutId = layoutId;
    }

    // Adapter's Constructor
    public ItemListAdapter(List<I> items, @LayoutRes int layoutId) {
        this.items.addAll(items);
        this.layoutId = layoutId;
    }

    // Create new views. This is invoked by the layout manager.
    @Override
    public ItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view by inflating the row item xml.
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        ViewHolder holder = newHolder(itemView);
        if(bindViews) {
            holder.bindViews(ItemListAdapter.this, bindClicks);
        }
        return holder;
    }

    protected ViewHolder newHolder(View item) {
        ViewHolder holder = new ViewHolder(item) {
            @Override
            public List<View> getViews(View itemView) {
                return GuiUtils.getAllChildren(itemView);
            }
        };
        currentHolders.add(holder);
        return holder;
    }

    public Set<ViewHolder> getCurrentHolders() {
        return currentHolders;
    }

    public ViewHolder getHolder(int itemIndex) {
        for (ItemListAdapter.ViewHolder holder : getCurrentHolders()) {
            if (holder.getAdapterPosition() == itemIndex) {
                return holder;
            }
        }
        return null;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.items != null ? this.items.size() : 0;
    }

    public int getAbsoluteItemCount() {
        if (originalItems == null) {
            return this.items != null ? this.items.size() : 0;
        } else {
            return this.originalItems != null ? this.originalItems.size() : 0;
        }
    }

    public List<I> getItems() {
        return this.items;
    }

    public List<I> getOriginalItems() {
        return this.originalItems == null ? this.items : this.originalItems;
    }

    public void enterFilteringMode() {
        if (originalItems == null) {
            this.originalItems = new ArrayList<>(items);
        }
    }

    public void exitFilteringMode() {
        if (originalItems != null) {
            changeTo(originalItems);
            this.originalItems = null;
        }
    }

    public List<I> addItems(List<I> items) {
        if (originalItems == null) {
            this.items.addAll(items);
            notifyDataSetChanged();
            return items;
        } else {
            this.originalItems.addAll(items);
            List<I> added = new ArrayList<>(items);
            added.retainAll(filter(lastQuery));
            return added;
        }
    }

    public Object getLastQuery() {
        return lastQuery;
    }

    public void addItem(I item) {
        this.items.add(item);
        notifyItemInserted(this.items.size());
    }


    public void addItem(int position, I item) {
        this.items.add(position, item);
        notifyItemInserted(position);
    }

    public I removeItem(int position) {
        final I item = this.items.remove(position);
        notifyItemRemoved(position);
        return item;
    }

    public void moveItem(int fromPosition, int toPosition) {
        final I item = this.items.remove(fromPosition);
        this.items.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void selectText(ViewHolder holder, boolean erase, String query, int color) {
        if ("".equals(query)) {
            query = null;
        }
        for (TextView textView : holder.getViews(TextView.class)) {
            GuiUtils.selectText(textView, erase, query, color);
        }
    }

    public void selectText(String query, boolean erase,  int color) {
        for (ViewHolder holder : currentHolders) {
            selectText(holder, erase, query, color);
        }
    }

    public List<I> filter(Object query) {
        if (query == null) {
            return items;
        }
        List<I> founded = null;
        if (originalItems != null) {
            founded = find(query, true);
            changeTo(founded);
        }
        lastQuery = query;
        return founded;
    }

    public List<I> find(Object query, boolean original) {
        return find(query, original ? getOriginalItems() : getItems());
    }

    public List<I> find(Object query, List<I> items) {
        if (query == null) return items;
        final List<I> filteredList = new ArrayList<>();
        for (I item : items) {
            if (item instanceof Findable) {
                if (((Findable) item).find(query)) {
                    filteredList.add(item);
                }
            } else {
                final String text = item.toString().toLowerCase();
                if (text.contains(query.toString().toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        return filteredList;
    }

    public synchronized void changeTo(List<I> items) {
        applyAndAnimateRemovals(items);
        applyAndAnimateAdditions(items);
        applyAndAnimateMovedItems(items);
    }

    private void applyAndAnimateRemovals(List<I> newItems) {
        for (int i = this.items.size() - 1; i >= 0; i--) {
            final I item = this.items.get(i);
            if (!newItems.contains(item)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<I> newItems) {
        for (int i = 0; i < newItems.size(); i++) {
            final I item = newItems.get(i);
            if (!this.items.contains(item)) {
                if (this.items.size() > i)
                    addItem(i, item);
                else
                    addItem(item);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<I> newItems) {
        for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
            final I item = newItems.get(toPosition);
            final int fromPosition = this.items.indexOf(item);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                if (this.items.size() > toPosition)
                    moveItem(fromPosition, toPosition);
                else
                    moveItem(fromPosition, this.items.size() - 1);
            }
        }
    }

    @Override
    public void onClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null) {
            onClick(view, holder.getLayoutPosition());
        }
    }

    public void onClick(View view, int position){

    }

    // Implement OnLongClick listener.
    @Override
    public boolean onLongClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null) {
            onLongClick(view, holder.getLayoutPosition());
        }
        return false;
    }

    public void onLongClick(View view, int position) {

    }


    // Create the ViewHolder class to keep references to your views
    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        private static final String TAG = ViewHolder.class.getSimpleName();

        protected HashMap<Integer, View> views;
        protected View rootView;

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
            rootView = itemView;
            this.views = new HashMap<>(views.size());
            for (View view : views) {
                this.views.put(view.getId(), view);
            }
            onCreateHolder(itemView);
        }

        public abstract List<View> getViews(View itemView);

        public void onCreateHolder(View itemView) {
        }

        protected ViewHolder bindViews(ItemListAdapter adapter, boolean bindClicks) {
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                if(viewEntry != rootView) {
                    View view = viewEntry.getValue();
                    if(bindClicks) {
                        view.setOnClickListener(adapter);
                        view.setOnLongClickListener(adapter);
                    }
                    view.setTag(ViewHolder.this);
                }
            }
            return this;
        }

        public View getRootView() {
            return rootView;
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

        public <V extends View>  List<V> getViews(Class<V> viewClass) {
            List<V> textViews = new ArrayList<>();
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                View view = viewEntry.getValue();
                if (viewClass.isAssignableFrom(view.getClass())) {
                    textViews.add((V) view);
                }
            }
            return textViews;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ViewHolder)) return false;

            ViewHolder that = (ViewHolder) o;

            return !(views != null ? !views.equals(that.views) : that.views != null);

        }

        @Override
        public int hashCode() {
            return views != null ? views.hashCode() : 0;
        }
    }
}