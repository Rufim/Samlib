package ru.samlib.client.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.*;
import android.widget.TextView;
import com.annimon.stream.Stream;
import org.acra.ACRA;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.LazyItemListAdapter;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.entity.ExternalWork;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by 0shad on 17.05.2017.
 */
public class ExternalWorksFragment extends ListFragment<ExternalWork> {

    @Inject
    DatabaseService databaseService;
    
    List<ExternalWork> toActionWorks = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_external_works);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_external_works);
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.external_works, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_external_work_cancel:
                toActionWorks.clear();
                refreshData(false);
                return true;
            case R.id.action_external_work_delete:
                for (ExternalWork toActionWork : toActionWorks) {
                    new File(toActionWork.getFilePath()).delete();
                }
                databaseService.deleteExternalWorks(toActionWorks);
                toActionWorks.clear();
                refreshData(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        App.getInstance().getComponent().inject(this);
        super.onCreate(savedInstanceState);
        safeInvalidateOptionsMenu();
    }


    @Override
    protected DataSource<ExternalWork> newDataSource() throws Exception {
        return (skip, size) -> databaseService.selectExternalWorks(skip, size);
    }

    @Override
    protected ItemListAdapter<ExternalWork> newAdapter() {
        return new ExternalWorksAdapter();
    }

    @Override
    public void onDataTaskException(Throwable ex) {
        stopLoading();
        ErrorFragment.show(this, R.string.error, ex);
        ACRA.getErrorReporter().handleException(ex);
    }


    private class ExternalWorksAdapter extends LazyItemListAdapter<ExternalWork> {

        public ExternalWorksAdapter() {
            super(R.layout.item_external_work);
        }

        @Override
        public boolean onClick(View view, @Nullable ExternalWork item) {
            if(item != null && item.isExist()) {
                int id = view.getId();
                switch (id) {
                    case R.id.external_item_work:
                    case R.id.external_item_work_layout:
                        SectionActivity.launchActivity(getContext(), "file://" + item.getFilePath());
                        return true;
                    case R.id.external_item_author:
                    case R.id.external_item_author_layout:
                        SectionActivity.launchActivity(getContext(), item.getAuthorUrl());
                        return true;
                }

            }
            return true;
        }

        @Override
        public boolean onLongClick(View view, @Nullable ExternalWork item) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if (!toActionWorks.contains(item)) {
                toActionWorks.add(item);
                holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                toActionWorks.remove(item);
                if (!item.isExist()) {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent_light_grey));
                } else {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }
            return true;
        }

        @Override
        public void onBindHolder(ViewHolder holder, @Nullable ExternalWork item) {
            ViewGroup root = (ViewGroup) holder.getItemView();
            if(toActionWorks.contains(item)) {
                root.setBackgroundColor(getResources().getColor(R.color.Orange));
            } else {
                if (!item.isExist()) {
                    root.setBackgroundColor(getResources().getColor(R.color.transparent_light_grey));
                } else {
                    root.setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }
            TextView filepathView = GuiUtils.getView(root, R.id.external_item_filepath);
            GuiUtils.setText(filepathView, item.getFilePath());
            /*if(item.getWorkUrl() != null) {
                GuiUtils.setVisibility(View.VISIBLE, root, R.id.external_item_author_layout);
                TextView titleView = GuiUtils.getView(root, R.id.external_item_work);
                TextView authorView = GuiUtils.getView(root, R.id.external_item_author);
                GuiUtils.setText(titleView, item.getWorkTitle());
                GuiUtils.setText(authorView, item.getAuthorShortName());
            } else*/
            {
                GuiUtils.setVisibility(View.GONE, root, R.id.external_item_author_layout);
                TextView titleView = GuiUtils.getView(root, R.id.external_item_work);
                TextView authorView = GuiUtils.getView(root, R.id.external_item_author);
                GuiUtils.setText(titleView, item.getWorkTitle());
                GuiUtils.setText(authorView, item.getAuthorShortName());
            }
        }
    }
}
