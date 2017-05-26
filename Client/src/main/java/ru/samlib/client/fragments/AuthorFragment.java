package ru.samlib.client.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Browser;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import butterknife.OnClick;
import com.annimon.stream.Stream;
import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.adapter.LazyItemListAdapter;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.parser.CategoryParser;
import ru.samlib.client.parser.Parser;
import ru.samlib.client.service.DatabaseService;
import ru.kazantsev.template.util.FragmentBuilder;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.util.LinkHandler;
import ru.samlib.client.util.PicassoImageHandler;
import ru.samlib.client.util.SamlibGuiUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Dmitry on 10.07.2015.
 */
public class AuthorFragment extends ListFragment<Linkable> {

    private static final String TAG = AuthorFragment.class.getSimpleName();

    private Author author;
    private Category category;
    private boolean simpleView = true;
    @Inject
    DatabaseService databaseService;



    public static AuthorFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, AuthorFragment.class);
    }

    public static AuthorFragment show(FragmentBuilder builder, @IdRes int container, Author author) {
        return show(builder.putArg(Constants.ArgsName.AUTHOR, author), container, AuthorFragment.class);
    }

    public static AuthorFragment show(BaseFragment fragment, String link) {
        return show(fragment, AuthorFragment.class, Constants.ArgsName.LINK, link);
    }

    public static AuthorFragment show(BaseFragment fragment, Author author) {
        return show(fragment, AuthorFragment.class, Constants.ArgsName.AUTHOR, author);
    }

    public AuthorFragment() {
        setDataSource((skip, size) -> {
            if (skip != 0) return null;
            while (author == null) {
                SystemClock.sleep(100);
            }
            if (!author.isParsed()) {
                new AuthorParser(author).parse();
                if (!Parser.isCachedMode() && author.isObservable()) {
                    databaseService.createOrUpdateAuthor(author.createEntity()).setParsed(true);
                    if (author.isHasUpdates()) {
                        postEvent(new AuthorUpdatedEvent(author));
                    }
                }
                author.setParsed(true);
                postEvent(new AuthorParsedEvent(author));
                safeInvalidateOptionsMenu();
            }
            return new ArrayList<>(author.getStaticCategory());
        });
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        if (ex instanceof IOException) {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.author, menu);
        if (author.isParsed()) {
            MenuItem item = menu.findItem(R.id.action_author_observable);
            if (author.isObservable()) {
                item.setChecked(true);
            } else {
                item.setChecked(false);
            }
        } else {
            menu.removeItem(R.id.action_author_observable);
        }
        if(!author.isObservable() || !author.isParsed()) {
            menu.removeItem(R.id.action_author_make_all_visited);
        }
        menu.findItem(R.id.action_author_mode).setChecked(simpleView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_author_observable:
                if (item.isChecked()) {
                    author.setObservable(false);
                    new Thread(() -> databaseService.createOrUpdateAuthor(author.createEntity())).start();
                    item.setChecked(false);
                    return true;
                } else {
                    new Thread(() -> databaseService.insertObservableAuthor(author.createEntity())).start();
                    item.setChecked(true);
                    return true;
                }
            case R.id.action_author_mode:
                if (item.isChecked()) {
                    item.setChecked(simpleView = false);
                } else {
                    item.setChecked(simpleView = true);
                }
                SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                editor.putBoolean(getString(R.string.preferenceAuthorSimpleView), simpleView);
                editor.apply();
                if(adapter != null) {
                    adapter.getItems().clear();
                }
                adapter = newAdapter();
                itemList.setAdapter(adapter);
                super.refreshData(false);
                return true;
            case R.id.action_author_make_all_visited:
                if(author.isEntity()) {
                    Stream.of(author.getAllWorks().entrySet()).map(Map.Entry::getValue).forEach(work -> {
                        work.setChanged(false);
                        work.setSizeDiff(null);
                    });
                    databaseService.createOrUpdateAuthor(author.createEntity());
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
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

    public Author getAuthor() {
        return author;
    }

    @Subscribe
    public void onEvent(CategorySelectedEvent event) {
        if (category == null) {
            saveLister();
            setDataSource((skip, size) -> {
                if (skip != 0) return null;
                if (category != null) {
                    if (!category.isParsed() && !(category instanceof CategoryEntity)) {
                        try {
                            category = new CategoryParser(category).parse();
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "Unknown exception", e);
                            return new ArrayList<>();
                        }
                    }
                    return Arrays.asList(category);

                } else {
                    return new ArrayList<>();
                }
            });
        }
        category = event.category;
        refreshData(true);
    }

    @Override
    public void refreshData(boolean showProgress) {
        author.setParsed(false);
        super.refreshData(showProgress);
    }


    @Override
    public boolean allowBackPress() {
        category = null;
        if (!restoreLister()) {
            return super.allowBackPress();
        } else {
            ((SectionActivity) getActivity()).cleanSelection();
            return false;
        }
    }

    @Override
    protected ItemListAdapter<Linkable> newAdapter() {
        if(simpleView) {
            return new ExpandableAuthorFragmentAdaptor();
        } else {
            return new AuthorFragmentAdaptor();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Author incomingAuthor = (Author) getArguments().getSerializable(Constants.ArgsName.AUTHOR);
        Author intentAuthor = null;
        setHasOptionsMenu(true);
        if (incomingAuthor != null && !incomingAuthor.equals(author)) {
            intentAuthor = incomingAuthor;
        } else if (link != null && (author == null || !author.getLink().equals(link))) {
            intentAuthor = new Author(link);
        }
        if (intentAuthor != null) {
            AuthorEntity entity;
            if ((entity = databaseService.getAuthor(intentAuthor.getLink())) != null) {
                author = entity;
            } else {
                author = intentAuthor;
            }
            clearData();
        }
        if (author.isParsed()) {
            safeInvalidateOptionsMenu();
            EventBus.getDefault().post(new AuthorParsedEvent(author));
        }
        simpleView = AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceAuthorSimpleView, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void openLinkable(Linkable linkable) {
        if (linkable.isWork()) {
            WorkFragment.show(newFragmentBuilder()
                            .addToBackStack()
                            .setAnimation(R.anim.slide_in_left, R.anim.slide_out_right)
                            .setPopupAnimation(R.anim.slide_in_right, R.anim.slide_out_left)
                    , getId(), linkable.getLink());
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkable.getLink()));
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
            getActivity().startActivity(intent);
        }
    }

    private class ExpandableAuthorFragmentAdaptor extends LazyItemListAdapter<Linkable> {

        public ExpandableAuthorFragmentAdaptor() {
            super(R.layout.item_section_expandable_header);
        }

        @Override
        public void onBindHolder(ViewHolder holder, @Nullable Linkable item) {
            Category category = (Category) item;
            if (holder.getTag() == category) {
                return;
            }
            boolean navBarCategory = savedDataSource != null;
            ViewGroup root = (ViewGroup) holder.getItemView();
            GuiUtils.setText(root, R.id.section_label, category.getTitle());
            ToggleButton expand = (ToggleButton) root.findViewById(R.id.section_expand_switch);
            expand.setTag(category);
            ViewGroup itemsView = ((ViewGroup) root.findViewById(R.id.section_layout_subitems));
            itemsView.removeAllViews();
            if(category.isHasUpdates()) {
                GuiUtils.setVisibility(View.VISIBLE, root,  R.id.section_update);
            } else {
                GuiUtils.setVisibility(View.GONE, root,  R.id.section_update);
            }
            if(navBarCategory) {
                GuiUtils.setVisibility(View.GONE, root, R.id.section_down_shadow, R.id.section_layout);
            }
            if(category.isInUIExpanded() || navBarCategory) {
                expand.setChecked(true);
                for (Work work : category.getWorks()) {
                    addWorkToRootItem(itemsView, work);
                }
                for (Link link : category.getLinks()) {
                    addLinkToRootItem(itemsView, link);
                }
                if(category.getWorks().size() > 0 || category.getLinks().size() > 0) {
                    itemsView.addView(new View(getContext()), -1, GuiUtils.dpToPx(3, getContext()));
                }
            } else {
                expand.setChecked(false);
            }
            expand.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ViewGroup itemRoot = (ViewGroup) ((ViewGroup) buttonView.getParent().getParent()).findViewById(R.id.section_layout_subitems);
                Category categoryTag = (Category) buttonView.getTag();
                categoryTag.setInUIExpanded(isChecked);
                if(isChecked) {
                    for (Work work : categoryTag.getWorks()) {
                        addWorkToRootItem(itemRoot, work);
                    }
                    for (Link link : categoryTag.getLinks()) {
                        addLinkToRootItem(itemRoot, link);
                    }
                    if(categoryTag.getWorks().size() > 0 || categoryTag.getLinks().size() > 0) {
                        itemRoot.addView(new View(getContext()), -1, GuiUtils.dpToPx(3, getContext()));
                    }
                } else {
                    itemRoot.removeAllViews();
                }
            });
            holder.setTag(category);
        }

        @Override
        public void onClick(View view) {
            if(view.getTag() instanceof  Linkable) {
                onClickLinkable(view, (Linkable) view.getTag());
            }
        }

        private void addWorkToRootItem(ViewGroup rootView, Work work) {
            ViewGroup subitem = GuiUtils.inflate(rootView, R.layout.item_section_extendale_subitem);
            initWorkView(subitem, work);
            Stream.of(GuiUtils.getAllChildren(subitem)).forEach(v -> {v.setOnClickListener(this); v.setTag(work);});
            subitem.setOnClickListener(this);
            rootView.addView(subitem);
        }

        private void addLinkToRootItem(ViewGroup rootView, Link link) {
            ViewGroup subitem = GuiUtils.inflate(rootView, R.layout.item_section_extendale_subitem);
            String title;
            if (link.getAnnotation() != null) {
                title = link.getTitle() + ": " + link.getAnnotation();
            } else {
                title = link.getTitle();
            }
            Stream.of(GuiUtils.getAllChildren(subitem)).forEach(v -> {v.setOnClickListener(this); v.setTag(link);});
            GuiUtils.setText(subitem.findViewById(R.id.work_item_title), GuiUtils.coloredText(getActivity(), title, R.color.material_deep_teal_200));
            rootView.addView(subitem);
        }
    }

    private void initWorkView(View workView, Work work) {
        String rate_and_size = "";
        if (work.getSize() != null) {
            rate_and_size += work.getSize() + "k";
            if (work.getSizeDiff() != null && work.getSizeDiff() != 0 && author.isObservable()) {
                if (work.getSizeDiff() > 0) {
                    rate_and_size += " (+" + work.getSizeDiff() + ")";
                } else {
                    rate_and_size += " (" + work.getSizeDiff() + ")";
                }
            }
        }
        if (work.getRate() != null) {
            rate_and_size += " " + work.getRate() + "*" + work.getKudoed();
        }
        GuiUtils.setText(workView.findViewById(R.id.work_item_title), SamlibGuiUtils.generateText(getContext(), work.getTitle(), rate_and_size, R.color.light_gold, 0.7f));
        Button illustrationButton = (Button) workView.findViewById(R.id.illustration_button);
        if (work.isHasIllustration()) {
            illustrationButton.setVisibility(View.VISIBLE);
        } else {
            illustrationButton.setVisibility(View.GONE);
        }
        Button commentsButton = (Button) workView.findViewById(R.id.comments_button);
        if (work.isHasComments()) {
            commentsButton.setVisibility(View.VISIBLE);
        } else {
            commentsButton.setVisibility(View.GONE);
        }
        if (!work.getGenres().isEmpty()) {
            GuiUtils.setTextOrHide(workView.findViewById(R.id.work_item_subtitle),
                    getString(R.string.item_genres_label) + " " + work.printGenres());
        } else {
            workView.findViewById(R.id.work_item_subtitle).setVisibility(View.GONE);
        }
        if(workView.findViewById(R.id.work_annotation) != null) {
            if (!work.getAnnotationBlocks().isEmpty()) {
                workView.findViewById(R.id.work_annotation_layout).setVisibility(View.VISIBLE);
                View annotation_view = workView.findViewById(R.id.work_annotation);
                TextView textView = (TextView) annotation_view;
                HtmlSpanner spanner = new HtmlSpanner();
                spanner.registerHandler("img", new PicassoImageHandler(textView));
                spanner.registerHandler("a", new LinkHandler(textView));
                spanner.setStripExtraWhiteSpace(true);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setText(spanner.fromHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold))));
            } else {
                workView.findViewById(R.id.work_annotation).setVisibility(View.GONE);
            }
        }
        if (work.isChanged() && author.isObservable()) {
            workView.findViewById(R.id.work_item_update).setVisibility(View.VISIBLE);
            if (work.getSizeDiff() != null) {
                GuiUtils.setText((TextView) workView.findViewById(R.id.work_item_update), R.string.favorites_update);
            } else {
                GuiUtils.setText((TextView) workView.findViewById(R.id.work_item_update), R.string.favorites_new);
            }
        } else {
            workView.findViewById(R.id.work_item_update).setVisibility(View.GONE);
        }
    }

    public boolean onClickLinkable(View view, Linkable linkable) {
        switch (view.getId()) {
            case R.id.work_item_layout:
            case R.id.work_item_title:
            case R.id.work_item_rate_and_size:
                openLinkable(linkable);
                return true;
            case R.id.illustration_button:
                IllustrationPagerFragment.show(newFragmentBuilder()
                        .addToBackStack()
                        .setAnimation(R.anim.slide_in_left, R.anim.slide_out_right)
                        .setPopupAnimation(R.anim.slide_in_right, R.anim.slide_out_left), getId(), (Work) linkable);
                return true;
            case R.id.comments_button:
                CommentsPagerFragment.show(newFragmentBuilder()
                        .addToBackStack()
                        .setAnimation(R.anim.slide_in_left, R.anim.slide_out_right)
                        .setPopupAnimation(R.anim.slide_in_right, R.anim.slide_out_left), getId(), (Work) linkable);
                return true;
        }
        return false;
    }

    private class AuthorFragmentAdaptor extends MultiItemListAdapter<Linkable> {

        public AuthorFragmentAdaptor() {
            super(true, R.layout.header_author_list, R.layout.item_section, R.layout.item_work);
        }

        @Override
        public void onClick(View view, int position) {
             onClickLinkable(view, getItem(position));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case R.layout.header_author_list:
                    initHeader(holder);
                    break;
                case R.layout.item_section:
                    Category category = (Category) getItem(position);
                    GuiUtils.setTextOrHide(holder.getView(R.id.section_label), category.getTitle() + ":");
                    if (!TextUtils.isEmpty(category.getAnnotation())) {
                        HtmlSpanner spanner = new HtmlSpanner();
                        TextView annotationView = holder.getView(R.id.section_annotation);
                        spanner.registerHandler("img", new PicassoImageHandler(annotationView));
                        spanner.registerHandler("a", new LinkHandler(annotationView));
                        spanner.setStripExtraWhiteSpace(true);
                        annotationView.setVisibility(View.VISIBLE);
                        annotationView.setMovementMethod(LinkMovementMethod.getInstance());
                        annotationView.setText(spanner.fromHtml(category.processAnnotation(getResources().getColor(R.color.SeaGreen))));
                    } else {
                        holder.getView(R.id.section_annotation).setVisibility(View.GONE);
                    }
                    break;
                case R.layout.item_work:
                    Linkable linkable = getItem(position);
                    if (linkable instanceof Link) {
                        CharSequence link;
                        if (linkable.getAnnotation() != null) {
                            link = linkable.getTitle() + ": " + linkable.getAnnotation();
                        } else {
                            link = linkable.getTitle();
                        }
                        GuiUtils.setText(holder.getView(R.id.work_item_title), GuiUtils.coloredText(getActivity(), link, R.color.material_deep_teal_200));
                        break;
                    } else {
                        Work work = (Work) getItem(position);
                        initWorkView(holder.getItemView(), work);
                    }
            }
        }

        private void initHeader(ViewHolder holder) {
            TextView authorAboutText = holder.getView(R.id.author_about_text);
            LinearLayout authorAboutLayout = holder.getView(R.id.author_about_layout);
            LinearLayout authorSuggestions = holder.getView(R.id.author_suggestions);//holder.getView(R.id.author_suggestions);
            LinearLayout authorSuggestionLayout = holder.getView(R.id.author_suggestion_layout);
            TextView authorSectionAnnotationText = holder.getView(R.id.author_section_annotation_text);
            LinearLayout authorSectionAnnotationLayout = holder.getView(R.id.author_section_annotation_layout);
            GridLayout authorGridInfo = holder.getView(R.id.author_grid_info);
            GuiUtils.setTextOrHide(authorAboutText, author.getAbout(), authorAboutLayout);
            GuiUtils.setTextOrHide(authorSectionAnnotationText, author.getSectionAnnotation(), authorSectionAnnotationLayout);
            if (author.getRecommendations().size() > 0) {
                authorSuggestions.removeAllViews();
                Stream.of(author.getRecommendations()).forEach(work -> {
                    LinearLayout work_row = (LinearLayout) getLayoutInflater(null)
                            .inflate(R.layout.item_work, authorSuggestions, false);
                    GuiUtils.setTextOrHide(work_row.findViewById(R.id.work_item_title), work.getTitle());
                    String rate_and_size = "";
                    if (work.getSize() != null) {
                        rate_and_size += work.getSize() + "k";
                    }
                    if (work.getRate() != null) {
                        rate_and_size += " " + work.getRate() + "*" + work.getKudoed();
                    }
                    GuiUtils.setTextOrHide(work_row.findViewById(R.id.work_item_rate_and_size), rate_and_size);
                    GuiUtils.setTextOrHide(work_row.findViewById(R.id.work_item_subtitle), work.getTypeName());
                    work_row.setOnClickListener(v -> {
                        WorkFragment.show(AuthorFragment.this, ((Work) v.getTag()).getLink());
                    });
                    work_row.setTag(work);
                    authorSuggestions.addView(work_row);
                });
                authorSuggestionLayout.setVisibility(View.VISIBLE);
            } else {
                authorSuggestionLayout.setVisibility(View.GONE);
            }
            authorGridInfo.removeAllViews();
            for (String title : getResources().getStringArray(R.array.author_grid)) {
                switch (title) {
                    case "WWW:":
                        addToGrid(authorGridInfo, title, author.getAuthorSiteUrl());
                        break;
                    case "Адрес:":
                        addToGrid(authorGridInfo, title, author.getAddress());
                        break;
                    case "Родился:":
                        addToGrid(authorGridInfo, title, author.getDateBirth());
                        break;
                    case "Обновлялось:":
                        addToGrid(authorGridInfo, title, author.getLastUpdateDate());
                        break;
                    case "Объем:":
                        addToGrid(authorGridInfo, title, author.getSize());
                        break;
                    case "Рейтинг:":
                        addToGrid(authorGridInfo, title, author.getRate());
                        break;
                    case "Посетителей за год:":
                        addToGrid(authorGridInfo, title, author.getViews());
                        break;
                    case "Friends:":
                        addToGrid(authorGridInfo, title, author.getFriends());
                        break;
                    case "Friend Of:":
                        addToGrid(authorGridInfo, title, author.getFriendsOf());
                        break;
                }
            }
        }

        private void addToGrid(GridLayout authorGridInfo, String title, Object content) {
            if (content != null) {
                TextView textTitle = new TextView(new ContextThemeWrapper(authorGridInfo.getContext(), R.style.author_info_column_0));
                TextView textContent = new TextView(new ContextThemeWrapper(authorGridInfo.getContext(), R.style.author_info_column_1));
                textTitle.setText(title);
                if (content instanceof Date) {
                    content = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN).format(content);
                }
                textContent.setText(content.toString());
                authorGridInfo.addView(textTitle);
                authorGridInfo.addView(textContent);
            }
        }

        @Override
        public int getLayoutId(Linkable item) {
            if (item instanceof Category) {
                return R.layout.item_section;
            } else {
                return R.layout.item_work;
            }
        }

        @Override
        public List<Linkable> getSubItems(Linkable item) {
            if (item instanceof Category) {
                return ((Category) item).getLinkables();
            } else {
                return null;
            }
        }

        @Override
        public boolean hasSubItems(Linkable item) {
            return item instanceof Category;
        }
    }

}
