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
import ru.samlib.client.domain.entity.Discussion;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.FilterEvent;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.DiscussionParser;
import ru.samlib.client.util.TextUtils;

import java.util.Locale;

/**
 * Created by Rufim on 04.01.2015.
 */
public class DiscussionFragment extends ListFragment<Discussion> {

    public DiscussionFragment() {
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
            filter(event);
        }
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DiscussionFragment newInstance() {
        return newInstance(DiscussionFragment.class);
    }

    @Override
    protected ItemListAdapter getAdapter() {
        return new NewestArrayAdapter();
    }

    @Override
    protected DataSource<Discussion> getDataSource() throws Exception {
        pageSize = 199;
        return new DiscussionParser();
    }

    protected class NewestArrayAdapter extends ItemListAdapter<Discussion> {

        private final Locale currentLocale = getResources().getConfiguration().locale;

        public NewestArrayAdapter() {
            super(R.layout.item_discussion);
        }

        @Override
        public void onClick(View view, int position) {
            int id = view.getId();
            String link = null;
            switch (id) {
                case R.id.discussion_item_work:
                case R.id.discussion_item_work_layout:
                    link = getItems().get(position).getWork().getCommentsLink().getFullLink();
                    break;
                case R.id.discussion_item_author:
                case R.id.discussion_item_author_layout:
                    link = getItems().get(position).getAuthor().getFullLink();
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
            TextView workTextView = holder.getView(R.id.discussion_item_work);
            TextView authorTextView = holder.getView(R.id.discussion_item_author);
            TextView timeTextView = holder.getView(R.id.discussion_item_time);
            TextView countTextView = holder.getView(R.id.discussion_item_count);
            TextView genresView = holder.getView(R.id.discussion_item_genres);
            Discussion discussion = getItems().get(position);
            Work work = discussion.getWork();
            workTextView.setText(work.getTitle());
            genresView.setText(work.printGenres());
            authorTextView.setText(work.getAuthor().getShortName());
            timeTextView.setText(TextUtils.getShortFormattedDate(discussion.getLastOne(), currentLocale));
            countTextView.setText(discussion.getCount() + (discussion.getCountOfDay() != null ? "/" + discussion.getCountOfDay() : ""));
        }
    }

}
