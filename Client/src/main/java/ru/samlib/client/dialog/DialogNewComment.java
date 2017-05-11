package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.*;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CommentSuccessEvent;

/**
 * Created by Admin on 10.05.2017.
 */
public class DialogNewComment  extends BaseDialog {

    View rootView;
    @BindView(R.id.comments_new_text)
    EditText comment;
    @BindView(R.id.comments_new_name)
    TextInputEditText name;
    @BindView(R.id.comments_new_email)
    TextInputEditText email;
    @BindView(R.id.comments_new_link)
    TextInputEditText link;

    private String preferenceName;
    private String preferenceEmail;
    private String preferenceLink;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_comments_new, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        name.setText(preferenceName = preferences.getString(getString(R.string.preferenceCommentName), ""));
        email.setText(preferenceEmail = preferences.getString(getString(R.string.preferenceCommentEmail), ""));
        link.setText(preferenceLink = preferences.getString(getString(R.string.preferenceCommentLink), ""));
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.comments_dialog_title)
                .setPositiveButton(R.string.comments_new_send, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        return adb.create();
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        if(TextUtils.isEmpty(name.getText())) {
            name.setError(getString(R.string.comments_new_error_name));
        } else {
            SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            if(!name.getText().toString().equals(preferenceName)) {
                editor.putString(getString(R.string.preferenceCommentName), preferenceName = name.getText().toString());
            }
            if (!name.getText().toString().equals(preferenceEmail)) {
                editor.putString(getString(R.string.preferenceCommentEmail), preferenceEmail = email.getText().toString());
            }
            if (!name.getText().toString().equals(preferenceLink)) {
                editor.putString(getString(R.string.preferenceCommentLink), preferenceLink = link.getText().toString());
            }
            editor.apply();

            postEvent(new CommentSuccessEvent(preferenceName, preferenceEmail, preferenceLink, comment.getText().toString()));
        }
    }

}
