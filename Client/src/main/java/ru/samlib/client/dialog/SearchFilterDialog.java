package ru.samlib.client.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.SystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Gender;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.fragments.SearchFragment;
import ru.samlib.client.parser.SearchStatParser;

import java.util.*;

/**
 * Created by 0shad on 25.10.2015.
 */
public class SearchFilterDialog extends BaseDialog {


    public static class ItemAdapter {
        private Linkable value;
        private String def = "Любой";

        ItemAdapter(Linkable value) {
            this.value = value;
        }

        public ItemAdapter(String def) {
            this.value = null;
            this.def = def;
        }

        @Override
        public String toString() {
            if(value != null && value.equals(Type.ARTICLE)) {
                return "Авторские группы/Статьи";
            }
            return value == null ? def : value.getTitle();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ItemAdapter && ((ItemAdapter) obj).value != null && ((ItemAdapter) obj).value.equals(this.value));
        }

        private static  <T extends Linkable> List<ItemAdapter> createList(String def, T... array) {
            List<ItemAdapter> arrayList = new ArrayList<>();
            arrayList.add(new ItemAdapter(def));
            for (T t : array) {
                if (TextUtils.notEmpty(t.getLink())) {
                    arrayList.add(new ItemAdapter(t));
                }
            }
            return arrayList;
        }

        public static <T extends Linkable> int indexOf(Linkable value, T... array) {
            List<ItemAdapter> list = createList(null , array);
            for (int i = 0; i < list.size(); i++) {
                ItemAdapter it = list.get(i);
                if(it.value == value) return i;
            }
            return 0;
        }

