package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.PagerFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.FragmentPagerAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.IllustrationSelectedEvent;
import ru.samlib.client.domain.events.IllustrationsParsedEvent;
import ru.samlib.client.parser.IllustrationsParser;
import ru.kazantsev.template.util.FragmentBuilder;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by 0shad on 26.10.2015.
 */
public class IllustrationPagerFragment extends PagerFragment<Image, IllustrationFragment> {

    private static final String TAG = IllustrationPagerFragment.class.getSimpleName();

    private Work work;

    public static IllustrationPagerFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, IllustrationPagerFragment.class);
    }

    public static IllustrationPagerFragment show(BaseFragment fragment, String link) {
        return show(fragment, IllustrationPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    @Override
    public boolean allowBackPress() {
        if(getFragmentManager().getBackStackEntryCount() == 0) {
            AuthorFragment.show(new FragmentBuilder(getFragmentManager()), getId(), work.getAuthor());
            return false;
        } else {
            return super.allowBackPress();
        }
    }

    @Override
    public void onDataTaskException(Throwable ex) {
        if(ex instanceof IOException) {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        boolean newWork = false;
        String link = getArguments().getString(Constants.ArgsName.LINK);
        if (link != null && (work == null || !work.getLink().equals(link))) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                newWork = true;
            }
        }
        pagesSize = 999;
        autoLoadMore = false;
        if(newWork) {
            clearData();
            try {
                setDataSource(new IllustrationsParser(work));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
                ErrorFragment.show(IllustrationPagerFragment.this, R.string.error);
            }
        } else {
            postEvent(new IllustrationsParsedEvent(adapter.getItems()));
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onEvent(IllustrationSelectedEvent event) {
        if (event.index >= 0) {
            pager.setCurrentItem(event.index);
        }
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        postEvent(new IllustrationsParsedEvent(adapter.getItems()));
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        ((SectionActivity) getActivity()).setSelected(position);
    }


    @Override
    public FragmentPagerAdapter<Image, IllustrationFragment> newAdapter(List<Image> currentItems) {
        return new FragmentPagerAdapter<Image, IllustrationFragment>(getChildFragmentManager(), currentItems) {
            @Override
            public IllustrationFragment getNewItem(int position) {
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
