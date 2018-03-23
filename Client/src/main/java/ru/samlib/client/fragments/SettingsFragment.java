package ru.samlib.client.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.PreferenceMaster;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.dialog.EditListPreferenceDialog;
import ru.samlib.client.dialog.EditTextPreferenceDialog;
import ru.samlib.client.dialog.OnCommit;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Font;
import ru.samlib.client.util.TTSPlayer;
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;

import java.util.*;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Admin on 30.05.2017.
 */
public class SettingsFragment extends ListFragment<SettingsFragment.Preference> {

    public static final Boolean DEF_OBSERVABLE_AUTO = true;

    private final static Float[] fontSizes = {6f, 8f, 9f, 10f, 10.5f, 11f, 11.5f, 12f, 12.5f, 13f, 13.5f, 14f, 15f, 16f, 18f, 20f, 22f, 24f};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getBaseActivity().setTitle(R.string.drawer_settings);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_settings);
        View root =  super.onCreateView(inflater, container, savedInstanceState);
        swipeRefresh.setEnabled(false);
        return root;
    }


    @Override
    protected DataSource newDataSource() throws Exception {
        return (skip, size) -> {
            isEnd = true;
            PreferenceGroup groupReader = new PreferenceGroup(R.string.preferenceGroupReader)
                    .addPreferenceList(R.string.preferenceFontReader, R.string.preferenceFontReaderName, 0, 0, Font.mapFonts(getContext().getAssets()), Font.getDefFont())
                    .addPreferenceList(R.string.preferenceFontSizeReader, R.string.preferenceFontSizeReaderName, 0, 0,  16f,  fontSizes)
                    .addPreferenceList(R.string.preferenceFontSizeComments, R.string.preferenceFontSizeCommentsName, 0, 0,  13f,  fontSizes)
                    .addPreferenceList(R.string.preferenceFontStyleReader, R.string.preferenceFontStyleReaderName, 0, 0, Font.Type.PLAIN, Font.getFontTypes(getContext(), null).keySet().toArray())
                    .addPreference(R.string.preferenceColorFontReader, R.string.preferenceColorFontReaderName, 0, R.layout.item_settings_color, DialogType.COLOR, GuiUtils.getThemeColor(getContext(), android.R.attr.textColor))
                    .addPreference(R.string.preferenceColorBackgroundReader, R.string.preferenceColorBackgroundReaderName, 0, R.layout.item_settings_color, DialogType.COLOR, getResources().getColor(R.color.transparent))
                    .addPreference(R.string.preferenceVoice, R.string.preferenceVoiceName);
            PreferenceGroup groupCache = new PreferenceGroup(R.string.preferenceGroupCache)
                    .addPreference(R.string.preferenceMaxCacheSize, R.string.preferenceMaxCacheSizeName, 0,0, DialogType.TEXT, getResources().getString(R.string.preferenceMaxCacheSizeDefault));
            PreferenceGroup themeGroup = new PreferenceGroup(R.string.preferenceGroupTheme)
                    .addPreferenceList(R.string.preferenceCurrentTheme,R.string.preferenceCurrentThemeName, 0, 0, generateThemeMap(), getActivity().getApplicationInfo().theme);
            PreferenceGroup observableGroup = new PreferenceGroup(R.string.preferenceGroupObservableName)
                    .addPreference(R.string.preferenceObservableAuto, R.string.preferenceObservableAutoName, 0, R.layout.item_settings_switch, DialogType.NONE, DEF_OBSERVABLE_AUTO)
                    .addPreference(R.string.preferenceObservableNotification, R.string.preferenceObservableNotificationName, 0, R.layout.item_settings_switch, DialogType.NONE, true);
            return Arrays.asList(groupReader, groupCache, themeGroup, observableGroup);
        };
    }

    private  Map<String, Integer> generateThemeMap() {
        Map<String, Integer> themes = new LinkedHashMap<>();
        String[] names = getResources().getStringArray(R.array.preferenceThemeNames);
        TypedArray ids = getResources().obtainTypedArray(R.array.preferenceThemeIds);
        for (int i = 0; i < names.length; i++) {
            themes.put(names[i], ids.getResourceId(i, -1));
        }
        ids.recycle();
        return themes;
    }

    @Override
    protected ItemListAdapter newAdapter() {
        return new SettingsAdaptor();
    }

    public class SettingsAdaptor extends MultiItemListAdapter<Object> {

        public SettingsAdaptor() {
            super(-1, R.layout.item_settings_group, R.layout.item_settings_text);
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
                if (preference.idKey == R.string.preferenceVoice) {
                    Intent intent = new Intent();
                    intent.setAction("com.android.settings.TTS_SETTINGS");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getBaseActivity().startActivity(intent);
                    TTSPlayer.dropAvailableLanguages();
                    return true;
                }
                switch (preference.dialogType) {
                    case TEXT:
                        EditTextPreferenceDialog editText = new EditTextPreferenceDialog();
                        editText.setPreference(preference);
                        editText.setOnCommit((value, d) -> {notifyChanged(); return true;});
                        if(preference.idKey == R.string.preferenceMaxCacheSize) {
                            editText.setOnCommit((value, dialog) -> {
                                if(TextUtils.parseInt(value.toString()) < 1) {
                                    dialog.setError(R.string.preferenceMaxCacheSizeError);
                                    return false;
                                } else {
                                    notifyChanged();
                                    return true;
                                }
                            });
                        }
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
                        editList.setOnCommit((value, d) -> {notifyChanged(); return true;});
                        if(preference.idKey == R.string.preferenceFontReader) {
                            editList.setSetItemList((textView, key, value) -> {
                                CalligraphyUtils.applyFontToTextView(getContext(), textView, Font.getFontPath(getContext(), value.toString(), Font.Type.PLAIN));
                                textView.setText(key.toString());
                            });
                            editList.setOnCommit((Object val, EditListPreferenceDialog dialog) -> {
                                SharedPreferences.Editor editor = AndroidSystemUtils.getDefaultPreference(getContext()).edit();
                                Font font = (Font) val;
                                Font.Type type;
                                if(font.getTypes().containsKey(Font.Type.PLAIN)) {
                                    type = Font.Type.PLAIN;
                                } else {
                                    type = font.getTypes().keySet().iterator().next();
                                }
                                editor.putString(getString(R.string.preferenceFontStyleReader), type.name());
                                editor.commit();
                                for (Object obj: getItems()) {
                                    if(obj instanceof Preference && ((Preference) obj).idKey == R.string.preferenceFontStyleReader) {
                                        ((Preference) obj).keyValue = Font.getFontTypes(getContext(), null);
                                    }
                                }
                                notifyChanged();
                                return true;
                            });
                        } else if(preference.idKey == R.string.preferenceFontSizeReader || preference.idKey == R.string.preferenceFontSizeComments) {
                            editList.setSetItemList((textView, key, value) -> {
                                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (Float) value);
                                textView.setText(key.toString());
                            });
                        } else if(preference.idKey == R.string.preferenceFontStyleReader) {
                            editList.setSetItemList((textView, key, value) -> {
                                CalligraphyUtils.applyFontToTextView(getContext(), textView, Font.getFontPath(getContext(), null, (Font.Type) value));
                                textView.setText(key.toString());
                            });
                        }  else if(preference.idKey == R.string.preferenceCurrentTheme) {
                            editList.setOnCommit(new OnCommit<Object, EditListPreferenceDialog>() {
                                @Override
                                public boolean onCommit(Object value, EditListPreferenceDialog dialog) {
                                    new Handler().postDelayed(() -> {
                                        Activity activity = getActivity();
                                        if(activity != null) {
                                            Intent intent = activity.getIntent();
                                            intent.putExtra(Constants.ArgsName.ON_CHANGE_THEME, true);
                                            activity.finish();
                                            activity.startActivity(intent);
                                        }
                                    }, 1000);
                                    return true;
                                }
                            });
                        }
                        editList.show(getFragmentManager(), editList.getClass().getSimpleName());
                        break;
                }
                return true;
            }
            return false;
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
                case R.layout.item_settings_switch:
                    Preference preference = (Preference) o;
                    preference.position = position;
                    GuiUtils.setText(root, R.id.settings_title, preference.title);
                    GuiUtils.setText(root, R.id.settings_subtitle, preference.subTitle);
                    switch (holder.getItemViewType()) {
                        case R.layout.item_settings_text:
                            if (preferences.containsKey(preference.key)) {
                                if(preference.dialogType.equals(DialogType.LIST)) {
                                    Object value = preferences.get(preference.key);
                                    GuiUtils.setText(root, R.id.settings_value, EditListPreferenceDialog.getValueKey(preference, value).toString());
                                } else {
                                    GuiUtils.setText(root, R.id.settings_value, preferences.get(preference.key).toString());
                                }
                            } else {
                                if(preference.dialogType.equals(DialogType.LIST)) {
                                    GuiUtils.setText(root, R.id.settings_value, EditListPreferenceDialog.getValueKey(preference, preference.defValue).toString());
                                } else {
                                    GuiUtils.setText(root, R.id.settings_value, preference.defValue == null ? "" : preference.defValue.toString());
                                }
                            }
                            break;
                        case R.layout.item_settings_color:
                            CardView colorView = (CardView) root.findViewById(R.id.settings_color);
                            if (preferences.containsKey(preference.key)) {
                                colorView.setCardBackgroundColor((Integer) preferences.get(preference.key));
                            } else {
                                colorView.setCardBackgroundColor(preference.defValue == null ? 0 : (Integer)  preference.defValue);
                            }
                            break;
                        case R.layout.item_settings_switch:
                            SwitchCompat switchCompat = (SwitchCompat) root.findViewById(R.id.item_settings_switch);
                            switchCompat.setTag(preference);
                            switchCompat.setChecked(AndroidSystemUtils.getStringResPreference(getContext(), preference.idKey, preference.defValue == null ? false : (Boolean)  preference.defValue));
                            if(preference.idKey == R.string.preferenceObservableNotification) {
                                switchCompat.setEnabled(AndroidSystemUtils.getStringResPreference(getContext(), R.string.preferenceObservableAuto, DEF_OBSERVABLE_AUTO));
                            }
                            switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                Preference pref  = (Preference) buttonView.getTag();
                                PreferenceMaster master = new PreferenceMaster(getContext());
                                master.putValue(pref.idKey, isChecked);
                                if(pref.idKey == R.string.preferenceObservableAuto) {
                                    for (Object o1 : getItems()) {
                                        if(o1 instanceof Preference) {
                                            Preference p = (Preference) o1;
                                            if(p.idKey == R.string.preferenceObservableNotification) {
                                                if(!isChecked) {
                                                    master.putValue(R.string.preferenceObservableNotification, false);
                                                }
                                                notifyItemChanged(p.position);
                                                break;
                                            }
                                        }
                                    }
                                }
                            });
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
            return addPreference(idKey, title, subtitle, layout, DialogType.NONE, "");
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, DialogType dialogType, Object defValue) {
            preferences.add(new Preference(getContext(),idKey, title, subtitle, layout, dialogType, defValue));
            return this;
        }

        public <T> PreferenceGroup addPreferenceList(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout,  T defValue, T [] values) {
            Map<String, T> keyValue = new LinkedHashMap<>();
            if(values != null)
            for (T value : values) {
                keyValue.put(value.toString(), value);
            }
            return addPreferenceList(idKey, title, subtitle, layout, keyValue, defValue);
        }

        public <T> PreferenceGroup  addPreferenceList(@StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, Map<String, T> keyValue, T defValue) {
            Preference preference = new Preference(getContext(), idKey, title, subtitle, layout, DialogType.LIST, defValue);
            preference.keyValue = keyValue;
            preferences.add(preference);
            return this;
        }
    }

    public static class Preference<T> {
        public final int idKey;
        public final int layout;
        public final int titleId;
        public final String key;
        public String title = "";
        public String subTitle = "";
        public T defValue;
        public final DialogType dialogType;
        public Map<Object, T> keyValue;
        public int position = -1;


        public Preference(Context context, @StringRes int idKey, T defValue) {
            this(context,idKey, 0, 0, 0, DialogType.TEXT, defValue);
        }

        public Preference(Context context, @StringRes int idKey, @StringRes int title, T defValue) {
            this(context, idKey, title, 0, 0, DialogType.TEXT, defValue);
        }

        public Preference(Context context, @StringRes int idKey, @StringRes int title, @LayoutRes int layout, T defValue) {
            this(context, idKey, title, 0, layout, DialogType.TEXT, defValue);
        }


        public Preference(Context context, @StringRes int idKey, @StringRes int title, @StringRes int subtitle, @LayoutRes int layout, DialogType dialogType, T defValue) {
            this.idKey = idKey;
            this.layout = layout;
            this.titleId = title;
            if (idKey > 0)  {
                this.key = context.getString(idKey);
            } else {
                this.key = "";
            }
            this.defValue = defValue;
            this.dialogType = dialogType;
            if (title > 0)
                this.title = context.getString(title);
            if (subtitle > 0)
                this.subTitle = context.getString(subtitle);
        }
    }

    public enum DialogType {
        TEXT, COLOR, LIST, NONE
    }
}