        public static <T extends Linkable> ArrayAdapter<ItemAdapter> createAdapter(Context context, @StringRes int id, T ... array) {
            return new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, createList(context.getString(id),array));
        }

    }


    @BindView(R.id.dialog_filter_switch_mode)
    Switch dialogFilterSwitchMode;
    @BindView(R.id.dialog_filter_genre)
    Spinner dialogFilterGenre;
    @BindView(R.id.dialog_filter_type)
    Spinner dialogFilterType;
    @BindView(R.id.dialog_filter_male)
    CheckBox dialogFilterMale;
    @BindView(R.id.dialog_filter_female)
    CheckBox dialogFilterFemale;
    @BindView(R.id.dialog_filter_undefined)
    CheckBox dialogFilterUndefined;
    @BindView(R.id.dialog_filter_sort_activity)
    RadioButton dialogSortActivity;
    @BindView(R.id.dialog_filter_sort_rating)
    RadioButton dialogSortRating;
    @BindView(R.id.dialog_filter_sort_views)
    RadioButton dialogSortViews;
    @BindView(R.id.dialog_filter_size)
    EditText dialogSize;
    View rootView;
    Genre genre;
    EnumSet<Gender> genderSet;
    Type type;
    Integer size;
    SearchStatParser.SortWorksBy sortWorksBy;
    boolean excluding = false;
    String query;
    SearchStatParser parser;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_search_filter, null);
        ButterKnife.bind(this, rootView);
        dialogFilterGenre.setAdapter(ItemAdapter.createAdapter(getContext(), R.string.dialog_filter_an, Genre.values()));
        dialogFilterType.setAdapter(ItemAdapter.createAdapter(getContext(), R.string.dialog_filter_any, Type.values()));
        SystemUtils.forEach((button) -> {
            button.setOnCheckedChangeListener((cb, checked) -> {
                if (checked) {
                    SystemUtils.forEach((rb) -> {
                        if (rb != null && rb.isChecked() && rb != cb) {
                            rb.setChecked(false);
                        }
                    }, dialogSortActivity, dialogSortRating, dialogSortViews);
                }
            });
        }, dialogSortActivity, dialogSortRating, dialogSortViews);
        invalidateViews();
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.dialog_filter_find, this)
                .setNegativeButton(R.string.dialog_filter_apply, this)
                .setNeutralButton(R.string.dialog_filter_clear, this)
                .setView(rootView);
        return adb.create();
    }



    public void setState(SearchStatParser parser) {
        if (parser != null) {
            this.parser = parser;
            Request request = parser.getRequest();
            if (TextUtils.notEmpty(request.getParam(SearchStatParser.SearchParams.genre))) {
                this.genre = Genre.valueOf(request.getParam(SearchStatParser.SearchParams.genre));
            } else {
                this.genre = null;
            }
            if (TextUtils.notEmpty(request.getParam(SearchStatParser.SearchParams.type))) {
                this.type = Type.valueOf(request.getParam(SearchStatParser.SearchParams.type));
            } else {
                this.type = null;
            }
            if (TextUtils.notEmpty(request.getParam(SearchStatParser.SearchParams.sort))) {
                this.sortWorksBy = SearchStatParser.SortWorksBy.valueOf(request.getParam(SearchStatParser.SearchParams.sort));
            } else {
                this.sortWorksBy = SearchStatParser.SortWorksBy.ACTIVITY;
            }
            if (TextUtils.notEmpty(request.getParam(SearchStatParser.SearchParams.work_size))) {
                this.size = TextUtils.parseInt(request.getParam(SearchStatParser.SearchParams.work_size));
            } else {
                this.size = null;
            }
            this.query = request.getParam(SearchStatParser.SearchParams.query);
        }
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        saveState();
        parser.setFilters(query, genre, type, size, sortWorksBy);
        Activity activity = getActivity();
        if(activity != null && activity instanceof MainActivity) {
            Fragment fragment = ((MainActivity) activity).getCurrentFragment();
            if (fragment instanceof SearchFragment) {
                ((SearchFragment) fragment).refreshData(true);
            }
        }
    }

    @Override
    public void onButtonNeutral(DialogInterface dialog) {
        parser.getRequest().clearParams();
        parser.getRequest().initParams(SearchStatParser.SearchParams.values());
        parser.setQuery(query);
        setState(parser);
        invalidateViews();
    }

    @Override
    public void onButtonNegative(DialogInterface dialog) {
        saveState();
        parser.setFilters(query, genre, type, size, sortWorksBy);
    }

    private void saveState() {
        genre = (Genre) ((ItemAdapter) dialogFilterGenre.getSelectedItem()).value;
        type = (Type) ((ItemAdapter) dialogFilterType.getSelectedItem()).value;
        size = TextUtils.parseInt(dialogSize.getText().toString());
        if (dialogSortActivity.isChecked()) sortWorksBy = SearchStatParser.SortWorksBy.ACTIVITY;
        else if (dialogSortRating.isChecked()) sortWorksBy = SearchStatParser.SortWorksBy.RATING;
        else if (dialogSortViews.isChecked()) sortWorksBy = SearchStatParser.SortWorksBy.VIEWS;
        else sortWorksBy = null;

        excluding = dialogFilterSwitchMode.isChecked();
        if (!dialogFilterMale.isChecked()) genderSet.remove(Gender.MALE);
        else genderSet.add(Gender.MALE);

        if (!dialogFilterFemale.isChecked()) genderSet.remove(Gender.FEMALE);
        else genderSet.add(Gender.FEMALE);

        if (!dialogFilterUndefined.isChecked()) genderSet.remove(Gender.UNDEFINED);
        else genderSet.add(Gender.UNDEFINED);
    }

    @Override
    public void onResume() {
        super.onResume();
        // prevent dissmiss
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button neutral = (Button) d.getButton(Dialog.BUTTON_NEUTRAL);
            neutral.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonNeutral(d);
                }
            });
        }
    }

    private void invalidateViews() {
        if (genderSet == null) {
            genderSet = EnumSet.allOf(Gender.class);
        }
        dialogFilterSwitchMode.setChecked(excluding);
        if(genre != null) dialogFilterGenre.setSelection(ItemAdapter.indexOf(genre, Genre.values())); else dialogFilterGenre.setSelection(0);
        if(type != null) dialogFilterType.setSelection(ItemAdapter.indexOf(type, Type.values())); else dialogFilterType.setSelection(0);
        if(size != null && size > 0) dialogSize.setText((CharSequence) size.toString()); else dialogSize.setText("");
        for (Gender gender : genderSet) {
            switch (gender) {
                case MALE:
                    dialogFilterMale.setChecked(true);
                    break;
                case FEMALE:
                    dialogFilterFemale.setChecked(true);
                    break;
                case UNDEFINED:
                    dialogFilterUndefined.setChecked(true);
                    break;
            }
        }
        dialogSortActivity.setChecked(false);
        dialogSortRating.setChecked(false);
        dialogSortViews.setChecked(false);
        if (sortWorksBy != null) {
            switch (sortWorksBy) {
                case ACTIVITY:
                    dialogSortActivity.setChecked(true);
                    break;
                case RATING:
                    dialogSortRating.setChecked(true);
                    break;
                case VIEWS:
                    dialogSortViews.setChecked(true);
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        saveState();
        super.onDestroyView();
    }
}
