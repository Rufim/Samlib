package ru.samlib.client.dialog;

import android.annotation.SuppressLint;
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
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.App;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.events.CommentSendEvent;
import ru.samlib.client.parser.CommentsParser;

/**
 * Created by Admin on 10.05.2017.
 */
public class NewCommentDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.comments_new_text)
    EditText comment;
    @BindView(R.id.comments_new_name)
    TextInputEditText name;
    @BindView(R.id.comments_new_email)
    TextInputEditText email;
    @BindView(R.id.comments_new_link)
    TextInputEditText link;

    private CommentsParser.Operation operation = CommentsParser.Operation.store_new;
    private Comment existing = null;
    private String text = "";
    private Integer indexPage = 0;

    private String preferenceName;
    private String preferenceEmail;
    private String preferenceLink;
    private String preferenceComment;

    public CommentsParser.Operation getOperation() {
        return operation;
    }

    public void setOperation(CommentsParser.Operation operation) {
        this.operation = operation;
    }

    public Comment getExisting() {
        return existing;
    }

    public void setExisting(Comment existing) {
        this.existing = existing;
    }

    public Integer getIndexPage() {
        return indexPage;
    }

    public void setIndexPage(Integer indexPage) {
        this.indexPage = indexPage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_comments_new, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        name.setText(preferenceName = preferences.getString(getString(R.string.preferenceCommentName), ""));
        email.setText(preferenceEmail = preferences.getString(getString(R.string.preferenceCommentEmail), ""));
        link.setText(preferenceLink = preferences.getString(getString(R.string.preferenceCommentLink), ""));
        preferenceComment = preferences.getString(getString(R.string.preferenceCommentContent), "");
        if(text != null) {
            if(operation.equals(CommentsParser.Operation.store_reply)) {
                if(preferenceComment.startsWith(text)) {
                  text = preferenceComment;
                }
                comment.setText(text + "\n");
                comment.setSelection(comment.getText().length());
            } else {
                comment.setText(TextUtils.isEmpty(text) ? preferenceComment : text);
            }
        } else {
            comment.setText(preferenceComment);
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.comments_dialog_title)
                .setPositiveButton(R.string.comments_new_send, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        return adb.create();
    }

    @Override
    public void onDestroy() {
        SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(App.getInstance()).edit();
        editor.putString(getString(R.string.preferenceCommentContent), comment.getText().toString());
        editor.apply();
        super.onDestroy();
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        if(TextUtils.isEmpty(name.getText())) {
            name.setError(getString(R.string.comments_new_error_name));
        } else if(isValidOperation()) {
            SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            if(!name.getText().toString().equals(preferenceName)) {
                editor.putString(getString(R.string.preferenceCommentName), preferenceName = name.getText().toString());
            }
            if (!email.getText().toString().equals(preferenceEmail)) {
                editor.putString(getString(R.string.preferenceCommentEmail), preferenceEmail = email.getText().toString());
            }
            if (!link.getText().toString().equals(preferenceLink)) {
                editor.putString(getString(R.string.preferenceCommentLink), preferenceLink = link.getText().toString());
            }
            if (!comment.toString().equals(preferenceComment)) {
                editor.putString(getString(R.string.preferenceCommentContent), preferenceComment = comment.getText().toString());
            }
            editor.apply();

            postEvent(new CommentSendEvent(preferenceName, preferenceEmail, preferenceLink, comment.getText().toString().replace("\n", "\r\n"), existing != null ? existing.getMsgid() : "", operation, indexPage));
        } else {
            GuiUtils.toast(getContext(), R.string.error);
        }
    }

    private boolean isValidOperation() {
        return !TextUtils.isEmpty(name.getText())
                && ((existing == null && operation.equals(CommentsParser.Operation.store_new))
                || (existing != null && (operation.equals(CommentsParser.Operation.store_reply) || operation.equals(CommentsParser.Operation.store_edit))));
    }

}
