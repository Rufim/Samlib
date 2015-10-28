package ru.samlib.client.adapter;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.fragments.BaseFragment;
import ru.samlib.client.util.FragmentBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Dmitry on 26.10.2015.
 */
public abstract class FragmentPagerAdapter<I,F extends BaseFragment> extends FragmentStatePagerAdapter {

    protected SparseArray<F> registeredFragments = new SparseArray<>();
    protected List<I> items = new ArrayList<>();

    public FragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public FragmentPagerAdapter(FragmentManager fm, List<I> items) {
        super(fm);
        this.items = items;
    }

    public List<I> getItems(){
        return items;
    }

    public void addItems(Collection<I> newItems) {
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItem(I item) {
        items.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        F fragment = (F) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
        //do nothing here! no call to super.restoreState(arg0, arg1);
    }

    public F getRegisteredFragment(int position) {
        if(position < 0 || position >= registeredFragments.size()) {
            return null;
        }
        return registeredFragments.get(position);
    }
    @Override
    public abstract Fragment getItem(int position);
}