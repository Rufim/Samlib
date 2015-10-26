package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;

/**
 * Created by Dmitry on 26.10.2015.
 */
public class IllustrationFragment extends BaseFragment {

    @Bind(R.id.load_progress)
    ProgressBar progressBar;
    @Bind(R.id.loading_text)
    TextView loadingText;
    @Bind(R.id.illustration)
    ImageView illustration;
    String imageLink = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_illustration, container, false);
        bind(rootView);
        imageLink = getArguments().getString(Constants.ArgsName.LINK);
        Picasso.with(getActivity()).load(imageLink).into(illustration, new Callback() {
            @Override
            public void onSuccess() {
                illustration.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);
            }

            @Override
            public void onError() {
                ErrorFragment.show(IllustrationFragment.this, R.string.error_network);
            }
        });
        return rootView;
    }

    public static void show(FragmentManager manager, @IdRes int container, String link) {
        show(manager, container, IllustrationFragment.class, Constants.ArgsName.LINK, link);
    }
}
