package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Browser;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.nd.android.sdp.im.common.widget.htmlview.view.HtmlView;
import de.greenrobot.event.EventBus;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Category;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.parser.CategoryParser;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;

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
                    author = new AuthorParser(author).parse();
                    author.setParsed(true);
                    postEvent(new AuthorParsedEvent(author));
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
        if (incomingAuthor != null) {
            if(!incomingAuthor.equals(author)) {
                author = incomingAuthor;
            }
            if (author.isParsed()) {
                EventBus.getDefault().post(new AuthorParsedEvent(author));
            }
        } else if (link != null) {
            if (author == null || !author.getLink().equals(link)) {
                author = new Author(link);
                clearData();
            }
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
            }
        }

        public void openLinkable(Linkable linkable) {
            if (linkable.isWork()) {
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
                    GuiUtils.setText(holder.getView(R.id.section_label), category.getTitle());
                    if (category.getAnnotation() != null) {
                        holder.getView(R.id.section_annotation).setVisibility(View.VISIBLE);
                        HtmlView htmlView = holder.getView(R.id.section_annotation);
                        htmlView.loadHtml(category.processAnnotation(getResources().getColor(R.color.SeaGreen)));
                    } else {
                        holder.getView(R.id.section_annotation).setVisibility(View.GONE);
                    }
                    break;
                case R.layout.item_work:
                    Linkable linkable = getItem(position);
                    if (linkable.getClass() == Link.class) {
                        GuiUtils.setText(holder.getView(R.id.work_item_title), linkable.getTitle());
                        holder.getView(R.id.work_item_rate_and_size).setVisibility(View.GONE);
                        break;
                    }
                    Work work = (Work) getItem(position);
                    GuiUtils.setText(holder.getView(R.id.work_item_title), work.getTitle());
                    String rate_and_size = "";
                    if (work.getSize() != null) {
                        rate_and_size += work.getSize() + "k";
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
                    if (!work.getGenres().isEmpty()) {
                        GuiUtils.setTextOrHide(holder.getView(R.id.work_item_subtitle),
                                getString(R.string.item_genres_label) + " " + work.printGenres());
                    } else {
                        holder.getView(R.id.work_item_subtitle).setVisibility(View.GONE);
                    }
                    if (!work.getAnnotationBlocks().isEmpty()) {
                        holder.getView(R.id.work_annotation_layout).setVisibility(View.VISIBLE);
                        HtmlView htmlView = holder.getView(R.id.work_annotation);
                        String testTable = "<table border=\"1\" cellpadding=\"4\" cellspacing=\"1\">    <tbody><tr><td width=\"400\"><img src=\"http://budclub.ru/img/p/plotnikow_sergej_aleksandrowich/podpiskairassylka/facebook.png\" alt=\"лого_фейсбук\" width=\"30\"><a href=\"https://www.facebook.com/plotnikovs.ru\" target=\"_blank\">Задать вопрос или поболтать на ФЕЙСБУКЕ</a> </td></tr>  </tbody></table>";
                        htmlView.loadHtml(work.processAnnotationBloks(getResources().getColor(R.color.light_gold)));
                    } else {
                        holder.getView(R.id.work_annotation).setVisibility(View.GONE);
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
                        new FragmentBuilder(getFragmentManager())
                                .putArg(Constants.ArgsName.LINK, v.getTag())
                                .addToBackStack()
                                .replaceFragment(AuthorFragment.this, WorkFragment.class);
                    });
                    work_row.setTag(work.getLink());
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
                    content = new SimpleDateFormat("dd MM yyyy").format(content);
                }
                textContent.setText(content.toString());
                authorGridInfo.addView(textTitle);
                authorGridInfo.addView(textContent);
            }
        }

        @Override
        public int getLayoutId(Linkable item) {
            if (item.getClass() == Category.class) {
                return R.layout.item_section;
            } else {
                return R.layout.item_work;
            }
        }

        @Override
        public List<Linkable> getSubItems(Linkable item) {
            if (item.getClass() == Category.class) {
                return ((Category) item).getLinks();
            } else {
                return null;
            }
        }

        @Override
        public boolean hasSubItems(Linkable item) {
            return item.getClass() == Category.class;
        }
    }

}
