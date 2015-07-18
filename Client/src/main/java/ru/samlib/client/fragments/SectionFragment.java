package ru.samlib.client.fragments;

import android.os.Bundle;
import android.os.SystemClock;
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
import com.squareup.picasso.Picasso;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Section;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.PicassoImageHandler;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Dmitry on 10.07.2015.
 */
public class SectionFragment extends ListFragment<Linkable> {

    private static final String TAG = SectionFragment.class.getSimpleName();

    private Author author;
    private List<AuthorListener> listeners = new ArrayList<>();

    public interface AuthorListener {
        public void onAuthorParsed(Author author);
    }

    public SectionFragment addListener(AuthorListener listener) {
        listeners.add(listener);
        return this;
    }

    public SectionFragment() {
        setLister((skip, size) -> {
            while (author == null) {
                SystemClock.sleep(10);
            }
            if (!author.isParsed()) {
                try {
                    author = new AuthorParser(author).parse();
                    author.setParsed(true);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            for (AuthorListener listener : listeners) {
                                listener.onAuthorParsed(author);
                            }
                        });
                    }
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Unknown exception", e);
                }
            }
            return Stream.of(author.getStaticSections())
                    .skip(skip)
                    .limit(size)
                    .collect(Collectors.toList());
        });

    }

    @Override
    protected ItemListAdapter<Linkable> getAdapter() {
        return new SectionFragmentAdaptor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String link = getArguments().getString(Constants.ArgsName.LINK);
        if (author == null || !author.getLink().equals(link)) {
            author = new Author(link);
        } else {
            for (AuthorListener listener : listeners) {
                listener.onAuthorParsed(author);
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private class SectionFragmentAdaptor extends MultiItemListAdapter<Linkable> {

        public SectionFragmentAdaptor() {
            super(true, R.layout.fragment_author, R.layout.section_item, R.layout.work_item);
        }

        @Override
        public void onClick(View view, int position) {

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case R.layout.fragment_author:
                    initHeader(holder);
                    break;
                case R.layout.section_item:
                    Section section = (Section) getItem(position);
                    GuiUtils.setText(holder.getView(R.id.section_label), section.getTitle());
                    GuiUtils.setTextOrHide(holder.getView(R.id.section_annotation), section.getAnnotation());
                    break;
                case R.layout.work_item:
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
                            .inflate(R.layout.work_item, authorSuggestions, false);
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
                    Calendar cal = Calendar.getInstance();
                    cal.setTime((Date) content);
                    int month = cal.get(Calendar.MONTH);
                    content = new SimpleDateFormat("dd MM yyyy").format(content);
                }
                textContent.setText(content.toString());
                authorGridInfo.addView(textTitle);
                authorGridInfo.addView(textContent);
            }
        }

        @Override
        public int getLayoutId(Linkable item) {
            if (item.getClass() == Section.class) {
                return R.layout.section_item;
            } else {
                return R.layout.work_item;
            }
        }

        @Override
        public List<Linkable> getSubItems(Linkable item) {
            if (item.getClass() == Section.class) {
                return ((Section) item).getLinks();
            } else {
                return null;
            }
        }

        @Override
        public boolean hasSubItems(Linkable item) {
            return item.getClass() == Section.class;
        }
    }

}
