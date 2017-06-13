package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.entity.Discussion;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.DiscussionParser;
import ru.kazantsev.template.util.TextUtils;

import java.util.Locale;

/**
 * Created by Rufim on 04.01.2015.
 */
public class DiscussionFragment extends FilterDialogListFragment<Discussion> {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DiscussionFragment newInstance() {
        return newInstance(DiscussionFragment.class);
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new NewestArrayAdapter();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_discuss);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_discuss);
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    protected DataSource<Discussion> newDataSource() throws Exception {
        pageSize = 199;
        return new DiscussionParser();
    }

    protected class NewestArrayAdapter extends ItemListAdapter<Discussion> {

        private final Locale currentLocale = getResources().getConfiguration().locale;

        public NewestArrayAdapter() {
            super(R.layout.item_discussion);
        }

        @Override
        public boolean onClick(View view, int position) {
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
            SectionActivity.launchActivity(getContext(), link);
            return true;
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
