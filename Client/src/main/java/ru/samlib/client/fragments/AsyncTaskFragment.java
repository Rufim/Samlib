package ru.samlib.client.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/**
 * Created by 0shad on 12.07.2015.
 */
public abstract class AsyncTaskFragment<Params, Progress, Result> extends Fragment {

    public static final String PARAMS = "params";

    private static final String TAG = AsyncTaskFragment.class.getSimpleName();
    private volatile AsyncTask task;
    private Params[] params;
    private FragmentManager manager;
    private String tag;

    public AsyncTaskFragment setParams(Params... params) {
        this.params = params;
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final AsyncTaskFragment<Params, Progress, Result> current = this;
        this.task = new AsyncTask<Params, Progress, Result>() {

            @Override
            protected void onPreExecute() {
                current.onPreExecute();
            }

            @Override
            protected void onPostExecute(Result result) {
                current.onPostExecute(result);
                cancelTask();
            }

            @Override
            protected void onProgressUpdate(Progress... values) {
                current.onProgressUpdate(values);
            }

            @Override
            protected void onCancelled(Result result) {
                current.onCancelled(result);
                cancelTask();
            }

            @Override
            protected void onCancelled() {
                current.onCancelled();
            }

            @Override
            protected Result doInBackground(Params... params) {
                return current.doInBackground(params);
            }
        };
        task.execute(params);
        return null;
    }

    private void cancelTask() {
        if (manager == null) {
            manager = getFragmentManager();
        }
        if (manager != null) {
            if (!task.isCancelled()) {
                task.cancel(true);
            }
            manager.beginTransaction().remove(this).commit();
        }
    }

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected void onCancelled(Result result) {
    }

    protected void onCancelled() {
    }

    protected abstract Result doInBackground(Params... params);

    public void execute(FragmentManager manager, String tag) {
        this.manager = manager;
        Fragment fragment = manager.findFragmentByTag(tag);
        if (fragment == null) manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        if (fragment instanceof AsyncTaskFragment) {
            AsyncTaskFragment oldAsync = (AsyncTaskFragment) fragment;
            if (!oldAsync.eq(this)) {
                oldAsync.cancelTask();
            }
        } else {
            throw new IllegalThreadStateException("Tag already used");
        }
    }

    public boolean eq(AsyncTaskFragment o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        AsyncTaskFragment<?, ?, ?> that = (AsyncTaskFragment<?, ?, ?>) o;
        if (!Arrays.equals(params, that.params)) return false;
        return !(tag != null ? !tag.equals(that.tag) : that.tag != null);
    }
}