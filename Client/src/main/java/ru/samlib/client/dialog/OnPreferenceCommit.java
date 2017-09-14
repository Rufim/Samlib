package ru.samlib.client.dialog;

import ru.kazantsev.template.dialog.BaseDialog;

/**
 * Created by 0shad on 01.06.2017.
 */
public interface OnPreferenceCommit<D extends BaseDialog> {
    boolean onCommit(Object value, D dialog);
}
