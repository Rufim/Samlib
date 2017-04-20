package ru.samlib.client.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import org.greenrobot.eventbus.EventBus;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.service.ObservableService;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by 0shad on 16.06.2016.
 */
public class ObservableFragment extends ListFragment<Author> {

    @Inject
    ObservableService observableService;
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN);

    private boolean loading;

    public static ObservableFragment newInstance() {
        return newInstance(ObservableFragment.class);
    }

    public ObservableFragment() {
        enableSearch = true;
        enableFiltering = true;
        enableScrollbar = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_observable);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Author> getDataSource() throws Exception {
        return new DataSource<Author>() {
            @Override
            public List<Author> getItems(int skip, int size) throws IOException {
                EntityDataStore<Persistable> dataStore = App.getInstance().getDataStore();
                return Stream.of(dataStore.select(AuthorEntity.class).get().toList()).skip(skip).limit(size).collect(Collectors.toList());
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
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

    @Subscribe
    public void onEvent(AuthorUpdatedEvent event) {
        initializeAuthor(event.author);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ObservableCheckedEvent event) {
        loading = false;
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void refreshData(boolean showProgress) {
        swipeRefresh.setRefreshing(true);
        loading = true;
        adapter.clear();
        adapter.getItems().addAll(App.getInstance().getDataStore().select(AuthorEntity.class).limit(currentCount).get().toList());
        adapter.notifyDataSetChanged();
        new Thread(() -> {
            ObservableUpdateJob.updateObservable(observableService, getContext());
        }).start();
    }

    private void initializeAuthor(Author author) {
        if (author.isHasUpdates()) {
            final int index;
            for (int i = 0; i < adapter.getItems().size(); i++) {
                if (author.getLink().equals(adapter.getItems().get(i).getLink())) {
                    index = i;
                    adapter.getItems().set(i, author);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> adapter.notifyItemChanged(index));
                    break;
                }
            }
        }
    }


    @Override
    protected ItemListAdapter newAdapter() {
        return new FavoritesAdapter();
    }

    protected class FavoritesAdapter extends ItemListAdapter<Author> {

        public FavoritesAdapter() {
            super(R.layout.item_favorites);
        }

        @Override
        public void onClick(View view, int position) {
            if (!loading) {
                Author author = getItems().get(position);
                author.setHasUpdates(false);
                App.getInstance().getDataStore().update(author.createEntry());
                adapter.notifyDataSetChanged();
                Intent i = new Intent(getActivity(), SectionActivity.class);
                author = new Author(author.getLink());
                author.setId(author.getId());
                i.putExtra(Constants.ArgsName.AUTHOR, author);
                startActivity(i);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.favorites_author);
            TextView lastUpdateView = holder.getView(R.id.favorites_last_update);
            TextView newText = holder.getView(R.id.favorites_update);
            TextView annotationTextView = holder.getView(R.id.favorites_annotation);
            Author author = getItems().get(position);
            authorTextView.setText(author.getFullName());
            lastUpdateView.setText(dateFormat.format(author.getLastUpdateDate()));
            annotationTextView.setText(author.getAnnotation());
            if (author.isHasUpdates()) {
                newText.setVisibility(View.VISIBLE);
            } else {
                newText.setVisibility(View.GONE);
            }
        }

    }


}
