package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ru.samlib.client.adapter.FragmentPagerAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.util.FragmentBuilder;

/**
 * Created by 0shad on 26.10.2015.
 */
public class IllustrationPagerFragment extends PagerFragment<Linkable, IllustrationFragment> {



    @Override
    public FragmentPagerAdapter<Linkable, IllustrationFragment> getAdapter() {
        return new FragmentPagerAdapter<Linkable, IllustrationFragment>(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return new FragmentBuilder(getFragmentManager())
                        .putArg(Constants.ArgsName.LINK, items.get(position).getFullLink())
                        .newFragment(IllustrationFragment.class);
            }
        };
    }



}
