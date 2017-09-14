package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.events.AuthorAddEvent;
import ru.samlib.client.fragments.SettingsFragment;
import ru.samlib.client.util.SamlibUtils;

/**
 * Created by 0shad on 27.05.2017.
 */
public class EditTextPreferenceDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.settings_input_layout)
    TextInputLayout layout;
    @BindView(R.id.settings_dialog_text)
    TextInputEditText input;
    SettingsFragment.Preference preference;
    OnPreferenceCommit<EditTextPreferenceDialog> onPreferenceCommit = (v, dialog) -> true;

    public void setPreference(SettingsFragment.Preference preference) {
        this.preference = preference;
    }

    public void setOnPreferenceCommit(OnPreferenceCommit<EditTextPreferenceDialog> onPreferenceCommit) {
        this.onPreferenceCommit = onPreferenceCommit;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_text_preferemce, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        input.setText(preferences.getString(preference.key, ""));
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(preference.title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        return adb.create();
    }

    public void setError(@StringRes int error) {
        layout.setError(getString(error));
        layout.setErrorEnabled(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                    String value = input.getText().toString();
                    editor.putString(preference.key, value);
                    if (onPreferenceCommit != null) {
                        if(onPreferenceCommit.onCommit(value, EditTextPreferenceDialog.this)) {
                            editor.commit();
                            d.dismiss();
                        }
                    }
                }
            });
        }
    }

}
