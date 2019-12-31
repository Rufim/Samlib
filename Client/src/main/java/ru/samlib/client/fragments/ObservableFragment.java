package ru.samlib.client.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.YuvImage;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.annimon.stream.Stream;
import net.vrallev.android.cat.Cat;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ru.kazantsev.template.dialog.DirectoryChooserDialog;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.SystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.dialog.AddObservableDialog;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.ExternalWork;
import ru.samlib.client.domain.events.AuthorAddEvent;
import ru.samlib.client.domain.events.AuthorUpdatedEvent;
import ru.samlib.client.domain.events.ObservableCheckedEvent;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.parser.Parser;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by 0shad on 16.06.2016.
 */
public class ObservableFragment extends ListFragment<Author> {

    @Inject
    DatabaseService databaseService;
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN);

    private boolean loading;
    private Thread updateThread;
    private boolean stopped = false;

    List<Author> toAction = new ArrayList<>();

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
        setHasOptionsMenu(true);
        View root = super.onCreateView(inflater, container, savedInstanceState);
        swipeRefresh.setOnRefreshListener(() -> {
            if (!isLoading) {
                if (!isUpdateThreadActive()) {
                    loading = true;
                    Handler handler = new Handler();
                    updateThread = new Thread(() -> {
                        ObservableUpdateJob.updateObservable(databaseService, getContext());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.getItems().addAll(databaseService.getObservableAuthors(0, pageSize));
                                adapter.notifyDataSetChanged();
                            }
                        });
                    });
                    updateThread.start();
                }
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });
        return root;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.observable, menu);
        if (Parser.isCachedMode()) {
            menu.removeItem(R.id.action_observable_import);
            menu.removeItem(R.id.action_observable_add_link_or_author);
            menu.removeItem(R.id.action_observable_check_updates);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_observable_add_link_or_author:
                AddObservableDialog dialog = (AddObservableDialog) getFragmentManager().findFragmentByTag(AddObservableDialog.class.getSimpleName());
                if (dialog == null) {
                    dialog = new AddObservableDialog();
                    dialog.show(getFragmentManager(), AddObservableDialog.class.getSimpleName());
                }
                return true;
            case R.id.action_observable_import:
                if (isAdded()) {
                    getBaseActivity().doActionWithPermission(Manifest.permission.READ_EXTERNAL_STORAGE, permissionGained -> {
                        if (permissionGained) {
                            DirectoryChooserDialog chooserDialogImport = new DirectoryChooserDialog(getActivity(), DirectoryChooserDialog.NeutralButtonAction.NONE, false, false, true);
                            chooserDialogImport.setSourceDirectory(AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceLastSavedWorkPath, Environment.getExternalStorageDirectory().getAbsolutePath()));
                            chooserDialogImport.setTitle(getString(R.string.observable_import) + "...");
                            chooserDialogImport.setIcon(android.R.drawable.ic_menu_save);
                            chooserDialogImport.setAllowRootDir(false);
                            chooserDialogImport.setFileTypes("txt");
                            chooserDialogImport.setOnChooseFileListener(chosenFile -> {
                                if (chosenFile != null) {
                                    loadMoreBar.setVisibility(View.VISIBLE);
                                    new AsyncTask<File, Void, Boolean>() {

                                        @Override
                                        protected Boolean doInBackground(File... params) {
                                            String fileContent = null;
                                            try {
                                                fileContent = SystemUtils.readFile(params[0], "UTF-8");
                                            } catch (FileNotFoundException e) {
                                                Cat.e("Файл не найден!", e);
                                                return false;
                                            }
                                            for (String line : fileContent.split("\n")) {
                                                String link = TextUtils.eraseHost(line).replace("indexdate.shtml", "").replace("indextitle.shtml", "");
                                                Author author;
                                                if (Linkable.isAuthorLink(link)) {
                                                    try {
                                                        Author entity = databaseService.getAuthor(link);
                                                        if (entity == null) {
                                                            author = new AuthorParser(link).parse();
                                                            if (!TextUtils.isEmpty(author.getShortName())) {
                                                                databaseService.insertObservableAuthor(author);
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        Cat.e("Unknown exception", e);
                                                    }
                                                }
                                            }
                                            return true;
                                        }

                                        @Override
                                        protected void onPostExecute(Boolean success) {
                                            if (isAdded()) {
                                                if (success) {
                                                    refreshData(false);
                                                } else {
                                                    GuiUtils.toast(getContext(), R.string.error);
                                                }
                                            }
                                            loadMoreBar.setVisibility(View.GONE);
                                        }
                                    }.execute(chosenFile);
                                    chooserDialogImport.dismiss();
                                }
                            });
                            chooserDialogImport.show();
                        }
                    });
                }
                return true;
            case R.id.action_observable_export:
                if (isAdded()) {
                    getBaseActivity().doActionWithPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, permissionGained -> {
                        if (permissionGained) {
                            DirectoryChooserDialog chooserDialogExport = new DirectoryChooserDialog(getActivity(), DirectoryChooserDialog.NeutralButtonAction.CREATE_DIR, false);
                            chooserDialogExport.setSourceDirectory(AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceLastSavedWorkPath, Environment.getExternalStorageDirectory().getAbsolutePath()));
                            chooserDialogExport.setTitle(getString(R.string.observable_export) + "...");
                            chooserDialogExport.setIcon(android.R.drawable.ic_menu_save);
                            chooserDialogExport.setAllowRootDir(false);
                            chooserDialogExport.setFileTypes("txt");
                            chooserDialogExport.setOnChooseFileListener(chosenFile -> {
                                if (chosenFile != null) {
                                    try {
                                        File file = new File(chosenFile, "authors.txt");
                                        StringBuilder builder = new StringBuilder();
                                        for (Author authorEntity : databaseService.getObservableAuthors()) {
                                            builder.append(authorEntity.getFullLink());
                                            builder.append("\n");
                                        }
                                        SystemUtils.copy(new ByteArrayInputStream(builder.toString().getBytes()), new FileOutputStream(file));
                                    } catch (Exception e) {
                                        Cat.e("Unknown exception", e);
                                    }
                                }
                            });
                            chooserDialogExport.show();
                        }
                    });
                }
                return true;
            case R.id.action_observable_check_updates:
                refreshData(true);
                return true;
            case R.id.action_observable_delete:
                for (Author entity : toAction) {
                    entity.setObservable(false);
                }
                databaseService.deleteAuthors(toAction);
                toAction.clear();
                refreshData(false);
                return true;
            case R.id.action_observable_cancel:
                toAction.clear();
                refreshData(false);
                return true;

        }
        return false;
    }

    @Subscribe
    public void onEvent(AuthorAddEvent event) {
        loadMoreBar.setVisibility(View.VISIBLE);
        new AsyncTask<String, Void, Author>() {

            @Override
            protected Author doInBackground(String... params) {
                Author author = null;
                String link = params[0];
                try {
                    author = databaseService.getAuthor(link);
                    if (author == null) {
                        author = new AuthorParser(params[0]).parse();
                        if (!TextUtils.isEmpty(author.getShortName())) {
                            author.setParsed(true);
                            databaseService.insertObservableAuthor(author);
                            return author;
                        } else {
                            author.setParsed(false);
                        }
                    }
                } catch (Exception ex) {
                    Cat.e(ex);
                    return new Author(link);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Author author) {
                if (author == null) {
                    GuiUtils.toast(getContext(), R.string.observable_add_link_or_author_exist);
                } else if (!author.isParsed()) {
                    GuiUtils.toast(getContext(), getString(R.string.observable_add_link_or_author_error_link) + " " + author.getFullLink());
                } else {
                    refreshData(false);
                    GuiUtils.toast(getContext(), R.string.observable_add_link_or_author_added);
                }
                loadMoreBar.setVisibility(View.GONE);
            }

        }.execute(event.link);
    }

    @Override
    protected DataSource<Author> newDataSource() throws Exception {
        return (skip, size) -> databaseService.getObservableAuthors(skip, size);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (stopped) {
            refreshData(false);
        }
        stopped = false;
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        stopped = true;
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
    public void refreshData(boolean update) {
        swipeRefresh.setRefreshing(update);
        loading = update;
        final int size = update ? adapter.getItemCount() : pageSize;
        Handler handler = new Handler();
        updateThread = new Thread(() -> {
            ObservableUpdateJob.updateObservable(databaseService, getContext());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.clear();
                    adapter.getItems().addAll(databaseService.getObservableAuthors(0, size));
                    adapter.notifyDataSetChanged();
                }
            });
        });
        updateThread.start();
    }

    private boolean isUpdateThreadActive() {
        return updateThread != null && updateThread.isAlive() && !updateThread.isInterrupted();
    }

    private void initializeAuthor(Author author) {
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


    @Override
    protected ItemListAdapter newAdapter() {
        return new FavoritesAdapter();
    }

    protected class FavoritesAdapter extends ItemListAdapter<Author> {

        public FavoritesAdapter() {
            super(R.layout.item_favorites);
            performSelectRoot = true;
        }

        @Override
        public boolean onClick(View view, int position) {
            if (!loading) {
                Author authorEntity = getItems().get(position);
                if (!authorEntity.isDeleted()) {
                    Intent i = new Intent(getActivity(), SectionActivity.class);
                    Author author = new Author(authorEntity.getLink());
                    i.putExtra(Constants.ArgsName.AUTHOR, author);
                    startActivity(i);
                } else {
                    GuiUtils.toast(getContext(), R.string.author_not_exist);
                }
            }
            return true;
        }

        @Override
        public boolean onLongClick(View view, int position) {
            ViewHolder holder = (ViewHolder) view.getTag();
            Author author = getItems().get(position);
            if (!toAction.contains(author)) {
                toAction.add(getItems().get(position));
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                toAction.remove(author);
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
            }
            return true;
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.favorites_author);
            TextView lastUpdateView = holder.getView(R.id.favorites_last_update);
            TextView newText = holder.getView(R.id.favorites_update);
            TextView annotationTextView = holder.getView(R.id.favorites_annotation);
            Author author = getItems().get(position);
            if (toAction.contains(author)) {
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                if (author.isDeleted()) {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent_light_grey));
                } else {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }
            authorTextView.setText(TextUtils.isEmpty(author.getFullName()) ? author.getShortName() : author.getFullName());
            if (author.getLastUpdateDate() != null) {
                lastUpdateView.setText(dateFormat.format(author.getLastUpdateDate()));
            }
            annotationTextView.setText(author.getAnnotation());
            if (author.isHasUpdates()) {
                newText.setVisibility(View.VISIBLE);
            } else {
                newText.setVisibility(View.GONE);
            }
        }

    }


}
