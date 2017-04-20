package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.kazantsev.template.fragments.ListFragment;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.domain.entity.Author;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.TopAuthorsParser;

/**
 * Created by Rufim on 16.01.2015.
 */
public class TopAuthorsFragment extends ListFragment<Author> {


    public static TopAuthorsFragment newInstance() {
        return newInstance(TopAuthorsFragment.class);
    }

    public TopAuthorsFragment() {
        enableSearch = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_top);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Author> getDataSource() throws Exception {
        TopAuthorsParser parser = new TopAuthorsParser();
        pageSize = parser.getPageSize();
        return parser;
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
        public void onClick(View view, int position) {
            String link = getItems().get(position).getFullLink();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
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
