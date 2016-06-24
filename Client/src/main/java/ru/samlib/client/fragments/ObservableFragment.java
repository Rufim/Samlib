package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.R;
import ru.samlib.client.App;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;
import ru.samlib.client.domain.events.AuthorParsedEvent;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.samlib.client.lister.DataSource;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by 0shad on 16.06.2016.
 */
public class ObservableFragment extends ListFragment<AuthorEntity>{


    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN);

    public static ObservableFragment newInstance() {
        return newInstance(ObservableFragment.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ObservableUpdateJob.start();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<AuthorEntity> getDataSource() throws Exception {
        return new DataSource<AuthorEntity>() {
            @Override
            public List<AuthorEntity> getItems(int skip, int size) throws IOException {
                EntityDataStore<Persistable> dataStore = getDataStore();
                return Stream.of(dataStore.select(AuthorEntity.class).get().toList()).skip(skip).limit(size).collect(Collectors.toList());
            }
        };
    }

    public void onEvent(AuthorUpdatedEvent event) {
        initializeAuthor(event.author);
    }

    private void initializeAuthor(AuthorEntity author) {
      if(author.isHasUpdates()) {
          int index = -1;
          for (int i = 0; i < adapter.getItems().size(); i++) {
              if(author.getLink().equals(adapter.getItems().get(0).getLink()))  {
                  index = 1;
              }
          }
          adapter.getItems().set(index, author);
          Handler handler = new Handler(Looper.getMainLooper());
          handler.post(new Runnable() {
              @Override
              public void run() {
                adapter.notifyDataSetChanged();
              }
          });
      }
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
