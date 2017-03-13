package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.snappydb.SnappydbException;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.database.SnappyHelper;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.util.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Dmitry on 29.12.2015.
 */
public class HistoryFragment extends FilterDialogListFragment {

    private static final String TAG = HistoryFragment.class.getSimpleName();


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HistoryFragment newInstance() {
        return newInstance(HistoryFragment.class);
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new HistoryAdapter();
    }

    @Override
    protected DataSource getDataSource() throws Exception {
        return (skip, size) -> {
            SnappyHelper helper = new SnappyHelper(getActivity(), TAG);
            try {
                if(adapter.getItems().isEmpty()) {
                    return Stream.of(helper.getWorks())
                            .skip(skip)
                            .limit(size)
                            .sorted((lhs, rhs) -> rhs.getCachedDate().compareTo(lhs.getCachedDate()))
                            .collect(Collectors.toList());
                } else {
                    return new ArrayList<>();
                }
            } catch (SnappydbException e) {
                Log.e(TAG, "Unknown exception", e);
                return new ArrayList<>();
            } finally {
                SnappyHelper.close(helper);
            }
        };
    }

    protected class HistoryAdapter extends ItemListAdapter<Work> {

        private final Locale currentLocale = getResources().getConfiguration().locale;

        public HistoryAdapter() {
            super(R.layout.item_history);
        }

        @Override
        public void onClick(View view, int position) {
            int id = view.getId();
            String link = null;
            switch (id) {
                case R.id.history_item_work:
                case R.id.history_item_work_layout:
                    link = getItems().get(position).getFullLink();
                    break;
                case R.id.history_item_author:
                case R.id.history_item_author_layout:
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
            TextView workTextView = holder.getView(R.id.history_item_work);
            TextView authorTextView = holder.getView(R.id.history_item_author);
            TextView timeTextView = holder.getView(R.id.history_item_time);
            TextView genresView = holder.getView(R.id.history_item_genres);
            Work work = getItems().get(position);
            workTextView.setText(work.getTitle());
            genresView.setText(work.printGenres());
            authorTextView.setText(work.getAuthor().getShortName());
            timeTextView.setText(TextUtils.getShortFormattedDate(work.getCachedDate(), currentLocale));
        }
    }
}
