package ru.samlib.client.adapter;

import android.support.annotation.LayoutRes;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.samlib.client.util.GuiUtils;

import java.util.*;

/**
 * Created by 0shad on 08.03.2016.
 */
public abstract class ExpandableItemListAdapter<G,C> extends BaseExpandableListAdapter implements View.OnClickListener,
        View.OnLongClickListener {

    private final Map<Pair<Integer, G>, List<C>> items;
    protected Set<ViewHolder> currentHolders = Collections.newSetFromMap(new WeakHashMap<>());
    private final int group_view;
    private final int child_view;
    protected boolean bindViews = true;
    protected boolean bindClicks = true;

    public ExpandableItemListAdapter(@LayoutRes int group_view, @LayoutRes int child_view, Map<Pair<Integer, G>, List<C>> items) {
        this.group_view = group_view;
        this.child_view = child_view;
        this.items = items;
    }

    public ExpandableItemListAdapter(@LayoutRes int group_view, @LayoutRes int child_view) {
        this(group_view, child_view, new HashMap<>());
    }

    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return getGroup(groupPosition).size();
    }

    @Override
    public List<C> getGroup(int groupPosition) {
        for (Pair<Integer, G> pair : items.keySet()) {
            if(pair.first == groupPosition) {
                return items.get(pair);
            }
        }
        return null;
    }

    public G getGroupValue(int groupPosition) {
        for (Pair<Integer, G> pair : items.keySet()) {
            if (pair.first == groupPosition) {
                return pair.second;
            }
        }
        return null;
    }

    @Override
    public C getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;

    }

    public ExpandableItemListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, boolean isGroup) {
        View view;
        if (isGroup) {
            view = GuiUtils.inflate(parent, group_view);
        } else {
            view = GuiUtils.inflate(parent, child_view);
        }
        ExpandableItemListAdapter.ViewHolder holder = newHolder(view);
        if (bindViews) {
            holder.bindViews(ExpandableItemListAdapter.this, bindClicks);
        }
        holder.setGroup(isGroup);
        return holder;
    }

    protected ViewHolder newHolder(View view) {
        ViewHolder holder = new ViewHolder(view) {
            @Override
            public List<View> getViews(View childView) {
                return GuiUtils.getAllChildren(childView);
            }
        };
        currentHolders.add(holder);
        return holder;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = onCreateViewHolder(parent, true);
            convertView = holder.getItemView();
            // TODO: may be better solution?
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setGroupPosition(groupPosition);
        holder.setExpanded(isExpanded);
        onBindViewHolder(holder);
        return convertView;

    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = onCreateViewHolder(parent, false);
            convertView = holder.getItemView();
            // TODO: may be better solution?
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setLastChild(isLastChild);
        holder.setGroupPosition(groupPosition);
        holder.setChildPosition(childPosition);
        onBindViewHolder(holder);
        return convertView;
    }

    public abstract void onBindViewHolder(ViewHolder holder);
    
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void onClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null && holder.isGroup()) {
            if(holder.isGroup()) {
                ExpandableListView mExpandableListView = (ExpandableListView) holder.itemView.getParent();
                if(holder.isExpanded) {
                    mExpandableListView.collapseGroup(holder.getGroupPosition());
                } else {
                    mExpandableListView.expandGroup(holder.getGroupPosition());
                }
                setListViewHeight(mExpandableListView);
                onGroupClick(view, holder.getGroupPosition());
            } else {
                onChildClick(view, holder.getGroupPosition(), holder.getChildPosition());
            }
        }
    }

    public void onGroupClick(View view, int groupPosition) {

    }

    public void onChildClick(View view, int groupPosition, int childPosition) {

    }
    
    // Implement OnLongClick listener.
    @Override
    public boolean onLongClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder.getView(view.getId()) != null && holder.isGroup()) {
            if (holder.isGroup()) {
                onLongGroupClick(view, holder.getGroupPosition());
            } else {
                onLongChildClick(view, holder.getGroupPosition(), holder.getChildPosition());
            }
        }
        return false;
    }

    public void onLongGroupClick(View view, int groupPosition) {

    }


    public void onLongChildClick(View view, int groupPosition, int childPosition) {

    }

    public void setListViewHeight(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getLayoutParams().height;
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight
                + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }


    private void setListViewHeight(ExpandableListView listView) {
        ExpandableListAdapter listAdapter = listView.getExpandableListAdapter();
        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(),
                View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getGroupCount(); i++) {
            View groupItem = listAdapter.getGroupView(i, false, null, listView);
            groupItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += groupItem.getLayoutParams().height;

            if (listView.isGroupExpanded(i)) {
                for (int j = 0; j < listAdapter.getChildrenCount(i); j++) {
                    View listItem = listAdapter.getChildView(i, j, false, null,
                            listView);
                    listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                    totalHeight += listItem.getLayoutParams().height;
                }
            }
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        int height = totalHeight
                + (listView.getDividerHeight() * (listAdapter.getGroupCount() - 1));
        if (height < 10)
            height = 200;
        params.height = height;
        listView.setLayoutParams(params);
        listView.requestLayout();

    }

    public static abstract class ViewHolder  {

        protected final View itemView;
        protected HashMap<Integer, View> views;
        protected boolean isExpanded = false;
        protected boolean isLastChild = false;
        protected boolean isGroup = false;
        protected int groupPosition;
        protected int childPosition;

        /**
         * Constructor
         *
         * @param itemView The container view which holds the elements from the row item xml
         */
        public ViewHolder(View itemView) {
            this.itemView = itemView;
            cacheViews(itemView);
        }


        protected void setExpanded(boolean expanded) {
            isExpanded = expanded;
        }

        protected void setGroup(boolean group) {
            isGroup = group;
        }

        protected void setLastChild(boolean lastChild) {
            isLastChild = lastChild;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public boolean isLastChild() {
            return isLastChild;
        }

        public int getGroupPosition() {
            return groupPosition;
        }

        protected void setGroupPosition(int groupPosition) {
            this.groupPosition = groupPosition;
        }

        public int getChildPosition() {
            return childPosition;
        }

        protected void setChildPosition(int childPosition) {
            this.childPosition = childPosition;
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

        protected <C extends View.OnClickListener & View.OnLongClickListener> ViewHolder bindViews(C clickable, boolean bindClicks) {
            for (Map.Entry<Integer, View> viewEntry : views.entrySet()) {
                if (viewEntry != itemView) {
                    View view = viewEntry.getValue();
                    if (bindClicks) {
                        if (view instanceof AdapterView) {
                            AdapterView adapterView = (AdapterView) view;
                            // TODO: bind adapter
                        } else {
                            view.setOnClickListener(clickable);
                            view.setOnLongClickListener(clickable);
                        }
                    }
                    view.setTag(ViewHolder.this);
                }
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
            return itemView;
        }

        public <V extends View> List<V> getViews(Class<V> viewClass) {
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
