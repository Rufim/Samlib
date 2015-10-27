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
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.util.SystemUtils;

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
    Image image;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_illustration, container, false);
        bind(rootView);
        image = (Image) getArguments().getSerializable(Constants.ArgsName.IMAGE);
        Picasso.with(getActivity()).load(image.getFullLink()).resize(image.getWidth(), image.getHeight()).into(illustration, new ImageCallback(progressBar, loadingText, illustration) {
            @Override
            public void onSuccess() {
                SystemUtils.nn(() -> {
                    illustration.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                }, illustration, progressBar, loadingText);
            }

            @Override
            public void onError() {
                ErrorFragment.show(IllustrationFragment.this, R.string.error_network);
            }
        });
        return rootView;
    }

    class ImageCallback implements Callback {

        ProgressBar progressBar;
        TextView loadingText;
        ImageView illustration;

        public ImageCallback(ProgressBar progressBar, TextView loadingText, ImageView illustration) {
            this.progressBar = progressBar;
            this.loadingText = loadingText;
            this.illustration = illustration;
        }

        @Override
        public void onSuccess() {
            if (illustration != null) {
                illustration.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);
            }
        }

        @Override
        public void onError() {
            ErrorFragment.show(IllustrationFragment.this, R.string.error_network);
        }
    }

}
