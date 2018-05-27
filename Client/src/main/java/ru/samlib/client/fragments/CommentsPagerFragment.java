package ru.samlib.client.fragments;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import org.acra.ACRA;
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
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.FragmentPagerAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.dialog.EditListPreferenceDialog;
import ru.samlib.client.dialog.NewCommentDialog;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CommentSendEvent;
import ru.samlib.client.domain.events.CommentsParsedEvent;
import ru.samlib.client.domain.events.SelectCommentPageEvent;
import ru.samlib.client.parser.CommentsParser;
import ru.kazantsev.template.util.FragmentBuilder;
import ru.samlib.client.parser.Parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static CommentsPagerFragment show(BaseFragment fragment, String link) {
        return show(fragment, CommentsPagerFragment.class, Constants.ArgsName.LINK, link);
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
                NewCommentDialog dialog = (NewCommentDialog) getFragmentManager().findFragmentByTag(NewCommentDialog.class.getSimpleName());
                if (dialog == null) {
                    dialog = new NewCommentDialog();
                    dialog.show(getFragmentManager(), NewCommentDialog.class.getSimpleName());
                }
                return true;
            case R.id.action_comments_choose_archive:
                Map<String, Integer> TitleVal = new LinkedHashMap<>();
                String current = getString(R.string.comments_current);
                TitleVal.put(current, 0);
                for (int i = 1; i <= parser.getArchiveCount(); i++) {
                    TitleVal.put(i + "", i);
                }
                EditListPreferenceDialog editListPreferenceDialog = new EditListPreferenceDialog();
                SettingsFragment.Preference preference = new SettingsFragment.Preference(getContext(), -1, parser.getCurrentArchive());
                preference.title = getString(R.string.comments_choose_archive);
                preference.keyValue = TitleVal;
                editListPreferenceDialog.setPreference(preference);
                editListPreferenceDialog.setOnCommit((value, d) -> {
                    parser.setArchive((Integer) value);
                    safeInvalidateOptionsMenu();
                    refreshData(true);
                    return true;
                });
                editListPreferenceDialog.show(getFragmentManager(), editListPreferenceDialog.getClass().getSimpleName());
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
        ((SectionActivity) getActivity()).setSelected(0);
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
    public void onDataTaskException(Throwable ex) {
        if(ex instanceof IOException) {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
        }
    }


    @Subscribe
    public void onEvent(final CommentSendEvent event) {
        loadMoreBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Object[]>() {

            @Override
            protected Object[] doInBackground(Void... params) {
                Object[] answer = new Object[3];
                boolean saveNewCookie = !Parser.hasCoockieComment();
                Response response = CommentsParser.sendComment(work, event.name, event.email, event.link, event.comment, event.operation, event.msgid);
                try {
                    if (response == null || response.getCode() != 200) {
                        answer[0] = "ERROR";
                        answer[1] = getString(R.string.error);
                    } else {
                        Document resp = Jsoup.parse(response.getRawContent(response.getEncoding()));
                        Elements error = resp.select("table[BORDERCOLOR=#222222] table table b");
                        if (error.size() == 0) {
                            answer[0] = "OK";
                            answer[1] = saveNewCookie ? Parser.getCommentCookie() : null;
                            answer[2] = resp;
                        } else {
                            answer[0] = "ERROR";
                            answer[1] = error.text();
                        }
                    }
                } catch (IOException e) {
                    answer[0] = "ERROR";
                    answer[1] = getString(R.string.error);
                }
                return answer;
            }

            @Override
            protected void onPostExecute(Object[] answer) {
                if ("OK".equals(answer[0])) {
                    if(answer[1] != null) {
                        SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                        editor.putString(getString(R.string.preferenceCommentCoockie), answer[1].toString());
                        editor.apply();
                    }
                    if (adapter.getCount() > 0) {
                        adapter.getRegisteredFragment(event.indexPage).refreshWithDocument((Document) answer[2]);
                    }
                } else if("ERROR".equals(answer[0])) {
                    GuiUtils.toast(getContext(), answer[1].toString(), false);
                }
                loadMoreBar.setVisibility(View.GONE);
            }

        }.execute();
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
            public CommentsFragment getNewItem(int position) {
                return new FragmentBuilder(null)
                        .putArg(Constants.ArgsName.COMMENTS_PAGE, position)
                        .putArg(Constants.ArgsName.COMMENTS_ARCHIVE, parser.getCurrentArchive())
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
