package ru.samlib.client.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.activity.BaseActivity;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.FragmentAttachedEvent;
import ru.samlib.client.util.FragmentBuilder;

/**
 * A placeholder fragment containing a simple view.
 */
public class BaseFragment extends Fragment implements BaseActivity.BackCallback {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static <F extends Fragment> F newInstance(Class<F> fragmentClass, Bundle args) {
        return FragmentBuilder.newInstance(fragmentClass, args);
    }

    public static <F extends Fragment> F newInstance(Class<F> fragmentClass) {
        return FragmentBuilder.newInstance(fragmentClass);
    }

    protected static <F extends Fragment> F show(FragmentBuilder builder, @IdRes int container, Class<F> fragmentClass, String key, Object obj) {
        return builder.putArg(key, obj).newFragment().replaceFragment(container, fragmentClass);
    }

    protected static <F extends Fragment> F show(FragmentManager manager, @IdRes int container, Class<F> fragmentClass, String key, Object obj) {
        return new FragmentBuilder(manager).newFragment().putArg(key, obj).replaceFragment(container, fragmentClass);
    }

    protected static <F extends Fragment> F show(Fragment fragment, Class<F> fragmentClass, String key, Object obj) {
        return new FragmentBuilder(fragment.getFragmentManager()).newFragment().addToBackStack().putArg(key, obj).replaceFragment(fragment, fragmentClass);
    }

    protected boolean retainInstance = true;

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
        setRetainInstance(retainInstance);
    }


    public EntityDataStore<Persistable>  getDataStore() {
         return App.getInstance().getDataStore();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        String title = getArguments().getString(Constants.ArgsName.TITLE);
        if(title != null) {
            getActivity().setTitle(title);
        }
        super.onViewCreated(view, savedInstanceState);
    }

    public BaseFragment show(FragmentManager manager, @IdRes int container, String key, Object obj) {
        return show(manager, container, this.getClass(), key, obj);
    }

    public int getContainerId() {
        return ((ViewGroup) getView().getParent()).getId();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        postEvent(new FragmentAttachedEvent(this));
    }

    public boolean allowBackPress() {
       return true;
    }


    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
