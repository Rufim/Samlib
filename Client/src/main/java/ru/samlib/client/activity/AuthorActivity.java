package ru.samlib.client.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.annimon.stream.Stream;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.domain.events.FragmentAttachedEvent;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;

import java.text.SimpleDateFormat;

/**
 * Created by 0shad on 12.07.2015.
 */
public class AuthorActivity extends BaseActivity {

    private ViewGroup drawerHeader;
    private Author author;
    private Work work;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment sectionFragment = getLastFragment(savedInstanceState);
        if (sectionFragment != null) {
            if (sectionFragment instanceof SectionFragment) {
                initializeAuthor(((SectionFragment) sectionFragment).getAuthor());
            }
            if (sectionFragment instanceof WorkFragment) {
                initializeAuthor(((WorkFragment) sectionFragment).getWork().getAuthor());
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
            if (authorLink.endsWith(".shtml")) {
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
        Stream.of(author.getLinkableCategory()).forEach(sec -> navigationView.getMenu().add(sec.getTitle()));
        navigationView.addHeaderView(drawerHeader);
    }

    private void initializeWork(Work work) {
        this.work = work;
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
        GuiUtils.setText(workCreated, new SimpleDateFormat("dd MM yyyy").format(work.getCreateDate()));
        GuiUtils.setText(workUpdated, new SimpleDateFormat("dd MM yyyy").format(work.getUpdateDate()));
        GuiUtils.setText(workGenres, work.printGenres());
        GuiUtils.setText(workSeries, work.getType().getTitle());
        navigationView.addHeaderView(drawerHeader);
    }

    private boolean validateIntent(Intent intent) {
        Uri data = getIntent().getData();
        return Intent.ACTION_VIEW.equals(intent.getAction())
                && data.getPath().matches("/*[a-z]/+[a-z_]+((/*)|(/+[a-z-_0-9]+\\.shtml))?");
    }

    @Override
    protected boolean onNavigationItemSelected(MenuItem item) {
        postEvent(new CategorySelectedEvent(Stream
                .of(author.getLinkableCategory())
                .filter(cat -> cat.getTitle().equals(item.getTitle()))
                .findFirst()
                .get()));
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
