package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.util.PicassoTransformImage;
import ru.samlib.client.util.SystemUtils;

import java.lang.reflect.Field;

/**
 * Created by Dmitry on 26.10.2015.
 */
public class IllustrationFragment extends BaseFragment {

    @BindView(R.id.load_progress)
    ProgressBar progressBar;
    @BindView(R.id.loading_text)
    TextView loadingText;
    @BindView(R.id.illustration)
    ImageView illustration;
    Image image;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        retainInstance = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_illustration, container, false);
        bind(rootView);
        image = (Image) getArguments().getSerializable(Constants.ArgsName.IMAGE);
        float density = getResources().getDisplayMetrics().density;
        int maxWidth = getResources().getDisplayMetrics().widthPixels;
        Picasso.with(getActivity())
                .load(image.getFullLink())
                .transform(new PicassoTransformImage(image.getWidth(), image.getHeight(), maxWidth, density))
                .into(illustration, new ImageCallback(progressBar, loadingText, illustration) {
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

    @Override
    public void onDetach() {
        super.onDetach();

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
