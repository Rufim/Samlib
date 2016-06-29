package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Browser;
import android.support.annotation.IdRes;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.parser.CategoryParser;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.service.ObservableService;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.LinkHandler;
import ru.samlib.client.util.PicassoImageHandler;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Dmitry on 10.07.2015.
 */
public class AuthorFragment extends ListFragment<Linkable> {

    private static final String TAG = AuthorFragment.class.getSimpleName();

    private Author author;
    private Category category;
    @Inject
   ObservableService observableModule;

    public static void show(FragmentBuilder builder, @IdRes int container, String link) {
        show(builder, container, AuthorFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(FragmentBuilder builder, @IdRes int container, Author author) {
        show(builder, container, AuthorFragment.class, Constants.ArgsName.AUTHOR, author);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, AuthorFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Author author) {
        show(fragment, AuthorFragment.class, Constants.ArgsName.AUTHOR, author);
    }

    public AuthorFragment() {
        pageSize = 10;
        setDataSource((skip, size) -> {
            while (author == null) {
                SystemClock.sleep(10);
            }
            if (!author.isParsed()) {
                try {
                    if(author.getId() == null) {
                        AuthorEntity authorEntity = observableModule.getAuthorByLink(author.getLink());
                        if (authorEntity != null) {
                            author = authorEntity;
                        }
                        author = new AuthorParser(author).parse();
                        if (authorEntity != null) {
                            observableModule.upsertAuthor(authorEntity);
                            if(authorEntity.isHasUpdates()) {
                                postEvent(new AuthorUpdatedEvent(authorEntity));
                            }
                        }
                    } else {
                        author = observableModule.getAuthorById(author.getId());
                    }
                    author.setParsed(true);
                    postEvent(new AuthorParsedEvent(author));
                    getActivity().invalidateOptionsMenu();
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                    return new ArrayList<>();
                }
            }
            return Stream.of(author.getStaticCategory())
                    .skip(skip)
                    .limit(size)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.author, menu);
        MenuItem item = menu.findItem(R.id.action_author_observable);
        if(author instanceof AuthorEntity) {
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_author_observable:
                if(!item.isChecked()) {
                    observableModule.insertAuthor(author.createEntry());
                    item.setChecked(true);
                    return true;
                } else {
                    observableModule.deleteAuthor((AuthorEntity) author);
                    item.setChecked(false);
                    return true;
                }
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

    public void onEvent(CategorySelectedEvent event) {
        if (category == null) {
            saveLister();
            setDataSource((skip, size) -> {
                if (category != null) {
                    if (!category.isParsed()) {
                        try {
                            category = new CategoryParser(category).parse();
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "Unknown exception", e);
                            return new ArrayList<>();
                        }
                    }
                    return Stream.of(category)
                            .skip(skip)
                            .limit(size)
                            .collect(Collectors.toList());
                } else {
                    return new ArrayList<>();
                }
            });
        }
        category = event.category;
        refreshData(true);
    }

    @Override
    public boolean allowBackPress() {
        category = null;
        if (!restoreLister()) {
            return super.allowBackPress();
        } else {
            return false;
        }
    }

    @Override
    protected ItemListAdapter<Linkable> getAdapter() {
        return new AuthorFragmentAdaptor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Author incomingAuthor = (Author) getArguments().getSerializable(Constants.ArgsName.AUTHOR);
        setHasOptionsMenu(true);
        if (incomingAuthor != null) {
            if (!incomingAuthor.equals(author)) {
                author = incomingAuthor;
                clearData();
            }
        } else if (link != null) {
            if (author == null || !author.getLink().equals(link)) {
                author = new Author(link);
                clearData();
            }
        }
        if (author.isParsed()) {
            EventBus.getDefault().post(new AuthorParsedEvent(author));
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private class AuthorFragmentAdaptor extends MultiItemListAdapter<Linkable> {


        public AuthorFragmentAdaptor() {
            super(true, R.layout.header_author_list, R.layout.item_section, R.layout.item_work);
        }

        @Override
        public void onClick(View view, int position) {
            switch (view.getId()) {
                case R.id.work_item_layout:
                case R.id.work_item_title:
                case R.id.work_item_rate_and_size:
                    openLinkable(getItem(position));
                    break;
                case R.id.illustration_button:
                    IllustrationPagerFragment.show(AuthorFragment.this, (Work) getItem(position));
                    break;
                case R.id.comments_button:
                    CommentsFragment.show(AuthorFragment.this, (Work) getItem(position));
                    break;
            }
        }

        public void openLinkable(Linkable linkable) {
            if (linkable.isWork()) {
                Work work = (Work) linkable;
                if(work.isChanged() && work.getId() != null) {
                    work.setChanged(false);
                    work.setSizeDiff(null);
                    getDataStore().update((WorkEntity) work);
                }
                WorkFragment.show(AuthorFragment.this, linkable.getLink());
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkable.getLink()));
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
                getActivity().startActivity(intent);
            }

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case R.layout.header_author_list:
                    initHeader(holder);
                    break;
                case R.layout.item_section:
                    Category category = (Category) getItem(position);
                    GuiUtils.setTextOrHide(holder.getView(R.id.section_label), category.getTitle());
                    if (category.getAnnotation() != null) {
                        HtmlSpanner spanner = new HtmlSpanner();
                        TextView annotationView = holder.getView(R.id.section_annotation);
                        spanner.registerHandler("img", new PicassoImageHandler(annotationView));
                        spanner.registerHandler("a", new LinkHandler(annotationView));
                        annotationView.setText(spanner.fromHtml(category.processAnnotation(getResources().getColor(R.color.SeaGreen))));
                        annotationView.setVisibility(View.VISIBLE);
                    } else {
                        holder.getView(R.id.section_annotation).setVisibility(View.GONE);
                    }
                    break;
                case R.layout.item_work:
                    Linkable linkable = getItem(position);
                    if (linkable.getClass() == Link.class) {
                        CharSequence link;
                        if (linkable.getAnnotation() != null) {
                            link = linkable.getTitle() + ": " + linkable.getAnnotation();
                        } else {
                            link = linkable.getTitle();
                        }
                        GuiUtils.setText(holder.getView(R.id.work_item_title), GuiUtils.coloredText(getActivity(), link, R.color.material_deep_teal_200));
                        holder.getView(R.id.work_item_rate_and_size).setVisibility(View.GONE);
                        break;
                    }
                    Work work = (Work) getItem(position);
                    GuiUtils.setText(holder.getView(R.id.work_item_title), work.getTitle());
                    String rate_and_size = "";
                    if (work.getSize() != null) {
                        rate_and_size += work.getSize() + "k";
                        if(work.getSizeDiff() != null) {
                            if(work.getSizeDiff() > 0) {
                                rate_and_size += " (+" + work.getSizeDiff() + ")";
                            } else {
                                rate_and_size += " (" + work.getSizeDiff() + ")";
                            }
                        }
                    }
                    if (work.getRate() != null) {
                        rate_and_size += " " + work.getRate() + "*" + work.getKudoed();
                    }
                    GuiUtils.setTextOrHide(holder.getView(R.id.work_item_rate_and_size), rate_and_size);
                    Button illustrationButton = holder.getView(R.id.illustration_button);
                    if (work.isHasIllustration()) {
                        illustrationButton.setVisibility(View.VISIBLE);
                    } else {
                        illustrationButton.setVisibility(View.GONE);
                    }
                    Button commentsButton = holder.getView(R.id.comments_button);
                    if (work.isHasComments()) {
                        commentsButton.setVisibility(View.VISIBLE);
                    } else {
                        commentsButton.setVisibility(View.GONE);
                    }
                    if (!work.getGenres().isEmpty()) {
                        GuiUtils.setTextOrHide(holder.getView(R.id.work_item_subtitle),
                                getString(R.string.item_genres_label) + " " + work.printGenres());
                    } else {
                        holder.getView(R.id.work_item_subtitle).setVisibility(View.GONE);
                    }
                    if (!work.getAnnotationBlocks().isEmpty()) {
                        holder.getView(R.id.work_annotation_layout).setVisibility(View.VISIBLE);
                        View annotation_view = holder.getView(R.id.work_annotation);
                        TextView textView = (TextView) annotation_view;
                        HtmlSpanner spanner = new HtmlSpanner();
                        spanner.registerHandler("img", new PicassoImageHandler(textView));
                        spanner.registerHandler("a", new LinkHandler(textView));
                        textView.setText(spanner.fromHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold))));
                    } else {
                        holder.getView(R.id.work_annotation).setVisibility(View.GONE);
                    }
                    if (work.isChanged() && work.getId() != null) {
                        holder.getView(R.id.work_item_update).setVisibility(View.VISIBLE);
                    } else {
                        holder.getView(R.id.work_item_update).setVisibility(View.GONE);
                    }
                    break;
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
                        WorkFragment.show(AuthorFragment.this, (Work) v.getTag());
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
                        addToGrid(authorGridInfo, title, author.getSite());
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
