package ru.samlib.client.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.annimon.stream.Stream;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;

/**
 * Created by 0shad on 12.07.2015.
 */
public class AuthorActivity extends BaseActivity implements SectionFragment.AuthorListener {

    ImageView authorAvatar;
    TextView drawerAuthorTitle;
    TextView drawerAuthorAnnotation;
    RelativeLayout drawerHeader;
    private Author author;

    private AsyncTaskFragment task;

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
        if (checkIntent(intent)) {
            authorLink = intent.getData().getPath();
        }
        if (authorLink.contains(".shtml")) {
            return;
        }
        if (authorLink != null) {
            new FragmentBuilder(getSupportFragmentManager())
                    .putArg(Constants.ArgsName.LINK, authorLink)
                    .replaceFragment(R.id.container, SectionFragment.class)
                    .addListener(this);
        }
    }

    private void initializeAuthor(Author author) {
        this.author = author;
        actionBar.setTitle(author.getShortName());
        drawerHeader = (RelativeLayout) getLayoutInflater().inflate(R.layout.author_header, navigationView, false);
        authorAvatar = GuiUtils.getView(drawerHeader, R.id.drawer_author_avatar);
        drawerAuthorTitle = GuiUtils.getView(drawerHeader, R.id.drawer_author_title);
        drawerAuthorAnnotation = GuiUtils.getView(drawerHeader, R.id.drawer_author_annotation);
        drawerAuthorTitle.setText(author.getFullName());
        drawerAuthorAnnotation.setText(author.getAnnotation());
        if (author.isHasAvatar()) {
            Picasso.with(this).load(author.getImageLink()).resize(GuiUtils.dpToPx(150, this), GuiUtils.dpToPx(150, this)).into(authorAvatar);
        }
        Stream.of(author.getLinkableSections()).forEach(sec -> navigationView.getMenu().add(sec.getTitle()));
        navigationView.addHeaderView(drawerHeader);
    }

    private boolean checkIntent(Intent intent) {
        Uri data = getIntent().getData();
        return Intent.ACTION_VIEW.equals(intent.getAction())
                && Constants.Net.BASE_SCHEME.equals(data.getScheme())
                && Constants.Net.BASE_HOST.equals(data.getHost());
    }

    @Override
    protected boolean onNavigationItemSelected(MenuItem item) {

        return false;
    }

    @Override
    protected void onDrawerClosed(View drawerView) {

    }

    @Override
    protected void onDrawerOpened(View drawerView) {

    }

    @Override
    public void onFragmentAttached(BaseFragment fragment) {

    }

    public void onAuthorParsed(Author author) {
        initializeAuthor(author);
    }

}
