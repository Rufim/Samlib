package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.acra.ACRA;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.LazyItemListAdapter;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.entity.ExternalWork;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * Created by 0shad on 17.05.2017.
 */
public class ExternalWorksFragment extends ListFragment<ExternalWork> {

    @Inject
    DatabaseService databaseService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_external_works);
        return super.onCreateView(inflater, container, savedInstanceState);
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
    protected void onDataTaskException(Exception ex) {
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
                        SectionActivity.launchActivity(getContext(), item.getWork().getAuthor().getFullLink());
                        return true;
                }

            }
            return false;
        }

        @Override
        public void onBindHolder(ViewHolder holder, @Nullable ExternalWork item) {
            ViewGroup root = (ViewGroup) holder.getItemView();
            if(!item.isExist()) {
                root.setBackgroundColor(getResources().getColor(R.color.light_grey));
            }
            TextView titleView = GuiUtils.getView(root, R.id.external_item_work);
            TextView filepathView = GuiUtils.getView(root, R.id.external_item_filepath);
            TextView authorView = GuiUtils.getView(root, R.id.external_item_author);
            GuiUtils.setText(titleView, item.getWork().getTitle());
            GuiUtils.setText(filepathView, item.getFilePath());
            GuiUtils.setText(authorView, item.getWork().getAuthor().getShortName());
        }
    }
}
