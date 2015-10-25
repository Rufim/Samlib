package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.dialog.FilterDialog;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CategorySelectedEvent;
import ru.samlib.client.domain.events.FilterEvent;
import ru.samlib.client.parser.NewestParser;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Rufim on 04.01.2015.
 */
public class NewestFragment extends ListFragment {

    public NewestFragment() {
        super(new NewestParser());
        enableFiltering = true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.newest, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_newest_filter:
                FilterDialog dialog = (FilterDialog) getFragmentManager().findFragmentByTag(FilterDialog.class.getSimpleName());
                if(dialog == null) {
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

    public void onEvent(FilterEvent event) {
        if(event.genres == null) {
            adapter.exitFilteringMode();
        } else {
            String query = searchView.getQuery().toString();
            if (query != null && !query.isEmpty()) {
                event.query = query;
            }
            adapter.enterFilteringMode();
            adapter.filter(event);
        }
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NewestFragment newInstance() {
        return newInstance(NewestFragment.class);
    }

    @Override
    protected ItemListAdapter getAdapter() {
        return new NewestArrayAdapter();
    }

    protected class NewestArrayAdapter extends ItemListAdapter<Work> {

        private final Locale currentLocale = getResources().getConfiguration().locale;

        public NewestArrayAdapter() {
            super(R.layout.item_newest);
        }

        @Override
        public void onClick(View view, int position) {
            int id = view.getId();
            String link = null;
            switch (id) {
                case R.id.newest_item_work:
                case R.id.newest_item_work_layout:
                    link = getItems().get(position).getFullLink();
                    break;
                case R.id.newest_item_author:
                case R.id.newest_item_author_layout:
                    link = getItems().get(position).getAuthor().getFullLink(); //Link.getBaseDomain() +  "/p/plotnikow_sergej_aleksandrowich/"; //"/t/tagern/"; //
                    break;
            }
            if (link != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(link));
                startActivity(i);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView workTextView = holder.getView(R.id.newest_item_work);
            TextView authorTextView = holder.getView(R.id.newest_item_author);
            TextView timeTextView = holder.getView(R.id.newest_item_time);
            TextView genresView = holder.getView(R.id.newest_item_genres);
            Work work = getItems().get(position);
            workTextView.setText(work.getTitle());
            genresView.setText(work.printGenres());
            authorTextView.setText(work.getAuthor().getShortName());
            timeTextView.setText(work.getShortFormattedDate(work.getUpdateDate(), currentLocale));
        }
    }

}
