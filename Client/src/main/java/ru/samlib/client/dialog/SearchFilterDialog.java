package ru.samlib.client.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import ru.kazantsev.template.dialog.BaseDialog;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.GuiUtils;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by 0shad on 25.10.2015.
 */
public class SearchFilterDialog extends BaseDialog {

    @BindView(R.id.dialog_filter_switch_mode)
    Switch dialogFilterSwitchMode;
    @BindView(R.id.dialog_filter_grid_genre)
    GridLayout dialogFilterGridGenre;
    @BindView(R.id.dialog_filter_grid_type)
    GridLayout dialogFilterGridType;
    @BindView(R.id.scrollViewGenre)
    ScrollView scrollViewGenre;
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
    View rootView;
    List<Genre> genreList;
    EnumSet<Gender> genderSet;
    Type type;
    SearchStatParser.SortWorksBy sortWorksBy;
    boolean excluding = false;
    String query;
    SearchStatParser parser;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.search_dialog_filter, null);
        ButterKnife.bind(this, rootView);
        for (Genre genre : Genre.values()) {
            addToGrid(dialogFilterGridGenre, genre, false);
        }
        for (Type type : Type.values()) {
            if(!type.equals(Type.OTHER))
            addToGrid(dialogFilterGridType, type, false);
        }
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
                .setTitle(R.string.search_filter)
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
                this.genreList = Arrays.asList(Genre.valueOf(request.getParam(SearchStatParser.SearchParams.genre)));
            } else {
                this.genreList = new ArrayList<>();
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
            this.query = request.getParam(SearchStatParser.SearchParams.query);
        }
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        saveState();
        parser.setFilters(query, genreList.size() > 0 ? genreList.get(0) : null, type, sortWorksBy);
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
        parser.setFilters(query, genreList.size() > 0 ? genreList.get(0) : null, type, sortWorksBy);
    }

    private void saveState() {
        ArrayList<Genre> genres = new ArrayList<>();
        for (int i = 0; i < dialogFilterGridGenre.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) dialogFilterGridGenre.getChildAt(i);
            if (radioButton.isChecked()) {
                genres.add((Genre) radioButton.getTag());
            }
        }
        genreList = genres;
        type = null;
        for (int i = 0; i < dialogFilterGridType.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) dialogFilterGridType.getChildAt(i);
            if (radioButton.isChecked()) {
                type = (Type) radioButton.getTag();
                break;
            }
        }

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
        for (int i = 0; i < dialogFilterGridGenre.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) dialogFilterGridGenre.getChildAt(i);
            if (genreList == null) {
                radioButton.setChecked(false);
            } else {
                radioButton.setChecked(genreList.contains(radioButton.getTag()));
            }
        }
        for (int i = 0; i < dialogFilterGridType.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) dialogFilterGridType.getChildAt(i);
            if (type == null) {
                radioButton.setChecked(false);
            } else {
                radioButton.setChecked(type.equals(radioButton.getTag()));
            }
        }
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

    private void addToGrid(GridLayout gridLayout, Linkable linkable, boolean checked) {
        if (linkable != null) {
            RadioButton radioButton = new RadioButton(getActivity());
            radioButton.setText(linkable.getTitle());
            radioButton.setTag(linkable);
            radioButton.setChecked(checked);
            radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        for (int i = 0; i < gridLayout.getChildCount(); i++) {
                            RadioButton radioButton = (RadioButton) gridLayout.getChildAt(i);
                            if (compoundButton != radioButton && radioButton.isChecked()) {
                                radioButton.setChecked(false);
                            }
                        }
                    }
                }
            });
            gridLayout.addView(radioButton);
        }
    }

    @Override
    public void onDestroyView() {
        saveState();
        super.onDestroyView();
    }
}
