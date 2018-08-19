package ru.samlib.client.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.annimon.stream.Stream;
import com.squareup.picasso.Picasso;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ru.kazantsev.template.activity.NavigationActivity;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.*;
import ru.samlib.client.fragments.AuthorFragment;
import ru.samlib.client.fragments.CommentsPagerFragment;
import ru.samlib.client.fragments.IllustrationPagerFragment;
import ru.samlib.client.fragments.WorkFragment;
import ru.kazantsev.template.util.FragmentBuilder;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.parser.Parser;
import ru.samlib.client.util.SamlibUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by 0shad on 12.07.2015.
 */
public class SectionActivity extends NavigationActivity<String> {

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
        setTheme(AndroidSystemUtils.getStringResPreference(this, R.string.preferenceCurrentTheme, getApplicationInfo().theme));
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
                    Log.e(TAG, "Error ocurred unknown author!! Work url is: " + workFragment.getWork());
                }
            }
            builder.replaceFragment(R.id.container, sectionFragment);
        }
        navigationListMenu.setPadding(0, (int) getResources().getDimension(R.dimen.spacing_medium), 0, (int) getResources().getDimension(R.dimen.spacing_medium));
        if(!Parser.hasCoockieComment()) {
            Parser.setCommentCookie(AndroidSystemUtils.getDefaultPreference(this).getString(getString(R.string.preferenceCommentCoockie), null));
        }
        if(!Parser.hasCoockieVote()) {
            Parser.setVoteCookie(AndroidSystemUtils.getDefaultPreference(this).getString(getString(R.string.preferenceVoteCoockie), null));
        }
    }

    @Override
    protected int getNavigationViewId() {
        return R.layout.item_section_navigation;
    }


    @Override
    protected void onBindNavigationView(int position, String title, View navigationView) {
        TextView textView  = GuiUtils.getView(navigationView, R.id.item_section_navigation_text);
        if(state.equals(SectionActivityState.ILLUSTRATIONS) || state.equals(SectionActivityState.COMMENTS)) {
            textView.setGravity(Gravity.CENTER);    
        } else {
            textView.setGravity(Gravity.START);
        }
        if(author != null && state.equals(SectionActivityState.AUTHOR) && getCurrentFragment() instanceof AuthorFragment && ((AuthorFragment)getCurrentFragment()).isSimpleView()) {
            Category category = Stream.of(author.getLinkableCategory()).filter(c -> title.equals(c.toString())).findFirst().orElse(null);
            if(category.isHasUpdates()) {
                GuiUtils.setText(textView, SamlibUtils.generateText(title, getResString(R.string.favorites_update), GuiUtils.getThemeColor(this, R.attr.colorAccent), 0.8f));
                return;
            }
        }
        GuiUtils.setText(textView, title);
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
        String link;
        Uri data;
        if ((data = intent.getData()) != null && (link = data.getPath()) != null) {
            File file = new File(link);
            if (Linkable.isAuthorLink(link)) {
                AuthorFragment.show(builder, id, link);
            } else if (Linkable.isWorkLink(link)) {
                if(!isRestore(current, intent, false)) {
                    WorkFragment.show(builder, id, link);
                }
            } else if (Linkable.isIllustrationsLink(link)) {
                IllustrationPagerFragment.show(builder, id, link);
            } else if (Linkable.isCommentsLink(link)) {
                CommentsPagerFragment.show(builder, id, link);
            } else if((data.getScheme() != null && data.getScheme().startsWith("file")) || (file.exists() && file.isFile())) {
                if(!isRestore(current, intent, true)) {
                    WorkFragment.showFile(builder, id, link);
                }
            } else if(data.getScheme() != null && data.getScheme().startsWith("content")) {
                if (!isRestore(current, intent, true)) {
                    WorkFragment.showContent(builder, id, intent.getData(), intent.getType());
                }
            } else {
                BaseFragment.show(builder, id, getResString(R.string.page_not_supported));
            }
        }
    }

    public boolean isRestore(Fragment current, Intent intent, boolean external) {
        if(current != null && current instanceof WorkFragment && intent.getBooleanExtra(Constants.ArgsName.WORK_RESTORE, false)) {
            Uri data = intent.getData();
            if(external) {
                ExternalWork externalWork = ((WorkFragment) current).getExternalWork();
                if(intent.getScheme() != null && intent.getScheme().equals("content")) {
                    return externalWork != null && externalWork.getContentUri() != null && externalWork.getContentUri().equals(data);
                }  else {
                    return externalWork != null && externalWork.getFilePath().equals(data.getPath());
                }
            } else {
                Work work =  ((WorkFragment) current).getWork();
                return work != null && work.getLink() != null && work.getLink().equals(data.getPath());
            }
        } else {
            return false;
        }
    }

    public SectionActivityState getState() {
        return state;
    }

    public void setState(SectionActivityState state) {
        this.state = state;
    }

    private void initializeAuthor(Author author) {
        if(author != null) {
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
            setNavigationLayoutWidth(GuiUtils.dpToPx(navigationFixedDpWidth, this));
        }
    }

    private void initializeWork(Work work) {
        this.work = work;
        if(work != null) {
            setState(SectionActivityState.WORK);
            initNavigationView(R.layout.header_work_bar, work.getAutoBookmarks().toArray());
            actionBar.setTitle(work.getTitle());
            TextView workTitle = GuiUtils.getView(drawerHeader, R.id.work_title);
            TextView workCreated = GuiUtils.getView(drawerHeader, R.id.work_created);
            TextView workUpdated = GuiUtils.getView(drawerHeader, R.id.work_updated);
            TextView workGenres = GuiUtils.getView(drawerHeader, R.id.work_genres);
            TextView workSeries = GuiUtils.getView(drawerHeader, R.id.work_series);
            GuiUtils.setText(workTitle, work.getTitle());
            if (work.getCreateDate() != null) {
                GuiUtils.setText(workCreated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getCreateDate()));
                GuiUtils.setVisibility(View.VISIBLE, drawerHeader, R.id.work_created_layout);
            } else {
                GuiUtils.setVisibility(View.GONE, drawerHeader, R.id.work_created_layout);
            }
            if (work.getUpdateDate() != null) {
                GuiUtils.setText(workUpdated, new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(work.getUpdateDate()));
                GuiUtils.setVisibility(View.VISIBLE, drawerHeader, R.id.work_updated_layout);
            } else {
                GuiUtils.setVisibility(View.GONE, drawerHeader, R.id.work_updated_layout);
            }
            if(TextUtils.isEmpty(work.printGenres())) {
                GuiUtils.setVisibility(View.GONE, drawerHeader, R.id.work_genres_layout);
            } else {
                GuiUtils.setVisibility(View.VISIBLE, drawerHeader, R.id.work_genres_layout);
                GuiUtils.setText(workGenres, work.printGenres());
            }
            if(TextUtils.isEmpty(work.getType().getTitle())) {
                GuiUtils.setVisibility(View.GONE, drawerHeader, R.id.work_series_layout);
            } else {
                GuiUtils.setVisibility(View.VISIBLE, drawerHeader, R.id.work_series_layout);
                GuiUtils.setText(workSeries, work.getType().getTitle());
            }
            setNavigationLayoutWidth(GuiUtils.dpToPx(navigationFixedDpWidth, this));
        }
    }

    @SuppressLint("ResourceType")
    private View initNavigationView(@LayoutRes int header, Object... titles) {
        removeHeaderView();
        clearNavigationMenu();
        for (int i = 0; i < titles.length; i++) {
            Object title = titles[i];
            if (!TextUtils.isEmpty(title.toString())) {
                addNavigationMenu(title.toString());
            } else {
                if (state.equals(SectionActivityState.ILLUSTRATIONS)) {
                    addNavigationMenu(String.valueOf(i));
                } else {
                    addNavigationMenu(title.toString());
                }
            }
        }

        if (header > 0) {
            drawerHeader = (ViewGroup) getLayoutInflater().inflate(header, navigationView, false);
            addHeader(drawerHeader);
            return null;
        }
        return drawerHeader;
    }


    private void initializeComments(List<Integer> pages) {
        setState(SectionActivityState.COMMENTS);
        initNavigationView(0, pages.toArray());
        setNavigationLayoutWidth(GuiUtils.dpToPx(60, this));
    }

    private void initializeIllustrations(List<Image> images) {
        setState(SectionActivityState.ILLUSTRATIONS);
        initNavigationView(0, images.toArray());
        setNavigationLayoutWidth(GuiUtils.dpToPx(navigationFixedDpWidth, this));
    }


    @Override
    protected boolean onNavigationItemSelected(int position, String title, View item) {
        switch (state) {
            case INIT:
                return false;
            case WORK:
                postEvent(new ChapterSelectedEvent(work.getAutoBookmarks().get(position)));
                return false;
            case AUTHOR:
                postEvent(new CategorySelectedEvent(author.getLinkableCategory().get(position)));
                break;
            case COMMENTS:
                postEvent(new SelectCommentPageEvent(position));
                break;
            case ILLUSTRATIONS:
                postEvent(new IllustrationSelectedEvent(position));
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment fr = getCurrentFragment();
        if (!(fr instanceof BackCallback) || ((BackCallback) fr).allowBackPress()) {
            if (isTaskRoot()) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                onBackPressedOriginal();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AuthorParsedEvent event) {
        initializeAuthor(event.author);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(WorkParsedEvent event) {
        initializeWork(event.work);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CommentsParsedEvent event) {
        initializeComments(event.pages);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(IllustrationsParsedEvent event) {
        initializeIllustrations(event.images);
    }

    public static void launchActivity(Context context, String link) {
        if (link != null) {
            Intent i = new Intent(context, SectionActivity.class);
            i.setData(Uri.parse(link));
            context.startActivity(i);
        }
    }


}
