package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.adapter.MultiItemListAdapter;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                    .addPreference(R.string.preferenceFontReader, R.string.preferenceFontReaderName)
                    .addPreference(R.string.preferenceColorFontReader, R.string.preferenceColorFontReaderName)
                    .addPreference(R.string.preferenceColorBackgroundReader, R.string.preferenceColorBackgroundReaderName)
                    .addPreference(R.string.preferenceVoiceLanguage, R.string.preferenceVoiceLanguageName);
            PreferenceGroup groupCache = new PreferenceGroup(R.string.preferenceGroupCache)
                    .addPreference(R.string.preferenceMaxCacheSize, R.string.preferenceMaxCacheSizeName);
            return Arrays.asList(groupReader, groupCache);
        };
    }

    @Override
    protected ItemListAdapter newAdaptor() {
        return new SettingsAdaptor();
    }

    public static class SettingsAdaptor extends MultiItemListAdapter<Object> {

        public SettingsAdaptor() {
            super(false, R.layout.item_settings_group, R.layout.item_settings_text);
        }


        @Override
        public int getLayoutId(Object item) {
            if(item instanceof Preference) {
                return R.layout.item_settings_text;
            }
            if(item instanceof PreferenceGroup) {
                return R.layout.item_settings_group;
            }
            throw new RuntimeException("Item invalid");
        }

        @Override
        public List getSubItems(Object item) {
            return item instanceof PreferenceGroup ? ((PreferenceGroup) item).preferences: null;
        }

        @Override
        public boolean hasSubItems(Object item) {
            return item instanceof PreferenceGroup;
        }

        @Override
        public boolean onClick(View view, int position) {
            return true;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ViewGroup root = (ViewGroup) holder.getItemView();
            Object o = getItem(position);
            switch (holder.getItemViewType()) {
                case R.layout.item_settings_group:
                    PreferenceGroup group = (PreferenceGroup) o;
                    GuiUtils.setText(root, R.id.settings_group_label, group.title);
                    break;
                case R.layout.item_settings_text:
                    Preference preference = (Preference) o;
                    GuiUtils.setText(root, R.id.settings_title, preference.title);
                    GuiUtils.setText(root, R.id.settings_subtitle, preference.subTitle);
                    if(TextUtils.isEmpty(preference.subTitle)) {
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
            return addPreference(idKey, title, 0);
        }

        public PreferenceGroup addPreference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle) {
            preferences.add(new Preference(idKey, title, subtitle));
            return this;
        }
    }

    public class Preference {
        final String key;
        String title = "";
        String subTitle = "";


        public Preference(@StringRes int idKey) {
            this(idKey, 0 ,0);
        }

        public Preference(@StringRes int idKey, @StringRes int title) {
            this(idKey, title, 0);
        }

        public Preference(@StringRes int idKey, @StringRes int title, @StringRes int subtitle) {
            this.key = getString(idKey);
            if(title > 0)
            this.title = getString(title);
            if(subtitle > 0)
            this.subTitle = getString(subtitle);
        }
    }
}
