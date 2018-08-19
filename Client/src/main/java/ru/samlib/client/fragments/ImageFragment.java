package ru.samlib.client.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Field;

import butterknife.BindView;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ErrorFragment;
import ru.kazantsev.template.util.PicassoTransformImage;
import ru.kazantsev.template.util.SystemUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Image;

/**
 * Created by Dmitry on 26.10.2015.
 */
public class ImageFragment extends BaseFragment {

    @BindView(R.id.load_progress)
    ProgressBar progressBar;
    @BindView(R.id.loading_text)
    TextView loadingText;
    @BindView(R.id.illustration)
    ImageView illustration;
    Bitmap image;

    public static ImageFragment show(BaseFragment fragment, Bitmap image) {
        return show(fragment, ImageFragment.class, Constants.ArgsName.IMAGE, image);
    }

    @Override
    public void onStart() {
        getBaseActivity().hideActionBar();
        getBaseActivity().lockDrawerClosed();
        super.onStart();
    }

    @Override
    public void onStop() {
        getBaseActivity().showActionBar();
        getBaseActivity().unlockDrawer();
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.item_illustration, container, false);
        bind(rootView);
        image =  getArguments().getParcelable(Constants.ArgsName.IMAGE);
        int maxWidth = getResources().getDisplayMetrics().widthPixels;
        if(image != null) {
            illustration.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            PicassoTransformImage transformImage = new PicassoTransformImage(image.getWidth(), image.getHeight(), maxWidth, image.hashCode() + "");
            illustration.setImageBitmap(transformImage.transform(image));
        }
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

}
