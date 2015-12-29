package ru.samlib.client.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.*;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 12.07.2015.
 */
public class SectionActivity extends BaseActivity {

    private static final String TAG = SectionActivity.class.getSimpleName();

    enum SectionActivityState {
        INIT, WORK, AUTHOR, COMMENTS, ILLUSTRATIONS
    }

    private ViewGroup drawerHeader;
    private Author author;
    private Work work;
    private SectionActivityState state = SectionActivityState.INIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment sectionFragment = getLastFragment(savedInstanceState);
        if (sectionFragment != null) {
            FragmentBuilder builder = new FragmentBuilder(getSupportFragmentManager());
            if (sectionFragment instanceof AuthorFragment) {
                initializeAuthor(((AuthorFragment) sectionFragment).getAuthor());
            }
            if (sectionFragment instanceof WorkFragment) {
                WorkFragment workFragment = ((WorkFragment) sectionFragment);
                if (workFragment.getWork() != null && workFragment.getWork().getAuthor() != null) {
                    initializeAuthor(workFragment.getWork().getAuthor());
                } else {
                    Log.e(TAG, "Error ocurred unknovn author!! Work utl is: " + workFragment.getWork().getFullLink());
                }
            }
            builder.onOrientationChange();
            builder.replaceFragment(R.id.container, sectionFragment);
        }
    }

    @Override
    protected void handleIntent(Intent intent) {
        Bundle args = intent.getExtras();
        FragmentBuilder builder = new FragmentBuilder(getSupportFragmentManager());
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
        int id = R.id.container;
        if (current != null) {
            id = current.getId();
            builder.addToBackStack();
        }
        if (args != null) {
            author = (Author) args.getSerializable(Constants.ArgsName.AUTHOR);
            if (author != null) {
                AuthorFragment.show(builder, id, author);
                return;
            }
            work = (Work) args.getSerializable(Constants.ArgsName.WORK);
            if (work != null) {
                WorkFragment.show(builder, id, work);
                return;
            }
        }
        String link = null;
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            link = data.getPath();
            if (link != null) {
                if (Linkable.isAuthorLink(link)) {
                    AuthorFragment.show(builder, id, link);
                }
                if (Linkable.isWorkLink(link)) {
                    WorkFragment.show(builder, id, link);
                }
                if (Linkable.isIllustrationsLink(link)) {
                    IllustrationPagerFragment.show(builder, id, link);
                }
                if (Linkable.isCommentsLink(link)) {
                    CommentsFragment.show(builder, id, link);
                }
            }
        }
    }

    public SectionActivityState getState() {
        return state;
    }

    public void setState(SectionActivityState state) {
        this.state = state;
    }

    private void initializeAuthor(Author author) {
        this.author = author;
        setState(SectionActivityState.AUTHOR);
        actionBar.setTitle(author.getShortName());
        initNavigationView(R.layout.header_author_bar, author.getLinkableCategory().toArray());
        ImageView authorAvatar = GuiUtils.getView(drawerHeader, R.id.drawer_author_avatar);
        TextView drawerAuthorTitle = GuiUtils.getView(drawerHeader, R.id.drawer_author_title);
        TextView drawerAuthorAnnotation = GuiUtils.getView(drawerHeader, R.id.drawer_author_annotation);
        drawerAuthorTitle.setText(author.getFullName());
        drawerAuthorAnnotation.setText(author.getAnnotation());
        if (author.isHasAvatar()) {
            Picasso.with(this).load(author.getImageLink()).resize(GuiUtils.dpToPx(150, this), GuiUtils.dpToPx(150, this)).into(authorAvatar);
        }
    }

    private void initializeWork(Work work) {
        this.work = work;
        setState(SectionActivityState.WORK);
        initNavigationView(R.layout.header_work_bar, work.getAutoBookmarks().toArray());
        actionBar.setTitle(work.getAuthor().getShortName());
        TextView workTitle = GuiUtils.getView(drawerHeader, R.id.work_title);
        TextView workCreated = GuiUtils.getView(drawerHeader, R.id.work_created);
        TextView workUpdated = GuiUtils.getView(drawerHeader, R.id.work_updated);
        TextView workGenres = GuiUtils.getView(drawerHeader, R.id.work_genres);
        TextView workSeries = GuiUtils.getView(drawerHeader, R.id.work_series);
        GuiUtils.setText(workTitle, work.getTitle());
        if (work.getCreateDate() != null) {
            GuiUtils.setText(workCreated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getCreateDate()));
        }
        if (work.getUpdateDate() != null) {
            GuiUtils.setText(workUpdated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getUpdateDate()));
        }
        GuiUtils.setText(workGenres, work.printGenres());
        GuiUtils.setText(workSeries, work.getType().getTitle());
    }

    private View initNavigationView(@LayoutRes int header, Object... titles) {
        navigationView.removeHeaderView(drawerHeader);
        navigationView.getMenu().clear();

        for (int i = 0; i < titles.length; i++) {
            String title = titles[i].toString();
            if (title.length() > 22) {
                title = title.substring(0, 19) + "...";
            }
            navigationView.getMenu().add(Menu.NONE, Menu.NONE, i, title);
        }
        if (header > 0) {
            drawerHeader = (ViewGroup) getLayoutInflater().inflate(header, navigationView, false);
            navigationView.addHeaderView(drawerHeader);
            return null;
        }
        return drawerHeader;
    }


    private void initializeComments(int lastPage) {
        setState(SectionActivityState.COMMENTS);
        ArrayList<String> pages = new ArrayList<>();
        for (int i = 1; i < lastPage && lastPage > 0; i++) {
            pages.add(new String("Страница:" + i));
        }
        initNavigationView(R.layout.header_comments_bar, pages.toArray());
        final EditText number = GuiUtils.getView(drawerHeader, R.id.comments_comment_number);
        final Button scroll = GuiUtils.getView(drawerHeader, R.id.comments_scroll_to);
        scroll.setOnClickListener(v -> {
            postEvent(new ScrollToCommentEvent(TextUtils.extractInt(number.getText().toString()), -1));
            drawerLayout.closeDrawers();
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

    }

    private void initializeIllustrations(List<Image> images) {
        setState(SectionActivityState.ILLUSTRATIONS);
        initNavigationView(0, images.toArray());
    }


    @Override
    protected boolean onNavigationItemSelected(MenuItem item) {
        switch (state) {
            case INIT:
                break;
            case WORK:
                postEvent(new ChapterSelectedEvent(work.getAutoBookmarks().get(item.getOrder())));
                break;
            case AUTHOR:
                postEvent(new CategorySelectedEvent(author.getLinkableCategory().get(item.getOrder())));
                break;
            case COMMENTS:
                postEvent(new ScrollToCommentEvent(-1, item.getOrder()));
                break;
            case ILLUSTRATIONS:
                postEvent(new IllustrationSelectedEvent(item.getOrder()));
                break;
        }
        return false;
    }

    @Override
    protected void onDrawerClosed(View drawerView) {

    }

    @Override
    protected void onDrawerOpened(View drawerView) {

    }

    @Override
    public void onEvent(FragmentAttachedEvent fragmentAttached) {

    }

    public void onEventMainThread(AuthorParsedEvent event) {
        initializeAuthor(event.author);
    }

    public void onEventMainThread(WorkParsedEvent event) {
        initializeWork(event.work);
    }

    public void onEventMainThread(CommentsParsedEvent event) {
        initializeComments(event.lastPage);
    }

    public void onEventMainThread(IllustrationsParsedEvent event) {
        initializeIllustrations(event.images);
    }

}
