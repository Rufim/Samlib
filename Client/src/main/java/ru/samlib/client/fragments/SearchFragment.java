package ru.samlib.client.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.parser.SearchParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 23.06.2015.
 */
public class SearchFragment extends ListFragment {

    protected static final String ARG_SEARCH_QUERY = "search_query";

    public SearchFragment() {
        super(new SearchParser());
    }

    public static SearchFragment newInstance(String query) {
        Bundle args = new Bundle();
        args.putString(ARG_SEARCH_QUERY, query);
        return newInstance(SearchFragment.class, args);
    }

    @Override
    protected ItemListAdapter getAdapter() {
        return new SearchArrayAdapter(getActivity());
    }


    protected class SearchArrayAdapter extends ItemListAdapter<Work> {


        public SearchArrayAdapter(Context context) {
            super(R.layout.search_item);
            ((SearchParser) lister).setQuery(getArguments().getString(ARG_SEARCH_QUERY));
        }

        @Override
        public void onClick(View view, int position) {
            String link = getItems().get(position).getFullLink();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.search_item_autor);
            TextView titleTextView = holder.getView(R.id.search_item_title);
            TextView subtitleTextView = holder.getView(R.id.search_item_subtitle);
            Work work = getItems().get(position);
            authorTextView.setText(work.getAuthor().getFullName());
            titleTextView.setText("«" + work.getTitle() + "»");
            List<String> subtitle = new ArrayList<>();
            if (work.getType() != Type.OTHER) {
                subtitle.add(getString(R.string.item_form_label));
                subtitle.add(work.getType().toString());
            }
            if (work.getGenres() != null) {
                subtitle.add(getString(R.string.item_genres_label));
                subtitle.add(work.printGenres());
            }
            subtitle.add(work.getSize().toString() + "k");
            HtmlSpanner spanner = new HtmlSpanner();
            subtitleTextView.setText(spanner.fromHtml(TextUtils.join(" ", subtitle) + "\n\"" + work.getDescription() + "\""));
        }
    }
}
