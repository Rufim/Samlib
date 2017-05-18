package ru.samlib.client.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.TextView;
import org.acra.ACRA;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.dialog.FilterDialog;
import ru.samlib.client.domain.entity.*;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Dmitry on 29.12.2015.
 */
public class HistoryFragment extends FilterDialogListFragment<WorkEntity> {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    @Inject
    DatabaseService databaseService;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HistoryFragment newInstance() {
        return newInstance(HistoryFragment.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_history);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected ItemListAdapter<WorkEntity> newAdapter() {
        return new HistoryAdapter();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
        safeInvalidateOptionsMenu();
    }


    @Override
    protected DataSource<WorkEntity> newDataSource() throws Exception {
        return (skip, size) -> {
            if (adapter.getItems().isEmpty()) {
                return new ArrayList<>(databaseService.getHistory(skip, size));
            } else {
                return new ArrayList<>();
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_history_clean:
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.history_clean) + "?")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            App.getInstance().getDataStore().delete(BookmarkEntity.class).where(BookmarkEntity.WORK_ID.notNull()).get().value();
                            refreshData(true);
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        });
                adb.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        ErrorFragment.show(this, R.string.error, ex);
        ACRA.getErrorReporter().handleException(ex);
    }

    protected class HistoryAdapter extends ItemListAdapter<WorkEntity> {

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
            SectionActivity.launchActivity(getContext(), link);
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
