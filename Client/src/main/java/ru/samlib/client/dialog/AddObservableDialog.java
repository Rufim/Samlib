package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.vrallev.android.cat.Cat;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.PreferenceMaster;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.events.AuthorAddEvent;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.util.SamlibUtils;

/**
 * Created by 0shad on 27.05.2017.
 */
public class AddObservableDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.observable_link_or_author)
    TextInputEditText input;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_observable_add_link_or_author, null);
        ButterKnife.bind(this, rootView);
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.observable_link_or_author_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        input.setText(new PreferenceMaster(getContext()).getValue(R.string.preferenceObservableAddLastAuthor));
        return adb.create();
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        String input = this.input.getText().toString();
        String link = null;
        if(!TextUtils.isEmpty(input)) {
            link = "/" + TextUtils.eraseHost(input);
            if (!Linkable.isAuthorLink(link)) {
                link = SamlibUtils.getLinkFromAuthorName(input);
            }
            link = link.replaceAll("/+","/");
            new PreferenceMaster(getContext()).putValue(R.string.preferenceObservableAddLastAuthor, link);
            postEvent(new AuthorAddEvent(link));
        } else {
            this.input.setError(getString(R.string.observable_add_link_or_author_error));
        }
    }


}
