package ru.samlib.client.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
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
            super(R.layout.newest_item);
        }

        @Override
        public void onClick(View view, int position) {
            int id = view.getId();
            String link = null;
            switch (id) {
                case R.id.newest_item_work:
                    link = getItems().get(position).getFullLink();
                    break;
                case R.id.newest_item_author:
                    link = getItems().get(position).getAuthor().getFullLink(); //Link.getBaseDomain() +  "/p/plotnikow_sergej_aleksandrowich/"; //"/t/tagern/"; //
                    break;
            }
            if(link != null) {
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
