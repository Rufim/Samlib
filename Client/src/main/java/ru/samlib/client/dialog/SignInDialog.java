package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.fragments.SettingsFragment;
import ru.samlib.client.parser.SignInParser;

public class SignInDialog  extends BaseDialog {


    View rootView;
    @BindView(R.id.signIn_login_layout)
    TextInputLayout loginLayout;
    @BindView(R.id.signIn_login_text)
    TextInputEditText loginInput;
    @BindView(R.id.signIn_password_layout)
    TextInputLayout passwordLayout;
    @BindView(R.id.signIn_password_text)
    TextInputEditText passwordInput;
    @BindView(R.id.signIn_error)
    TextView error;
    SettingsFragment.Preference preference;
    SignInParser parser;
    OnCommit<String, SignInDialog> onCommit = (v, dialog) -> true;

    public SignInDialog setPreference(SettingsFragment.Preference preference) {
        this.preference = preference;
        return this;
    }

    public SignInDialog setOnCommit(OnCommit<String, SignInDialog> onCommit) {
        this.onCommit = onCommit;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_sign_in, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        parser = new SignInParser(preferences.getString(preference.key, ""));
        loginInput.setText(parser.getLogin());
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(preference.title)
                .setPositiveButton(R.string.sign_in_enter, this)
                .setNeutralButton(android.R.string.cancel, this)
                .setNegativeButton(R.string.sign_in_exit, this)
                .setView(rootView);
        return adb.create();
    }

    public void setLoginError(@StringRes int error) {
        loginLayout.setError(getString(error));
        loginLayout.setErrorEnabled(true);
    }

    public void setPasswordError(@StringRes int error) {
        loginLayout.setError(getString(error));
        loginLayout.setErrorEnabled(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    String login = loginInput.getText().toString();
                    String password = passwordInput.getText().toString();
                    if(TextUtils.isEmpty(login)) {
                        setLoginError(R.string.sign_in_login_empty);
                        return;
                    }
                    if(TextUtils.isEmpty(password)) {
                        setLoginError(R.string.sign_in_password_empty);
                        return;
                    }
                    new AsyncTask<String,Void, String>() {

                        @Override
                        protected String doInBackground(String... strings) {
                            return SignInParser.login(strings[0], strings[1]);
                        }

                        @Override
                        protected void onPostExecute(String cookie) {
                            if(TextUtils.notEmpty(cookie)) {
                                parser.enterLogin(getContext(), cookie);
                                onCommit.onCommit(cookie, SignInDialog.this);
                                d.dismiss();
                            } else {
                                error.setVisibility(View.VISIBLE);
                            }
                        }
                    }.execute(login, password);
                }
            });
            Button neutralButton = (Button) d.getButton(Dialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                }
            });
            Button negativeButton = (Button) d.getButton(Dialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    parser.eraseLogin(getContext());
                    onCommit.onCommit("", SignInDialog.this);
                    d.dismiss();
                }
            });

        }
    }

}
