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
        currentHolders.add(holder);
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

    public int getAbsoluteItemCount() {
        if(originalItems == null) {
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

    public void enterFilteringMode(){
        if(originalItems == null) {
            this.originalItems = new ArrayList<>(items);
        }
    }

    public void exitFilteringMode() {
        if(originalItems != null) {
            this.items = this.originalItems;
            this.originalItems = null;
            notifyDataSetChanged();
        }
    }

    public void addItems(List<I> items) {
        if(originalItems == null) {
            this.items.addAll(items);
            notifyDataSetChanged();
        } else {
            this.originalItems.addAll(items);
            filter(lastQuery);
        }
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

    public void selectText(String query, int color){
        query = query.toLowerCase();
        if(!query.isEmpty()) {
            for (ViewHolder holder : currentHolders) {
                for (TextView textView : holder.getAllTextViews()) {
                    Spannable raw = new SpannableString(textView.getText());
                    BackgroundColorSpan[] spans = raw.getSpans(0,
                            raw.length(),
                            BackgroundColorSpan.class);

                    for (BackgroundColorSpan span : spans) {
                        raw.removeSpan(span);
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
        }
    }

    public List<Integer> search(String query, boolean original) {
        List<Integer> indexes = new ArrayList<>();
        List<I> items = original ? getOriginalItems() : getItems();
        for (int i = 0; i < items.size(); i++) {
            I item = items.get(i);
            if (item instanceof Findable) {
                if (((Findable) item).find(query)) {
                    indexes.add(i);
                }
            } else {
                final String text = item.toString().toLowerCase();
                if (text.contains(query)) {
                    indexes.add(i);
                }
            }
        }
        return indexes;
    }

    public int filter(String query) {
        query = query.toLowerCase();
        if(originalItems != null) {
            changeTo(find(query, true));
        }
        lastQuery = query;
        return 0;
    }

    public List<I> find(String query, boolean original) {
        final List<I> filteredList = new ArrayList<>();
        for (I item : original ? getOriginalItems() : getItems()) {
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

    public void changeTo(List<I> items) {
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
                addItem(i, item);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<I> newItems) {
        for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
            final I item = newItems.get(toPosition);
            final int fromPosition = this.items.indexOf(item);
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

        public List<TextView> getAllTextViews() {
            List<TextView> textViews = new ArrayList<>();
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                View view = viewEntry.getValue();
                if(view instanceof TextView) {
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