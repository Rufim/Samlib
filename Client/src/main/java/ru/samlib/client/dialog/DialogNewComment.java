package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.*;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.CommentSuccessEvent;
import ru.samlib.client.parser.CommentsParser;

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

    private Work work;


    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_comments_new, null);
        ButterKnife.bind(this, rootView);
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
            CommentsParser.sendComment(work, name.getText(), email.getText(), link.getText(), comment.getText());
            postEvent(new CommentSuccessEvent());
        }
    }

}
