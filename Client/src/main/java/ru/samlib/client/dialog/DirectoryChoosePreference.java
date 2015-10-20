package ru.samlib.client.dialog;


import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Dmitry
 * Date: 27.02.13
 * Time: 10:59
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryChoosePreference extends DialogPreference {
    //Layout Fields                     ((?!Log).)*"\D*"
    private DirectoryChooserDialog chooserDialog;
    private String mText;

    //Called when addPreferencesFromResource() is called. Initializes basic paramaters
    public DirectoryChoosePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DirectoryChoosePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextPreferenceStyle);
    }

    public DirectoryChoosePreference(Context context) {
        this(context, null);
    }

    //Add views to Dialog
    @Deprecated
    @Override
    protected View onCreateDialogView() {
        return null;
    }

    protected void showDialog(Bundle state) {

        chooserDialog = new DirectoryChooserDialog(this.getContext(), mText, true);
        chooserDialog.setTitle(super.getDialogTitle());
        chooserDialog.setIcon(super.getDialogIcon());
        chooserDialog.setPositiveButton(super.getPositiveButtonText(), this);
        chooserDialog.setNegativeButton(super.getNegativeButtonText(), this);

        if (state != null) {
            chooserDialog.onRestoreInstanceState(state);
        }
        chooserDialog.setOnDismissListener(this);
        chooserDialog.show();
    }

    //persist values and disassemble views
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if(chooserDialog != null) {
            String path = chooserDialog.getPath();
            if (positiveResult && isPersistent()) {
                setText(path);
                setSummary(path);
            }

            for (File dir : chooserDialog.getCreatedNewDirectories()) {
                if (positiveResult || !path.equals(dir.getAbsolutePath())) {
                    dir.delete();
                }
            }

            chooserDialog.dismiss();
        }

        notifyChanged();
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public EditText getEditText() {
        if(chooserDialog != null) {
            return chooserDialog.getPathTextView();
        } else {
            return null;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return mText = a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.text = getText();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setText(myState.text);
    }

    private static class SavedState extends BaseSavedState {
        String text;

        public SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
