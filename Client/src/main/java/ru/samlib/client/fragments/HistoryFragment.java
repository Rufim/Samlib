package ru.samlib.client.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
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
import java.util.List;
import java.util.Locale;

/**
 * Created by Dmitry on 29.12.2015.
 */
public class HistoryFragment extends FilterDialogListFragment<Bookmark> {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    private boolean stopped = false;
    private boolean onlyLocked = false;

    List<Bookmark> toAction = new ArrayList<>();

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
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_history);
        onlyLocked = AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceHistoryMode, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected ItemListAdapter<Bookmark> newAdapter() {
        return new HistoryAdapter();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
        safeInvalidateOptionsMenu();
    }


    @Override
    public void onStart() {
        super.onStart();
        if(stopped) {
            refreshData(false);
        }
        stopped = false;
    }

    @Override
    public void onStop() {
        stopped = true;
        super.onStop();
    }


    @Override
    protected DataSource<Bookmark> newDataSource() throws Exception {
        return (skip, size) -> {
            if (adapter.getItems().isEmpty()) {
                if(!onlyLocked) {
                    return new ArrayList<>(databaseService.getHistory(skip, size));
                } else {
                    return new ArrayList<>(databaseService.getHistory(skip, size, true));
                }
            } else {
                return new ArrayList<>();
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history, menu);
        menu.findItem(R.id.action_history_only_locked).setChecked(onlyLocked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder adb;
        AlertDialog alert;
        switch (item.getItemId()) {
            case R.id.action_history_clean:
                adb = new AlertDialog.Builder(getActivity())
                        .setTitle(!toAction.isEmpty() ? getString(R.string.external_work_delete) : getString(R.string.history_clean) + "?")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if(toAction.isEmpty()) {
                                databaseService.deleteHistory();
                            } else {
                                databaseService.doAction(DatabaseService.Action.DELETE, toAction);
                            }
                            refreshData(true);
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        });
                alert = adb.create();
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && GuiUtils.getThemeColor(getContext(), ru.kazantsev.template.R.attr.colorOverlay) != 0) {
                    alert.getWindow().setBackgroundDrawableResource(ru.kazantsev.template.R.drawable.base_dialog_background);
                }
                alert.show();
                return true;
            case R.id.action_history_lock:
                adb = new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.history_lock_dialog))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if(!toAction.isEmpty()) {
                                for (Bookmark bookmark : toAction) {
                                    bookmark.setUserBookmark(!bookmark.isUserBookmark());
                                }
                                databaseService.doAction(DatabaseService.Action.UPDATE, toAction);
                            }
                            refreshData(true);
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        });
                alert = adb.create();
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && GuiUtils.getThemeColor(getContext(), ru.kazantsev.template.R.attr.colorOverlay) != 0) {
                    alert.getWindow().setBackgroundDrawableResource(ru.kazantsev.template.R.drawable.base_dialog_background);
                }
                alert.show();
                return true;
            case R.id.action_history_only_locked:
                item.setChecked(onlyLocked = !item.isChecked());
                SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                editor.putBoolean(getString(R.string.preferenceHistoryMode), onlyLocked);
                editor.apply();
                super.refreshData(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refreshData(boolean showProgress) {
        toAction.clear();
        super.refreshData(showProgress);
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        ErrorFragment.show(this, R.string.error, ex);
        ACRA.getErrorReporter().handleException(ex);
    }

    protected class HistoryAdapter extends ItemListAdapter<Bookmark> {

        private final Locale currentLocale = getResources().getConfiguration().locale;

        public HistoryAdapter() {
            super(R.layout.item_history);
            performSelectRoot = true;
        }

        @Override
        public boolean onClick(View view, int position) {
            int id = view.getId();
            String link = null;
            switch (id) {
                case R.id.history_item_work:
                case R.id.history_item_work_layout:
                    link = getItems().get(position).getWorkUrl();
                    break;
                case R.id.history_item_author:
                case R.id.history_item_author_layout:
                    link = getItems().get(position).getAuthorUrl();
                    break;
            }
            SectionActivity.launchActivity(getContext(), link);
            return true;
        }

        @Override
        public boolean onLongClick(View view, int position) {
            ViewHolder holder = (ViewHolder) view.getTag();
            Bookmark bookmark = getItems().get(position);
            if (!toAction.contains(bookmark)) {
                toAction.add(getItems().get(position));
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                toAction.remove(bookmark);
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            return true;
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView workTextView = holder.getView(R.id.history_item_work);
            TextView authorTextView = holder.getView(R.id.history_item_author);
            TextView timeTextView = holder.getView(R.id.history_item_time);
            TextView genresView = holder.getView(R.id.history_item_genres);
            ImageView lockedView  = holder.getView(R.id.history_locked);
            Bookmark bookmark = getItems().get(position);
            if(toAction.contains(bookmark)) {
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            if(bookmark.isUserBookmark() && !onlyLocked) {
                lockedView.setVisibility(View.VISIBLE);
            } else {
                lockedView.setVisibility(View.GONE);
            }
            workTextView.setText(bookmark.getWorkTitle());
            genresView.setText(bookmark.getGenres());
            authorTextView.setText(bookmark.getAuthorShortName());
            timeTextView.setText(TextUtils.getShortFormattedDate(bookmark.getSavedDate(), currentLocale));
        }
    }
}
