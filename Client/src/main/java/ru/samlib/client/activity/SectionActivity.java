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

    private ViewGroup drawerHeader;
    private Author author;
    private Work work;
    private boolean isWork = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment sectionFragment = getLastFragment(savedInstanceState);
        if (sectionFragment != null) {
            if (sectionFragment instanceof SectionFragment) {
                initializeAuthor(((SectionFragment) sectionFragment).getAuthor());
            }
            if (sectionFragment instanceof WorkFragment) {
                WorkFragment workFragment = ((WorkFragment) sectionFragment);
                if(workFragment.getWork().getAuthor() != null) {
                    initializeAuthor(workFragment.getWork().getAuthor());
                } else {
                    Log.e(TAG, "Error ocurred unknovn author!! Work utl is: " + workFragment.getWork().getFullLink());
                }
            }
            new FragmentBuilder(getSupportFragmentManager()).replaceFragment(R.id.container, sectionFragment);
        }
    }

    @Override
    protected void handleIntent(Intent intent) {
        Bundle args = intent.getExtras();
        if (args != null) {
            author = (Author) args.getSerializable(Constants.ArgsName.AUTHOR);
        }
        String authorLink = null;
        if (author != null) {
            authorLink = author.getLink();
        }
        if (validateIntent(intent)) {
            authorLink = intent.getData().getPath();
        }
        if (authorLink != null) {
            if (authorLink.endsWith(Work.HTML_SUFFIX)) {
                new FragmentBuilder(getSupportFragmentManager())
                        .putArg(Constants.ArgsName.LINK, authorLink)
                        .replaceFragment(R.id.container, WorkFragment.class);
            } else {
                new FragmentBuilder(getSupportFragmentManager())
                        .putArg(Constants.ArgsName.LINK, authorLink)
                        .replaceFragment(R.id.container, SectionFragment.class);
            }
        }
    }

    private void initializeAuthor(Author author) {
        this.author = author;
        isWork = false;
        navigationView.removeHeaderView(drawerHeader);
        navigationView.getMenu().clear();
        actionBar.setTitle(author.getShortName());
        drawerHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.author_bar_header, navigationView, false);
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
        isWork = true;
        navigationView.removeHeaderView(drawerHeader);
        navigationView.getMenu().clear();
        actionBar.setTitle(work.getAuthor().getShortName());
        drawerHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.work_bar_header, navigationView, false);
        TextView workTitle = GuiUtils.getView(drawerHeader, R.id.work_title);
        TextView workCreated = GuiUtils.getView(drawerHeader, R.id.work_created);
        TextView workUpdated = GuiUtils.getView(drawerHeader, R.id.work_updated);
        TextView workGenres = GuiUtils.getView(drawerHeader, R.id.work_genres);
        TextView workSeries = GuiUtils.getView(drawerHeader, R.id.work_series);
        GuiUtils.setText(workTitle, work.getTitle());
        if(work.getCreateDate() != null) {
            GuiUtils.setText(workCreated, new SimpleDateFormat("dd MM yyyy").format(work.getCreateDate()));
        }
        if(work.getUpdateDate() != null) {
            GuiUtils.setText(workUpdated, new SimpleDateFormat("dd MM yyyy").format(work.getUpdateDate()));
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

    private boolean validateIntent(Intent intent) {
        Uri data = getIntent().getData();
        return Intent.ACTION_VIEW.equals(intent.getAction())
                && (Linkable.isAuthorLink(data.getPath()) || Linkable.isWorkLink(data.getPath()));
    }

    @Override
    protected boolean onNavigationItemSelected(MenuItem item) {
        if (isWork) {
            postEvent(new ChapterSelectedEvent(work.getChapters().get(item.getOrder())));
        } else {
            postEvent(new CategorySelectedEvent(author.getLinkableCategory().get(item.getOrder())));
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
