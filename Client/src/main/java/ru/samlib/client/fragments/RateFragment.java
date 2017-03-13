package ru.samlib.client.fragments;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.RateParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 07.01.2015.
 */
public class RateFragment extends ListFragment<Work> {

    public static RateFragment newInstance() {
        return newInstance(RateFragment.class);
    }

    @Override
    protected DataSource<Work> getDataSource() throws Exception {
        RateParser rateParser = new RateParser();
        pageSize = rateParser.getPageSize();
        return rateParser;
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new RateArrayAdapter();
    }


    protected class RateArrayAdapter extends ItemListAdapter<Work> {


        public RateArrayAdapter() {
            super(R.layout.item_rate);
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
