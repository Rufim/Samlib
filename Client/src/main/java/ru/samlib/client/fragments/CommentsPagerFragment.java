package ru.samlib.client.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.greenrobot.event.EventBus;
import ru.samlib.client.R;
import ru.samlib.client.adapter.FragmentPagerAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.*;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.CommentsParser;
import ru.samlib.client.parser.IllustrationsParser;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentsPagerFragment extends PagerFragment<Integer, CommentsFragment>  {

    private static final String TAG = CommentsPagerFragment.class.getSimpleName();

    private Work work;

    private CommentsParser parser;

    public static void show(FragmentBuilder builder, @IdRes int container, String link) {
        show(builder, container, IllustrationPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, IllustrationPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Work work) {
        show(fragment, IllustrationPagerFragment.class, Constants.ArgsName.WORK, work);
    }

    @Override
    public boolean allowBackPress() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            AuthorFragment.show(new FragmentBuilder(getFragmentManager()), getId(), work.getAuthor());
            return false;
        } else {
            return super.allowBackPress();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        boolean newWork = false;
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (incomingWork != null && !incomingWork.equals(work)) {
            work = incomingWork;
            newWork = true;
        } else if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                newWork = true;
            }
        }
        pagesSize = 999;
        if (newWork) {
            clearData();
            try {
                parser = new CommentsParser(work, false);
                setDataSource((skip, size) -> {
                    int lastPage = parser.getLastPage();
                    if(lastPage < 0) {
                        parser.getPage(0);
                        lastPage = parser.getLastPage();
                    }
                    ArrayList<Integer> indexes = new ArrayList();
                    for (int i = skip; i < lastPage && i < size; i++) {
                        indexes.add(i);
                    }
                    return indexes;
                });
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
                ErrorFragment.show(CommentsPagerFragment.this, R.string.error);
            }
        } else {
            postEvent(new CommentsParsedEvent(parser.getLastPage()));
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

    public void onEvent(CommentPageEvent event) {
        if (event.pageIndex > 0) {
            pager.setCurrentItem(event.pageIndex);
        }
    }

    @Override
    public FragmentPagerAdapter<Integer, CommentsFragment> getAdapter(List<Integer> currentItems) {
        return new FragmentPagerAdapter<Integer, CommentsFragment>(getChildFragmentManager(), currentItems) {
            @Override
            public Fragment getItem(int position) {
                return new FragmentBuilder(null)
                        .putArg(Constants.ArgsName.COMMENTS_PAGE, position)
                        .putArg(Constants.ArgsName.WORK, work)
                        .newFragment(CommentsFragment.class);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return "Страница:" + String.valueOf(position);
            }
        };
    }

}
