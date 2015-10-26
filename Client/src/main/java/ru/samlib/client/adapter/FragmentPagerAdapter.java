package ru.samlib.client.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
    protected List<I> items = new ArrayList<>();

    public FragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public List<I> getItems(){
        return items;
    }

    public void addItems(Collection<I> newItems) {
        items.addAll(newItems);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }

    @Override
    public abstract Fragment getItem(int position);
}