package ru.samlib.client.domain.entity;

import android.annotation.SuppressLint;
import com.raizlabs.android.dbflow.sql.language.Operator;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Admin on 11.09.2017.
 */
public interface DBFlowFetch {


    @SuppressLint("NewApi")
    default <C> List<C> dbFlowOneTwoManyUtilMethod(List<C> list, Class<C> clazz, Operator in) {
        if (list == null) {
            list = SQLite.select()
                    .from(clazz)
                    .where(in)
                    .queryList();
        }
        return list;
    }
}
