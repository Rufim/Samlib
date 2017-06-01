package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.annimon.stream.Stream;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.kazantsev.template.view.helper.DividerItemDecoration;
import ru.samlib.client.R;
import ru.samlib.client.fragments.SettingsFragment;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by 0shad on 27.05.2017.
 */
public class EditListPreferenceDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.settings_dialog_list)
    RecyclerView recyclerView;
    SettingsFragment.Preference preference;
    String selected;
    OnPreferenceCommit onPreferenceCommit;
    OnSetItemList setItemList = (textView, key, value) -> {
        textView.setText(key);
    };


    public interface OnSetItemList {
        void setItemList(TextView textView, String key, Object value);
    }

    public void setPreference(SettingsFragment.Preference preference) {
        this.preference = preference;
    }

    public void setOnPreferenceCommit(OnPreferenceCommit onPreferenceCommit) {
        this.onPreferenceCommit = onPreferenceCommit;
    }

    public void setSetItemList(OnSetItemList setItemList) {
        this.setItemList = setItemList;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_list_preferemce, null);
        ButterKnife.bind(this, rootView);
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
        if (preferences.contains(preference.key)) {
            selected = getValueName(preference, preferences.getAll().get(preference.key));
        } else {
            selected = getValueName(preference, preference.defValue);
        }
        recyclerView.setAdapter(new ItemListAdapter<String>(new ArrayList<String>(preference.keyValue.keySet()), R.layout.item_settings_dialog) {

            @Override
            public void onClick(View view) {
                if (view.getTag() instanceof ViewHolder) {
                    ViewHolder holder = (ViewHolder) view.getTag();
                    selected = items.get(holder.getLayoutPosition());
                    notifyChanged();
                }
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                String key = items.get(position);
                setItemList.setItemList(GuiUtils.getView(holder.getItemView(), R.id.settings_dialog_item_text), key, preference.keyValue.get(key));
                if (items.get(position).equals(selected)) {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
                } else {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(preference.title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        return adb.create();
    }

    public static String getValueName(SettingsFragment.Preference preference, Object value) {
        return Stream.of((Map<String, ? extends Object>) preference.keyValue).filter(entry -> entry.getValue().equals(value)).findFirst().map(Map.Entry::getKey).orElse("");
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
        // add others of need
        Object value = preference.keyValue.get(selected);
        if (value instanceof Integer) {
            editor.putInt(preference.key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(preference.key, (Float) value);
        } else {
            editor.putString(preference.key, value.toString());
        }
        editor.commit();
        if (onPreferenceCommit != null) {
            onPreferenceCommit.onCommit();
        }
    }

}
