package ru.samlib.client.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.annimon.stream.Stream;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.GuiUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class AuthorFragment extends BaseFragment {

    @Bind(R.id.author_about_text)
    TextView authorAboutText;
    @Bind(R.id.author_about_layout)
    LinearLayout authorAboutLayout;
    @Bind(R.id.author_suggestions)
    LinearLayout authorSuggestions;
    @Bind(R.id.author_suggestion_layout)
    LinearLayout authorSuggestionLayout;
    @Bind(R.id.author_top_layout)
    RelativeLayout authorTopLayout;
    @Bind(R.id.author_section_annotation_text)
    TextView authorSectionAnnotationText;
    @Bind(R.id.author_section_annotation_layout)
    LinearLayout authorSectionAnnotationLayout;
    @Bind(R.id.author_grid_info)
    GridLayout authorGridInfo;
    @Bind(R.id.author_grid_layout)
    LinearLayout authorGridLayout;

    private Author author;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.header_author_list, container, false);
        Author newAuthor = (Author) getArguments().getSerializable(Constants.ArgsName.AUTHOR);
        ButterKnife.bind(this, rootView);
        if (author == null || !author.equals(newAuthor)) {
            author = newAuthor;
            GuiUtils.setTextOrHide(authorAboutText, author.getAbout(), authorAboutLayout);
            GuiUtils.setTextOrHide(authorSectionAnnotationText, author.getSectionAnnotation(), authorSectionAnnotationLayout);
            if (author.getRecommendations().size() > 0) {
                authorSuggestionLayout.setVisibility(View.VISIBLE);
                Stream.of(author.getRecommendations()).forEach(work -> {
                    LinearLayout work_row = (LinearLayout) inflater
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
                    authorSuggestions.addView(work_row);
                });
            } else {
                authorSuggestionLayout.setVisibility(View.GONE);
            }


            for (String title : getResources().getStringArray(R.array.author_grid)) {
                switch (title) {
                    case "WWW:":
                        addToGrid(title, author.getSite());
                        break;
                    case "Адрес:":
                        addToGrid(title, author.getAddress());
                        break;
                    case "Родился:":
                        addToGrid(title, author.getDateBirth());
                        break;
                    case "Обновлялось:":
                        addToGrid(title, author.getLastUpdateDate());
                        break;
                    case "Объем:":
                        addToGrid(title, author.getSize());
                        break;
                    case "Рейтинг:":
                        addToGrid(title, author.getRate());
                        break;
                    case "Посетителей за год:":
                        addToGrid(title, author.getViews());
                        break;
                    case "Friends:":
                        addToGrid(title, author.getFriends());
                        break;
                    case "Friend Of:":
                        addToGrid("Friend Of:", author.getFriendsOf());
                        break;
                }
            }


        }
        return rootView;
    }


    private void addToGrid(String title, Object content) {
        if (content != null) {
            TextView textTitle = new TextView(new ContextThemeWrapper(authorGridInfo.getContext(), R.style.author_info_column_0));
            TextView textContent = new TextView(new ContextThemeWrapper(authorGridInfo.getContext(), R.style.author_info_column_1));
            textTitle.setText(title);
            if(content instanceof Date) {
                content = new SimpleDateFormat("dd MM yyyy").format(content);
            }
            textContent.setText(content.toString());
            authorGridInfo.addView(textTitle);
            authorGridInfo.addView(textContent);
        }
    }
}
