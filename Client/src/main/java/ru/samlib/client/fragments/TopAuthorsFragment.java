package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.acra.ACRA;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.activity.SectionActivity;
import ru.samlib.client.domain.entity.Author;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.TopAuthorsParser;

import java.io.IOException;

/**
 * Created by Rufim on 16.01.2015.
 */
public class TopAuthorsFragment extends ListFragment<Author> {


    public static TopAuthorsFragment newInstance() {
        return newInstance(TopAuthorsFragment.class);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_top);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_top);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Author> newDataSource() throws Exception {
        TopAuthorsParser parser = new TopAuthorsParser();
        pageSize = parser.getPageSize();
        return parser;
    }

    @Override
    public void onDataTaskException(Throwable ex) {
        if(ex instanceof IOException) {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new TopAuthorsAdapter();
    }

    protected class TopAuthorsAdapter extends ItemListAdapter<Author> {

        public TopAuthorsAdapter() {
            super(R.layout.item_top_authors);
        }

        @Override
        public boolean onClick(View view, int position) {
            String link = getItems().get(position).getFullLink();
            SectionActivity.launchActivity(getContext(), link);
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.top_authors_author);
            TextView viewsTextView = holder.getView(R.id.top_authors_views);
            TextView annotationTextView = holder.getView(R.id.top_authors_annotation);
            TextView sectionAnnotationTextView = holder.getView(R.id.top_authors_section_annotation);
            Author author = getItems().get(position);
            authorTextView.setText(author.getFullName());
            viewsTextView.setText(author.getViews().toString());
            annotationTextView.setText(author.getAnnotation());
            if (sectionAnnotationTextView != null) {
                if (author.getAnnotation() != null) {
                    sectionAnnotationTextView.setText(author.getSectionAnnotation());
                } else {
                   sectionAnnotationTextView.setVisibility(View.GONE);
                }
            }
        }
    }

}
