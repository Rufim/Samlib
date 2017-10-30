package ru.samlib.client.dialog;

import ru.kazantsev.template.dialog.BaseDialog;

/**
 * Created by 0shad on 01.06.2017.
 */
public interface OnCommit<V, D extends BaseDialog> {
    boolean onCommit(V value, D dialog);
}
