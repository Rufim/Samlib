package ru.samlib.client.dialog;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Created by Dmitry on 20.10.2015.
 */
public class FilterDialog extends AlertDialog {
    protected FilterDialog(Context context) {
        super(context);
    }

    protected FilterDialog(Context context, int theme) {
        super(context, theme);
    }

    protected FilterDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }


}
