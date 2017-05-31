package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.parser.RateParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 07.01.2015.
 */
public class RateFragment extends ListFragment<Work> {

    public static RateFragment newInstance() {
        return newInstance(RateFragment.class);
    }

    public RateFragment() {
        enableSearch = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.drawer_favorite);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_rate);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Work> newDataSource() throws Exception {
        RateParser rateParser = new RateParser();
        pageSize = rateParser.getPageSize();
        return rateParser;
    }

    @Override
    protected void onDataTaskException(Exception ex) {
        if(ex instanceof IOException) {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error_network, ex);
        } else {
            ErrorFragment.show(this, ru.kazantsev.template.R.string.error, ex);
            ACRA.getErrorReporter().handleException(ex);
        }
    }

    @Override
    protected ItemListAdapter newAdaptor() {
        return new RateArrayAdapter();
    }


    protected class RateArrayAdapter extends ItemListAdapter<Work> {


        public RateArrayAdapter() {
            super(R.layout.item_rate);
        }

        @Override
        public boolean onClick(View view, int position) {
            String link = getItems().get(position).getFullLink();
            SectionActivity.launchActivity(getContext(), link);
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView authorTextView = holder.getView(R.id.rate_item_autor);
            TextView titleTextView = holder.getView(R.id.rate_item_title);
            TextView subtitleTextView = holder.getView(R.id.rate_item_subtitle);
            TextView expertRateTextView = holder.getView(R.id.rate_item_rate_expert);
            TextView peopleRateTextView = holder.getView(R.id.rate_item_rate_people);
            Work work = getItems().get(position);
            authorTextView.setText(work.getAuthor().getFullName());
            titleTextView.setText("«" + work.getTitle() + "»");
            expertRateTextView.setText(work.getExpertRate().toString());
            String rate = "";
            if (work.getRate() != null) {
                rate += work.getRate() + "*" + work.getKudoed();
            }
            peopleRateTextView.setText(rate);
            List<String> subtitle = new ArrayList<>();
            subtitle.add(getString(R.string.item_genres_label));
            subtitle.add(work.printGenres());
            subtitle.add(work.getSize().toString() + "k");
            subtitleTextView.setText(TextUtils.join(" ", subtitle));
        }
    }

}
