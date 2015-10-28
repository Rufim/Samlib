package ru.samlib.client.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.nd.android.sdp.im.common.widget.htmlview.view.RequestHandler;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;

/**
 * Created by 0shad on 13.07.2015.
 */
public class ErrorFragment extends BaseFragment {

    private static final String TAG = ErrorFragment.class.getSimpleName();


    @Bind(R.id.error_image)
    ImageView errorImage;
    @Bind(R.id.error_text)
    TextView errorMessage;
    @Bind(R.id.refresh)
    SwipeRefreshLayout swipeRefresh;
    Class<BaseFragment> fragmentClass;
    Bundle fragmentArgs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_error, container, false);
        ButterKnife.bind(this, rootView);
        fragmentClass = (Class<BaseFragment>) getArguments().getSerializable(Constants.ArgsName.FRAGMENT_CLASS);
        fragmentArgs = getArguments().getParcelable(Constants.ArgsName.FRAGMENT_ARGS);
        int icon_id = getArguments().getInt(Constants.ArgsName.RESOURCE_ID, R.drawable.ic_action_report_problem);
        errorImage.setImageResource(icon_id);
        swipeRefresh.setOnRefreshListener(() -> {
            getFragmentManager().executePendingTransactions();
            new FragmentBuilder(getFragmentManager())
                    .putArgs(fragmentArgs)
                    .replaceFragment(getId(), fragmentClass);
        });
        String message = getArguments().getString(Constants.ArgsName.MESSAGE);
        errorMessage.setText(message);
        return rootView;
    }

    public static void show(BaseFragment fragment, @StringRes int message, @DrawableRes int icon_id) {
        new FragmentBuilder(fragment.getFragmentManager())
                .putArg(Constants.ArgsName.MESSAGE, fragment.getString(message))
                .putArg(Constants.ArgsName.FRAGMENT_CLASS, fragment.getClass())
                .putArg(Constants.ArgsName.FRAGMENT_ARGS, fragment.getArguments())
                .putArg(Constants.ArgsName.RESOURCE_ID, icon_id)
                .replaceFragment(fragment, ErrorFragment.class);
    }

    public static void show(BaseFragment fragment, @StringRes int message) {
        new FragmentBuilder(fragment.getFragmentManager())
                .putArg(Constants.ArgsName.MESSAGE, fragment.getString(message))
                .putArg(Constants.ArgsName.FRAGMENT_CLASS, fragment.getClass())
                .putArg(Constants.ArgsName.FRAGMENT_ARGS, fragment.getArguments())
                .replaceFragment(fragment, ErrorFragment.class);
    }
}
