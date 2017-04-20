package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.GenreParser;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Genre genre = (Genre) getArguments().getSerializable(Constants.ArgsName.Type);
        if(genre.equals(Genre.LITREVIEW)) {
            getActivity().setTitle(R.string.drawer_review);
        } else {
            getActivity().setTitle(genre.getTitle());
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected ItemListAdapter newAdapter() {
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
