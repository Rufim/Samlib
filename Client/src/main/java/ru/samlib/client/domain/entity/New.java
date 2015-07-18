package ru.samlib.client.domain.entity;

import ru.samlib.client.util.SystemUtils;

/**
 * Created by Rufim on 03.07.2015.
 */
public enum New {
    RED, BROWN, GREY, EMPTY;

    public static New parseNew(String state) {
        state = SystemUtils.trim(state.toUpperCase());
        for (New nextNew : New.values()) {
            if(nextNew.name().equals(state.toUpperCase())) return nextNew;
        }
        return EMPTY;
    }
}
