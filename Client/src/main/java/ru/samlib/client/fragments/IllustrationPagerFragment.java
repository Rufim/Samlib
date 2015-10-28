package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.FragmentPagerAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.IllustrationsParser;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.TextUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 26.10.2015.
 */
public class IllustrationPagerFragment extends PagerFragment<Image, IllustrationFragment> {

    private static final String TAG = IllustrationPagerFragment.class.getSimpleName();

    private Work work;

    public static void show(FragmentManager manager, @IdRes int container, String link) {
        show(manager, container, IllustrationPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, IllustrationPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Work work) {
        show(fragment, IllustrationPagerFragment.class, Constants.ArgsName.WORK, work);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (incomingWork != null && !incomingWork.equals(work)) {
            work = incomingWork;
        }
        if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                clearData();
            }
        }
        try {
            setDataSource(new IllustrationsParser(work));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Unknown exception", e);
            ErrorFragment.show(IllustrationPagerFragment.this, R.string.error);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public FragmentPagerAdapter<Image, IllustrationFragment> getAdapter(List<Image> currentItems) {
        return new FragmentPagerAdapter<Image, IllustrationFragment>(getChildFragmentManager(), currentItems) {
            @Override
            public Fragment getItem(int position) {
                return new FragmentBuilder(null)
                        .putArg(Constants.ArgsName.IMAGE, items.get(position))
                        .newFragment(IllustrationFragment.class);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                String title = items.get(position).getTitle();
                if (!TextUtils.isEmpty(title)) return title;
                return String.valueOf(position);
            }
        };
    }

}
