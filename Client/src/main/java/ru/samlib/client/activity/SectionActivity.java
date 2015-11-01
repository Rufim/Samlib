package ru.samlib.client.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Category;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.*;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;

import java.text.SimpleDateFormat;
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
    private boolean onOrientationChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment sectionFragment = getLastFragment(savedInstanceState);
        if (sectionFragment != null) {
            onOrientationChange = true;
            FragmentBuilder builder = new FragmentBuilder(getSupportFragmentManager());
            if (sectionFragment instanceof AuthorFragment) {
                initializeAuthor(((AuthorFragment) sectionFragment).getAuthor());
            }
            if (sectionFragment instanceof WorkFragment) {
                WorkFragment workFragment = ((WorkFragment) sectionFragment);
                if(workFragment.getWork() != null && workFragment.getWork().getAuthor() != null) {
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
        if(current != null) {
            id = current.getId();
            builder.addToBackStack();
        }
        if(!onOrientationChange) {
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
            Uri data = getIntent().getData();
           if(Intent.ACTION_VIEW.equals(intent.getAction())) {
               link = data.getPath();
                if (link != null) {
                    if(Linkable.isAuthorLink(link)) {
                        AuthorFragment.show(builder, id, link);
                    }
                    if(Linkable.isWorkLink(link)) {
                        WorkFragment.show(builder, id, link);
                    }
                    if(Linkable.isIllustrationsLink(link)) {
                        IllustrationPagerFragment.show(builder, id, link);
                    }
                    if(Linkable.isCommentsLink(link)) {
                        CommentsFragment.show(builder, id, link);
                    }
                }
            }
        }
        onOrientationChange = false;
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
        navigationView.removeHeaderView(drawerHeader);
        navigationView.getMenu().clear();
        actionBar.setTitle(author.getShortName());
        drawerHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.header_author_bar, navigationView, false);
        ImageView authorAvatar = GuiUtils.getView(drawerHeader, R.id.drawer_author_avatar);
        TextView drawerAuthorTitle = GuiUtils.getView(drawerHeader, R.id.drawer_author_title);
        TextView drawerAuthorAnnotation = GuiUtils.getView(drawerHeader, R.id.drawer_author_annotation);
        drawerAuthorTitle.setText(author.getFullName());
        drawerAuthorAnnotation.setText(author.getAnnotation());
        if (author.isHasAvatar()) {
            Picasso.with(this).load(author.getImageLink()).resize(GuiUtils.dpToPx(150, this), GuiUtils.dpToPx(150, this)).into(authorAvatar);
        }
        List<Category> categories = author.getLinkableCategory();
        for (int i = 0; i < categories.size(); i++) {
            String title = categories.get(i).getTitle();
            if (title.length() > 22) {
                title = title.substring(0, 19) + "...";
            }
            navigationView.getMenu().add(Menu.NONE, Menu.NONE, i, title);
        }
        navigationView.addHeaderView(drawerHeader);
    }

    private void initializeWork(Work work) {
        this.work = work;
        setState(SectionActivityState.WORK);
        navigationView.removeHeaderView(drawerHeader);
        navigationView.getMenu().clear();
        actionBar.setTitle(work.getAuthor().getShortName());
        drawerHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.header_work_bar, navigationView, false);
        TextView workTitle = GuiUtils.getView(drawerHeader, R.id.work_title);
        TextView workCreated = GuiUtils.getView(drawerHeader, R.id.work_created);
        TextView workUpdated = GuiUtils.getView(drawerHeader, R.id.work_updated);
        TextView workGenres = GuiUtils.getView(drawerHeader, R.id.work_genres);
        TextView workSeries = GuiUtils.getView(drawerHeader, R.id.work_series);
        GuiUtils.setText(workTitle, work.getTitle());
        if(work.getCreateDate() != null) {
            GuiUtils.setText(workCreated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getCreateDate()));
        }
        if(work.getUpdateDate() != null) {
            GuiUtils.setText(workUpdated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getUpdateDate()));
        }
        GuiUtils.setText(workGenres, work.printGenres());
        GuiUtils.setText(workSeries, work.getType().getTitle());
        for (int i = 0; i < work.getChapters().size(); i++) {
            String title = work.getChapters().get(i).getTitle();
            if (title.length() > 22) {
                title = title.substring(0, 19) + "...";
            }
            navigationView.getMenu().add(Menu.NONE, Menu.NONE, i, title);
        }
        navigationView.addHeaderView(drawerHeader);
    }


    @Override
    protected boolean onNavigationItemSelected(MenuItem item) {
        switch (state) {
            case INIT:
                break;
            case WORK:
                postEvent(new ChapterSelectedEvent(work.getChapters().get(item.getOrder())));
                break;
            case AUTHOR:
                postEvent(new CategorySelectedEvent(author.getLinkableCategory().get(item.getOrder())));
                break;
            case COMMENTS:
                break;
            case ILLUSTRATIONS:
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
}
