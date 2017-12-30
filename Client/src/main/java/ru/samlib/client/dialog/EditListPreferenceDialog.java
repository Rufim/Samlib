package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.kazantsev.template.view.helper.DividerItemDecoration;
import ru.samlib.client.R;
import ru.samlib.client.fragments.SettingsFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by 0shad on 27.05.2017.
 */
public class EditListPreferenceDialog extends BaseDialog {

    View rootView;
    @BindView(R.id.settings_dialog_list)
    RecyclerView recyclerView;
    SettingsFragment.Preference preference;
    Object selected;
    OnCommit<Object, EditListPreferenceDialog> onCommit = (v, dialog) -> true;
    OnSetItemList setItemList = (textView, key, value) -> {
        textView.setText(key.toString());
    };


    public interface OnSetItemList {
        void setItemList(TextView textView, Object key, Object value);
    }

    public void setPreference(SettingsFragment.Preference preference) {
        this.preference = preference;
    }

    public void setOnCommit(OnCommit<Object, EditListPreferenceDialog> onCommit) {
        this.onCommit = onCommit;
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
        if (TextUtils.notEmpty(preference.key) &&  preferences.contains(preference.key)) {
            selected = getValueKey(preference, preferences.getAll().get(preference.key));
        } else {
            selected = getValueKey(preference, preference.defValue);
        }
        recyclerView.setAdapter(new ItemListAdapter<Object>(new ArrayList<>(preference.keyValue.keySet()), R.layout.item_settings_dialog) {

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
                Object key = items.get(position);
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

    public static Object getValueKey(SettingsFragment.Preference preference, Object value) {
         Iterator iterator =  preference.keyValue.entrySet().iterator();
         while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object entryValue = entry.getValue();
            if(entryValue instanceof Enum) {
                if (((Enum) entryValue).name().equals(value)) return entry.getKey();
            } else {
                if (entryValue.equals(value)) return entry.getKey();
            }
         }
         return "";
    }

    @Override
    public void onResume() {
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
                    Object value = preference.keyValue.get(selected);
                    if(value != null) {
                        SharedPreferences.Editor editor = null;
                        if(TextUtils.notEmpty(preference.key)) {
                            editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                            // add others of need
                            if (value instanceof Integer) {
                                editor.putInt(preference.key, (Integer) value);
                            } else if (value instanceof Float) {
                                editor.putFloat(preference.key, (Float) value);
                            } else if (value instanceof Enum) {
                                editor.putString(preference.key, ((Enum) value).name());
                            } else {
                                editor.putString(preference.key, value.toString());
                            }
                        }
                        if (onCommit != null) {
                            if (onCommit.onCommit(value, EditListPreferenceDialog.this)) {
                                if (TextUtils.notEmpty(preference.key)) {editor.commit();}
                                d.dismiss();
                            }
                        }
                    }
                }
            });
        }
    }

}
