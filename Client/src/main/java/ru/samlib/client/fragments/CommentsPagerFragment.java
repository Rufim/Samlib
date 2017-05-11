package ru.samlib.client.fragments;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.PagerFragment;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.FragmentPagerAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.dialog.DialogNewComment;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CommentSuccessEvent;
import ru.samlib.client.domain.events.CommentsParsedEvent;
import ru.samlib.client.domain.events.SelectCommentPageEvent;
import ru.samlib.client.parser.CommentsParser;
import ru.kazantsev.template.util.FragmentBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentsPagerFragment extends PagerFragment<Integer, CommentsFragment> {

    private static final String TAG = CommentsPagerFragment.class.getSimpleName();

    private Work work;

    CommentsParser parser;

    public static CommentsPagerFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, CommentsPagerFragment.class);
    }

    public static CommentsPagerFragment show(FragmentBuilder builder, @IdRes int container, Work work) {
        return show(builder.putArg(Constants.ArgsName.WORK, work), container, CommentsPagerFragment.class);
    }


    public static CommentsPagerFragment show(BaseFragment fragment, String link) {
        return show(fragment, CommentsPagerFragment.class, Constants.ArgsName.LINK, link);
    }

    public static CommentsPagerFragment show(BaseFragment fragment, Work work) {
        return show(fragment, CommentsPagerFragment.class, Constants.ArgsName.WORK, work);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.comments, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_comments_add_new:
                DialogNewComment dialog = (DialogNewComment) getFragmentManager().findFragmentByTag(DialogNewComment.class.getSimpleName());
                if (dialog == null) {
                    dialog = new DialogNewComment();
                    dialog.show(getFragmentManager(), DialogNewComment.class.getSimpleName());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    public void refreshData(boolean showProgress) {
        try {
            parser = new CommentsParser(work, false);
        } catch (MalformedURLException e) {
        }
        super.refreshData(showProgress);
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
                    int count = parser.requestPageCount();
                    ArrayList<Integer> indexes = new ArrayList();
                    for (int i = skip; i < count && i < size; i++) {
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


    @Subscribe
    public void onEvent(CommentSuccessEvent event) {
        loadMoreBar.setVisibility(View.VISIBLE);
        new AsyncTask<String, Void, String[]>() {

            @Override
            protected String[] doInBackground(String... params) {
                String[] answer = new String[2];
                if (!TextUtils.isEmpty(params[0])) {
                    Response response = CommentsParser.sendNewComment(work, params[0], params[1], params[2], params[3]);
                    try {
                        if (response == null || response.getCode() != 200) {
                            answer[0] = "ERROR";
                            answer[1] = getString(R.string.error);
                        } else {
                            Document resp =  Jsoup.parse(response.getRawContent(response.getEncoding()));
                            Elements error = resp.select("table[BORDERCOLOR=#222222] table table b");
                            if (error.size() == 0) {
                                if(parser != null) {
                                    parser.setDocument(resp);
                                }
                                answer[0] = "OK";
                                answer[1] = params[0];
                            } else {
                                answer[0] = "ERROR";
                                answer[1] = error.text();
                            }
                        }
                    } catch (IOException e) {
                        answer[0] = "ERROR";
                        answer[1] = getString(R.string.error);
                    }
                } else {
                    answer[0] = "ERROR";
                    answer[1] = getString(R.string.error_network);
                }
                return answer;
            }

            @Override
            protected void onPostExecute(String[] answer) {
                if ("OK".equals(answer[0])) {
                    SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                    editor.putString(getString(R.string.preferenceCommentCoockie), answer[1]);
                    editor.apply();
                    refreshData(true);
                } else {
                    GuiUtils.toast(getContext(), answer[1]);
                    loadMoreBar.setVisibility(View.GONE);
                }
            }

        }.execute(event.name, event.email, event.link, event.comment);
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
