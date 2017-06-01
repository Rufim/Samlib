package ru.samlib.client.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.annimon.stream.Stream;
import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.dialog.EditListPreferenceDialog;
import ru.samlib.client.dialog.EditTextPreferenceDialog;
import ru.samlib.client.dialog.OnPreferenceCommit;
import ru.samlib.client.util.SamlibUtils;
import ru.samlib.client.util.TTSPlayer;

import java.util.*;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Admin on 30.05.2017.
 */
public class SettingsFragment extends ListFragment<SettingsFragment.Preference> {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getBaseActivity().setTitle(R.string.drawer_settings);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_settings);
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    protected DataSource newDataSource() throws Exception {
        return (skip, size) -> {
            isEnd = true;
            PreferenceGroup groupReader = new PreferenceGroup(R.string.preferenceGroupReader)
                    .addPreferenceList(R.string.preferenceFontReader, R.string.preferenceFontReaderName, 0, 0, SamlibUtils.Font.mapFonts(getContext().getAssets()), "Roboto-Regular")
                    .addPreference(R.string.preferenceColorFontReader, R.string.preferenceColorFontReaderName, 0, R.layout.item_settings_color, DialogType.COLOR, getResources().getColor(R.color.Snow))
                    .addPreference(R.string.preferenceColorBackgroundReader, R.string.preferenceColorBackgroundReaderName, 0, R.layout.item_settings_color, DialogType.COLOR, getResources().getColor(R.color.transparent))
                    .addPreferenceList(R.string.preferenceVoiceLanguage, R.string.preferenceVoiceLanguageName, 0, 0, TTSPlayer.getAvailableLanguages(getContext()), TTSPlayer.getLanguageName(new Locale("ru")));
            PreferenceGroup groupCache = new PreferenceGroup(R.string.preferenceGroupCache)
                    .addPreference(R.string.preferenceMaxCacheSize, R.string.preferenceMaxCacheSizeName, 0,0, DialogType.TEXT, getResources().getInteger(R.integer.preferenceMaxCacheSizeDefault));
            return Arrays.asList(groupReader, groupCache);
        };
    }

    @Override
    protected ItemListAdapter newAdaptor() {
        return new SettingsAdaptor();
    }

    public class SettingsAdaptor extends MultiItemListAdapter<Object> {

        public SettingsAdaptor() {
            super(false, R.layout.item_settings_group, R.layout.item_settings_text);
        }


        @Override
        public int getLayoutId(Object item) {
            if (item instanceof Preference) {
                Preference preference = (Preference) item;
                if (preference.layout == 0) {
                    return R.layout.item_settings_text;
                } else {
                    return preference.layout;
                }
            }
            if (item instanceof PreferenceGroup) {
                return R.layout.item_settings_group;
            }
            throw new RuntimeException("Item invalid");
        }

        @Override
        public List getSubItems(Object item) {
            return item instanceof PreferenceGroup ? ((PreferenceGroup) item).preferences : null;
        }

        @Override
        public boolean hasSubItems(Object item) {
            return item instanceof PreferenceGroup;
        }

        @Override
        public boolean onClick(View view, int position) {
            Object o = getItem(position);
            if(o instanceof Preference) {
                Preference preference = (Preference) o;
                OnPreferenceCommit onPreferenceCommit = new OnPreferenceCommit() {
                    @Override
                    public void onCommit() {
                      notifyChanged();
                    }
                };
                switch (preference.dialogType) {
                    case TEXT:
                        EditTextPreferenceDialog editText = new EditTextPreferenceDialog();
                        editText.setPreference(preference);
                        editText.setOnPreferenceCommit(onPreferenceCommit);
                        editText.show(getFragmentManager(), editText.getClass().getSimpleName());
                        break;
                    case COLOR:
                        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(getContext());
                        ColorPickerDialog colorPickerDialog = ColorPickerDialog.newBuilder()
                                .setColor(preferences.getInt(preference.key, (Integer) preference.defValue))
                                .setDialogTitle(preference.titleId)
                                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                                .setShowAlphaSlider(true)
                                .create();
                        colorPickerDialog.setColorPickerDialogListener(new ColorPickerDialogListener() {

                            @Override
                            public void onColorSelected(int dialogId, @ColorInt int color) {
                                SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                                editor.putInt(preference.key, color);
                                editor.commit();
                                notifyChanged();
                            }

                            @Override
                            public void onDialogDismissed(int dialogId) {

                            }
                        });
                        colorPickerDialog.show(getActivity().getFragmentManager(), colorPickerDialog.getClass().getSimpleName());
                        break;
                    case LIST:
                        EditListPreferenceDialog editList = new EditListPreferenceDialog();
                        editList.setPreference(preference);
                        editList.setOnPreferenceCommit(onPreferenceCommit);
                        editList.show(getFragmentManager(), editList.getClass().getSimpleName());
                        break;
                }
            }
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ViewGroup root = (ViewGroup) holder.getItemView();
            Object o = getItem(position);
            Map<String, ?> preferences = AndroidSystemUtils.getDefaultPreference(root.getContext()).getAll();
            switch (holder.getItemViewType()) {
                case R.layout.item_settings_group:
                    PreferenceGroup group = (PreferenceGroup) o;
                    GuiUtils.setText(root, R.id.settings_group_label, group.title);
                    break;
                case R.layout.item_settings_text:
                case R.layout.item_settings_color:
                    Preference preference = (Preference) o;
                    GuiUtils.setText(root, R.id.settings_title, preference.title);
                    GuiUtils.setText(root, R.id.settings_subtitle, preference.subTitle);
                    switch (holder.getItemViewType()) {
                        case R.layout.item_settings_text:
                            if (preferences.containsKey(preference.key)) {
                                if(preference.dialogType.equals(DialogType.LIST)) {
                                    Object value = preferences.get(preference.key);
                                    GuiUtils.setText(root, R.id.settings_value, Stream.of(preference.keyValue).filter(entry -> entry.getValue().equals(value)).findFirst().map(Map.Entry::getKey).orElse(""));
                                } else {
                                    GuiUtils.setText(root, R.id.settings_value, preferences.get(preference.key).toString());
                                }
                            } else {
                                GuiUtils.setText(root, R.id.settings_value, preference.defValue.toString());
                            }
                            break;
                        case R.layout.item_settings_color:
                            View colorView = root.findViewById(R.id.settings_color);
                            if (preferences.containsKey(preference.key)) {
                                colorView.setBackgroundColor((Integer) preferences.get(preference.key));
                            } else {
                                colorView.setBackgroundColor((Integer) preference.defValue);
                            }
                            break;
                    }
                    if (TextUtils.isEmpty(preference.subTitle)) {
                        GuiUtils.setVisibility(GONE, root, R.id.settings_subtitle);
                    } else {
                        GuiUtils.setVisibility(VISIBLE, root, R.id.settings_subtitle);
                    }
                    break;
            }
        }
    }

    public class PreferenceGroup {
        final String title;
        List<Preference> preferences = new ArrayList<>();


        public PreferenceGroup(String title) {
            this.title = title;
        }

        public PreferenceGroup(@StringRes int title) {
            this.title = getString(title);
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title) {
            return addPreference(idKey, title, 0, 0);
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title, @LayoutRes int layout) {
            return addPreference(idKey, title, 0, layout);
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout) {
            return addPreference(idKey, title, subtitle, layout, DialogType.TEXT, "");
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, DialogType dialogType, Object defValue) {
            preferences.add(new Preference(idKey, title, subtitle, layout, dialogType, defValue));
            return this;
        }

        public PreferenceGroup addPreferenceList(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, Map<String, ?> keyValue, Object defValue) {
            Preference preference = new Preference(idKey, title, subtitle, layout, DialogType.LIST, defValue);
            preference.keyValue = keyValue;
            preferences.add(preference);
            return this;
        }
    }

    public class Preference {
        public final int idKey;
        public final int layout;
        public final int titleId;
        public final String key;
        public String title = "";
        public String subTitle = "";
        public Object defValue;
        public final DialogType dialogType;
        public Map<String, ?> keyValue;


        public Preference(@StringRes int idKey) {
            this(idKey, 0, 0, 0, DialogType.TEXT, "");
        }

        public Preference(@StringRes int idKey, @StringRes int title) {
            this(idKey, title, 0, 0, DialogType.TEXT, "");
        }

        public Preference(@StringRes int idKey, @StringRes int title, @LayoutRes int layout) {
            this(idKey, title, 0, layout, DialogType.TEXT, "");
        }


        public Preference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, DialogType dialogType, Object defValue) {
            this.idKey = idKey;
            this.layout = layout;
            this.titleId = title;
            this.key = getString(idKey);
            this.defValue = defValue;
            this.dialogType = dialogType;
            if (title > 0)
                this.title = getString(title);
            if (subtitle > 0)
                this.subTitle = getString(subtitle);
        }
    }

    public enum DialogType {
        TEXT, COLOR, LIST
    }
}
