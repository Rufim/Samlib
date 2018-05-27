package ru.samlib.client.domain.entity;

import ru.kazantsev.template.util.TextUtils;

import java.io.Serializable;

/**
 * Created by Rufim on 03.07.2015.
 */
public enum New implements Serializable {
    RED, BROWN, GREY, EMPTY;

    public static New parseNew(String state) {
        state = TextUtils.trim(state.toUpperCase());
        for (New nextNew : New.values()) {
            if(nextNew.name().equals(state.toUpperCase())) return nextNew;
        }
        return EMPTY;
    }
}
