package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.view.helper.DividerItemDecoration;
import ru.samlib.client.R;
import ru.samlib.client.fragments.SettingsFragment;

/**
 * Created by 0shad on 27.05.2017.
 */
public class EditListPreferenceDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.settings_dialog_list)
    RecyclerView recyclerView;
    SettingsFragment.Preference preference;
    Object selected;
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
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_list_preferemce, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        recyclerView.setAdapter(new ItemListAdapter<Object>(preference.listOptions, R.layout.item_settings_dialog) {

            @Override
            public void onClick(View view) {
                if(view.getTag() instanceof ViewHolder) {
                    ViewHolder holder = (ViewHolder) view.getTag();
                    selected = items.get(holder.getLayoutPosition());
                    notifyChanged();
                }
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                GuiUtils.setText(holder.getItemView().findViewById(R.id.settings_dialog_item_text), items.get(position).toString());
                if(items.get(position) == selected) {
                    holder.getItemView().setPressed(true);
                }
            }
        });
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));
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
        // add others of need
        if(selected instanceof Integer) {
            editor.putInt(preference.key, (Integer) selected);
        } else {
            editor.putString(preference.key, selected.toString());
        }
        editor.commit();
        if(onPreferenceCommit != null) {
            onPreferenceCommit.onCommit();
        }
    }

}
