package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.TopAuthorsParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 16.06.2016.
 */
public class FavoritesFragment extends ListFragment<Author>{


    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN);

    public static FavoritesFragment newInstance() {
        return newInstance(FavoritesFragment.class);
    }

    @Override
    protected DataSource<Author> getDataSource() throws Exception {
        return new DataSource<Author>() {
            @Override
            public List<Author> getItems(int skip, int size) throws IOException {
                return new ArrayList<>();
            }
        };
    }

    @Override
    protected ItemListAdapter getAdapter() {
        return new FavoritesAdapter();
    }

    protected class FavoritesAdapter extends ItemListAdapter<Author> {

        public FavoritesAdapter() {
            super(R.layout.item_favorites);
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
            TextView authorTextView = holder.getView(R.id.favorites_author);
            TextView lastUpdateView = holder.getView(R.id.favorites_last_update);
            TextView newText = holder.getView(R.id.favorites_new);
            TextView annotationTextView = holder.getView(R.id.favorites_annotation);
            Author author = getItems().get(position);
            authorTextView.setText(author.getFullName());
            lastUpdateView.setText(dateFormat.format(author.getLastUpdateDate()));
            annotationTextView.setText(author.getAnnotation());
            if(author.isHasUpdates()) {
                newText.setVisibility(View.VISIBLE);
            }
        }
    }


}
