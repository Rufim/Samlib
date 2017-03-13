package ru.samlib.client.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Switch;
import butterknife.Bind;
import butterknife.ButterKnife;
import ru.samlib.client.R;
import ru.samlib.client.domain.entity.Gender;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.fragments.FilterDialogListFragment;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Created by 0shad on 25.10.2015.
 */
public class FilterDialog extends BaseDialog {

    @Bind(R.id.dialog_filter_switch_mode)
    Switch dialogFilterSwitchMode;
    @Bind(R.id.dialog_filter_grid)
    GridLayout dialogFilterGrid;
    @Bind(R.id.scrollView)
    ScrollView scrollView;
    @Bind(R.id.dialog_filter_male)
    CheckBox dialogFilterMale;
    @Bind(R.id.dialog_filter_female)
    CheckBox dialogFilterFemale;
    @Bind(R.id.dialog_filter_undefined)
    CheckBox dialogFilterUndefined;
    View rootView;
    ArrayList<Genre> genreList;
    EnumSet<Gender> genderSet = EnumSet.allOf(Gender.class);
    boolean excluding = false;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_filter, null);
        ButterKnife.bind(this, rootView);
        dialogFilterSwitchMode.setChecked(excluding);
        for (Genre genre : Genre.values()) {
            if (genreList == null) {
                addToGrid(genre, false);
            } else {
                addToGrid(genre, genreList.contains(genre));
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
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.newest_filter)
                .setPositiveButton(android.R.string.ok, this)
                .setNeutralButton(R.string.dialog_filter_clear, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(rootView);
        return adb.create();
    }

    public void setState(FilterDialogListFragment.FilterEvent filterEvent) {
        if (filterEvent != null) {
            this.excluding = filterEvent.excluding;
            this.genreList = filterEvent.genres;
            this.genderSet = filterEvent.genders;
        }
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        super.onButtonPositive(dialog);
        saveState();
        postEvent(new FilterDialogListFragment.FilterEvent(genreList, genderSet, excluding));
    }

    private void saveState() {
        ArrayList<Genre> genres = new ArrayList<>();
        for (int i = 0; i < dialogFilterGrid.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) dialogFilterGrid.getChildAt(i);
            if (checkBox.isChecked()) {
                genres.add((Genre) checkBox.getTag());
            }
        }
        genreList = genres;
        excluding = dialogFilterSwitchMode.isChecked();
        if(!dialogFilterMale.isChecked()) genderSet.remove(Gender.MALE);
        else genderSet.add(Gender.MALE);

        if (!dialogFilterFemale.isChecked()) genderSet.remove(Gender.FEMALE);
        else genderSet.add(Gender.FEMALE);

        if (!dialogFilterUndefined.isChecked()) genderSet.remove(Gender.UNDEFINED);
        else genderSet.add(Gender.UNDEFINED);
    }

    @Override
    public void onButtonNeutral(DialogInterface dialog) {
        postEvent(new FilterDialogListFragment.FilterEvent(null, excluding));
    }

    private void addToGrid(Genre genre, boolean checked) {
        if (genre != null) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setText(genre.getTitle());
            checkBox.setTag(genre);
            checkBox.setChecked(checked);
            dialogFilterGrid.addView(checkBox);
        }
    }

    @Override
    public void onDestroyView() {
        saveState();
        super.onDestroyView();
    }
}
