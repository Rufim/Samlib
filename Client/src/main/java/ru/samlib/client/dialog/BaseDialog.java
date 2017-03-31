package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.util.FragmentBuilder;

/**
 * Created by Dmitry on 20.10.2015.
 */
public  class BaseDialog extends DialogFragment implements DialogInterface.OnClickListener {


    private BaseDialog loginDialog;

    public static <F extends BaseDialog> F newInstance(Class<F> fragmentClass, Bundle args) {
        return FragmentBuilder.newInstance(fragmentClass, args);
    }

    public static <F extends BaseDialog> F newInstance(Class<F> fragmentClass) {
        return FragmentBuilder.newInstance(fragmentClass);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void onClick(DialogInterface dialog, int which) {
        int i = 0;
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                onButtonPositive(dialog);
                break;
            case Dialog.BUTTON_NEGATIVE:
                onButtonNegative(dialog);
                break;
            case Dialog.BUTTON_NEUTRAL:
                onButtonNeutral(dialog);
                break;
        }

    }

    public void onButtonPositive(DialogInterface dialog) {

    }

    public void onButtonNegative(DialogInterface dialog) {

    }

    public void onButtonNeutral(DialogInterface dialog) {

    }

    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

}
