package ru.samlib.client.dialog;

import android.app.Dialog;
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
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.kazantsev.template.view.helper.DividerItemDecoration;
import ru.samlib.client.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 0shad on 27.05.2017.
 */
public class ListChooseDialog<K> extends BaseDialog {

    View rootView;
    @BindView(R.id.settings_dialog_list)
    RecyclerView recyclerView;
    LinkedHashMap<K, String> list = new LinkedHashMap<>();
    K selected;
    String title;
    OnCommit<K, ListChooseDialog> onCommit = (v, dialog) -> true;
    OnSetItemList setItemList = (textView, key,  value) -> {
        textView.setText(value);
    };

    public interface OnSetItemList<K> {
        void setItemList(TextView textView, K key, String value);
    }

    public void setValues(LinkedHashMap<K, String>  list) {
        this.list = list;
    }

    public void setSelected(K selected) {
        this.selected = selected;
    }

    public void setOnCommit(OnCommit<K, ListChooseDialog> onCommit) {
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
        recyclerView.setAdapter(new ItemListAdapter<K>(new ArrayList<>(list.keySet()), R.layout.item_settings_dialog) {

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
                K key = items.get(position);
                setItemList.setItemList(GuiUtils.getView(holder.getItemView(), R.id.settings_dialog_item_text), key, list.get(key));
                if (key.equals(selected)) {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.Orange));
                } else {
                    holder.getItemView().setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        if(!TextUtils.isEmpty(title)) {
            adb.setTitle(title);
        }
        return adb.create();
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
                    if(selected != null) {
                        if (onCommit != null) {
                            if (onCommit.onCommit(selected, ListChooseDialog.this)) {
                                d.dismiss();
                            }
                        }
                    }
                }
            });
        }
    }

}
