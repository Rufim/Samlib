package ru.samlib.client.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.GenreParser;
import ru.samlib.client.parser.RateParser;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 07.01.2015.
 */
public class GenreFragment extends ListFragment<Work> {

    public static GenreFragment newInstance() {
        return newInstance(GenreFragment.class);
    }

    @Override
    protected DataSource<Work> getDataSource() throws Exception {
        GenreParser genreParser = new GenreParser((Genre) getArguments().getSerializable(Constants.ArgsName.Type));
        pageSize = genreParser.getPageSize();
        return genreParser;
    }

    @Override
    protected ItemListAdapter getAdapter() {
        return new GenreArrayAdapter();
    }


    protected class GenreArrayAdapter extends ItemListAdapter<Work> {


        public GenreArrayAdapter() {
            super(R.layout.item_genre);
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
            TextView authorTextView = holder.getView(R.id.genre_item_autor);
            TextView titleTextView = holder.getView(R.id.genre_item_title);
            TextView subtitleTextView = holder.getView(R.id.genre_item_subtitle);
            Work work = getItems().get(position);
            authorTextView.setText(work.getAuthor().getFullName());
            titleTextView.setText("«" + work.getTitle() + "»");
            List<String> subtitle = new ArrayList<>();
            subtitle.add(getString(R.string.item_form_label));
            subtitle.add(work.getType().getTitle());
            if(work.getSize() != null) {
                subtitle.add(work.getSize().toString() + "k");
            }
            subtitleTextView.setText(TextUtils.join(" ", subtitle));
        }
    }

}
