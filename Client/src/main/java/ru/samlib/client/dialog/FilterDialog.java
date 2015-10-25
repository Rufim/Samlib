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
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.events.FilterEvent;

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
    View rootView;
    ArrayList<Genre> genreList;
    boolean excluding = false;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_filter, null);
        ButterKnife.bind(this, rootView);
        dialogFilterSwitchMode.setChecked(excluding);
        for (Genre genre : Genre.values()) {
            if(genreList == null) {
                addToGrid(genre, false);
            } else {
                addToGrid(genre, genreList.contains(genre));
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

    public void setState(FilterEvent filterEvent) {
        if(filterEvent != null) {
            this.excluding = filterEvent.excluding;
            this.genreList = filterEvent.genres;
        }
    }

    @Override
    public void onButtonPositive(DialogInterface dialog) {
        super.onButtonPositive(dialog);
        saveState();
        postEvent(new FilterEvent(genreList, excluding));
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
    }

    @Override
    public void onButtonNeutral(DialogInterface dialog) {
        postEvent(new FilterEvent(null, excluding));
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
