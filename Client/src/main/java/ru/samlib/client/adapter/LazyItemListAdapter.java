package ru.samlib.client.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.List;

/**
 * Created by Dmitry on 21.04.2016.
 */
public abstract class LazyItemListAdapter<I> extends ItemListAdapter<I>{

    public LazyItemListAdapter(@LayoutRes int layoutId) {
        super(layoutId);
    }

    public LazyItemListAdapter(List<I> items, @LayoutRes int layoutId) {
        super(items, layoutId);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if(items != null && items.size() > translatePosition(position)) {
            onBindHolder(holder, items.get(translatePosition(position)));
        } else {
            onBindHolder(holder, null);
        }
    }

    public abstract void onBindHolder(ViewHolder holder, @Nullable I item);

    @Override
    public void onClick(View view) {
        boolean handled = false;
        if(view.getTag() instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder.getView(view.getId()) != null) {
                handled = onClick(view, items.get(translatePosition(holder.getLayoutPosition())));
            }
        } else {
            handled = onClick(view, null);
        }
        if(!handled && view.getParent() != null){
            ((View)view.getParent()).performClick();
        }
    }

    public boolean onClick(View view, @Nullable I item){
       return false;
    }

    @Override
    public boolean onLongClick(View view) {
        boolean handled = false;
        if (view.getTag() instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder.getView(view.getId()) != null) {
                handled = onLongClick(view, items.get(translatePosition(holder.getLayoutPosition())));
            }
        } else {
            handled = onLongClick(view, null);
        }
        if(!handled && view.getParent() != null) {
            ((View)view.getParent()).performLongClick();
        }
        return handled;
    }

    public boolean onLongClick(View view, @Nullable I item) {
       return false;
    }

    public int translatePosition(int position) {
        return position;
    }

}
