package ru.samlib.client.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.greenrobot.event.EventBus;
import ru.samlib.client.R;
import ru.samlib.client.activity.BaseActivity;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.FragmentAttachedEvent;
import ru.samlib.client.util.FragmentBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A placeholder fragment containing a simple view.
 */
public class BaseFragment extends Fragment implements BaseActivity.BackCallback {

    private static BaseFragment lastFragment;
    private Bundle argsCache;

    private static final Map<String, Stack<Bundle>> argsStack = new HashMap<>();

    protected static boolean isStackInstance = false;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static <F extends BaseFragment> F newInstance(Class<F> fragmentClass, Bundle args) {
        return FragmentBuilder.newInstance(fragmentClass, args);
    }

    public static <F extends BaseFragment> F newInstance(Class<F> fragmentClass) {
        return FragmentBuilder.newInstance(fragmentClass);
    }

    public BaseFragment() {
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        postEvent(new FragmentAttachedEvent(this));
    }

    public boolean allowBackPress() {
        if(getFragmentManager().getBackStackEntryCount() > 0) {
            isStackInstance = true;
        }
        return true;
    }


    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

}
