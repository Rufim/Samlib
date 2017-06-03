package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.view.View;
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
    @BindView(R.id.settings_dialog_text)
    TextInputEditText input;
    SettingsFragment.Preference preference;
    OnPreferenceCommit onPreferenceCommit;

    public void setPreference(SettingsFragment.Preference preference) {
        this.preference = preference;
    }

    public void setOnPreferenceCommit(OnPreferenceCommit onPreferenceCommit) {
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

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
        String value = input.getText().toString();
        editor.putString(preference.key, value);
        editor.commit();
        if (onPreferenceCommit != null) {
            onPreferenceCommit.onCommit(value);
        }
    }

}
