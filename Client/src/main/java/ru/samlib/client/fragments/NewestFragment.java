package ru.samlib.client.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.NewestParser;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.util.SamlibUtils;

import java.util.Locale;

/**
 * Created by Rufim on 04.01.2015.
 */
public class NewestFragment extends FilterDialogListFragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NewestFragment newInstance() {
        return newInstance(NewestFragment.class);
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new NewestArrayAdapter();
    }

    @Override
    protected DataSource newDataSource() throws Exception {
        return new NewestParser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_new);
        return super.onCreateView(inflater, container, savedInstanceState);
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
            SectionActivity.launchActivity(getContext(), link);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView workTextView = holder.getView(R.id.newest_item_work);
            TextView authorTextView = holder.getView(R.id.newest_item_author);
            TextView timeTextView = holder.getView(R.id.newest_item_time);
            TextView genresView = holder.getView(R.id.newest_item_genres);
            Work work = getItems().get(position);
            workTextView.setText(SamlibUtils.generateText(getContext(), work.getTitle(), work.getSize() + "k", R.color.light_grey, 0.7f));
            genresView.setText(work.printGenres());
            authorTextView.setText(work.getAuthor().getShortName());
            timeTextView.setText(TextUtils.getShortFormattedDate(work.getUpdateDate(), currentLocale));
        }
    }

}
