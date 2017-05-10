package ru.samlib.client.fragments;

import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.dialog.FilterDialog;
import ru.samlib.client.domain.entity.Gender;
import ru.samlib.client.domain.entity.Genre;
import ru.kazantsev.template.domain.event.Event;
import ru.kazantsev.template.util.TextUtils;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Created by 0shad on 14.06.2016.
 */
public abstract class FilterDialogListFragment<T> extends ListFragment<T> {


    public FilterDialogListFragment() {
        enableSearch = true;
        enableFiltering = true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filter, menu);
    }

    @Override
    protected ItemListAdapter.FilterEvent newFilterEvent(String query) {
        if(adapter != null && adapter.getLastQuery() != null) {
            adapter.getLastQuery().query = query;
            return adapter.getLastQuery();
        }
        return new FilterEvent(query);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                FilterDialog dialog = (FilterDialog) getFragmentManager().findFragmentByTag(FilterDialog.class.getSimpleName());
                if (dialog == null) {
                    dialog = new FilterDialog();
                    dialog.setState((FilterEvent) adapter.getLastQuery());
                    dialog.show(getFragmentManager(), FilterDialog.class.getSimpleName());
                }
                return true;
        }
        return false;
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
    public void onEvent(FilterEvent event) {
        if (event.isEmpty()) {
            adapter.exitFilteringMode();
        } else {
            String query = searchView.getQuery().toString();
            if (query != null) {
                event.query = query;
            }
            filter(event);
        }
    }


    public static class FilterEvent extends ItemListAdapter.FilterEvent implements Event {

        public final ArrayList<Genre> genres;
        public final boolean excluding;
        public final EnumSet<Gender> genders;

        public FilterEvent(String query, ArrayList<Genre> genres, EnumSet<Gender> genders, boolean excluding) {
            super(null);
            this.genres = genres;
            this.excluding = excluding;
            this.genders = genders;
        }

        public FilterEvent(String query, ArrayList<Genre> genres, boolean excluding) {
            super(null);
            this.genres = genres;
            this.excluding = excluding;
            this.genders = null;
        }

        public FilterEvent(String query) {
            super(query);
            this.genres = null;
            this.excluding = false;
            this.genders = null;
        }

        public boolean isEmpty() {
            return (genders == null || genders.isEmpty()) && (genres == null || genders.isEmpty()) && TextUtils.isEmpty(query);
        }
    }
}
