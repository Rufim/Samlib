package ru.samlib.client.adapter;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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
    protected Set<ViewHolder> currentHolders = new HashSet<>();
    protected final int layoutId;
    protected String lastQuery;

    // Adapter's Constructor
    public ItemListAdapter() {
        this.layoutId = -1;
    }

    // Adapter's Constructor
    public ItemListAdapter(int layoutId) {
        this.layoutId = layoutId;
    }

    // Adapter's Constructor
    public ItemListAdapter(List<I> items, int layoutId) {
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
        holder.bindViews(ItemListAdapter.this);
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

    public String getLastQuery() {
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

    public void selectText(ViewHolder holder, String query, int color) {
        if (query == null) {
            query = "";
        }
        query = query.toLowerCase();
        for (TextView textView : holder.getAllTextViews()) {
            Spannable raw = new SpannableString(textView.getText());
            BackgroundColorSpan[] spans = raw.getSpans(0,
                    raw.length(),
                    BackgroundColorSpan.class);

            if (spans.length > 0) {
                for (BackgroundColorSpan span : spans) {
                    raw.removeSpan(span);
                }
                if (query.isEmpty()) {
                    textView.setText(raw);
                    continue;
                }
            }

            if (query.isEmpty()) {
                continue;
            }

            int index = TextUtils.indexOf(raw.toString().toLowerCase(), query);

            while (index >= 0) {
                raw.setSpan(new BackgroundColorSpan(color), index, index
                        + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = TextUtils.indexOf(raw.toString().toLowerCase(), query, index + query.length());
            }

            textView.setText(raw);
        }
    }

    public void selectText(String query, int color) {
        for (ViewHolder holder : currentHolders) {
            selectText(holder, query, color);
        }
    }

    public List<I> filter(String query) {
        if (query == null) {
            return items;
        }
        query = query.toLowerCase();
        List<I> founded = null;
        if (originalItems != null) {
            founded = find(query, true);
            changeTo(founded);
        }
        lastQuery = query;
        return founded;
    }

    public List<I> find(String query, boolean original) {
        return find(query, original ? getOriginalItems() : getItems());
    }

    public List<I> find(String query, List<I> items) {
        if (query == null) return items;
        final List<I> filteredList = new ArrayList<>();
        for (I item : items) {
            if (item instanceof Findable) {
                if (((Findable) item).find(query)) {
                    filteredList.add(item);
                }
            } else {
                final String text = item.toString().toLowerCase();
                if (text.contains(query)) {
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

    public void onLongClick(View view, int position) {
    }


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

        public void onCreateHolder(View itemView) {
        }

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

        public List<TextView> getAllTextViews() {
            List<TextView> textViews = new ArrayList<>();
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                View view = viewEntry.getValue();
                if (view instanceof TextView) {
                    textViews.add((TextView) view);
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