package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.PagerFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.FragmentPagerAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CommentPageEvent;
import ru.samlib.client.domain.events.CommentsParsedEvent;
import ru.samlib.client.domain.events.IllustrationsParsedEvent;
import ru.samlib.client.domain.events.SelectCommentPageEvent;
import ru.samlib.client.parser.CommentsParser;
import ru.kazantsev.template.util.FragmentBuilder;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentsPagerFragment extends PagerFragment<Integer, CommentsFragment> {

    private static final String TAG = CommentsPagerFragment.class.getSimpleName();

    private Work work;

    private CommentsParser parser;

    public static CommentsPagerFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, CommentsPagerFragment.class);
    }

    public static CommentsPagerFragment show(BaseFragment fragment, String link) {
        return show(fragment, CommentsPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static CommentsPagerFragment show(BaseFragment fragment, Work work) {
        return show(fragment, CommentsPagerFragment.class, Constants.ArgsName.WORK, work);
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
                    int lastPage = parser.requestLastPage();
                    ArrayList<Integer> indexes = new ArrayList();
                    for (int i = skip; i <= lastPage && i < size; i++) {
                        indexes.add(i);
                    }
                    return indexes;
                });
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
                ErrorFragment.show(CommentsPagerFragment.this, R.string.error);
            }
        } else {
            postEvent(new CommentsParsedEvent(adapter.getItems()));
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        postEvent(new CommentsParsedEvent(adapter.getItems()));
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
    public void onEvent(SelectCommentPageEvent event) {
        if (event.pageIndex >= 0) {
            pager.setCurrentItem(event.pageIndex);
        }
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        ((SectionActivity) getActivity()).setSelected(position);
    }

    @Override
    public FragmentPagerAdapter<Integer, CommentsFragment> newAdapter(List<Integer> currentItems) {
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
                return getString(R.string.comments_page) + ": " + String.valueOf(position);
            }
        };
    }

}
