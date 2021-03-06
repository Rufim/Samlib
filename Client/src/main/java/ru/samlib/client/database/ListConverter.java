package ru.samlib.client.database;

import android.annotation.SuppressLint;
import com.raizlabs.android.dbflow.converter.TypeConverter;
import net.vrallev.android.cat.Cat;
import ru.samlib.client.domain.entity.Genre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by 0shad on 26.06.2016.
 */
public interface ListConverter {
    String SEPARATOR = "\0007";
    char STRING = 'S';
    char ENUM = 'E';

    @SuppressLint("NewApi")
    default String getStringDBValue(List list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int size = list.size();
        int index = 0;
        if (list.get(0) instanceof String) {
            sb.append(STRING);
            for (String s : (List<String>) list) {
                ++index;
                sb.append(s);
                if (index < size) {
                    sb.append(SEPARATOR);
                }
            }
        }
        if (list.get(0) instanceof Enum) {
            sb.append(ENUM);
            sb.append("[" + list.get(0).getClass().getName() + "]");
            for (Enum s : (List<Enum>) list) {
                ++index;
                sb.append(s.name());
                if (index < size) {
                    sb.append(SEPARATOR);
                }
            }
        }
        return sb.toString();
    }

    @SuppressLint("NewApi")
    default List getListModelValue(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        switch (value.charAt(0)) {
            case ENUM:
                try {
                    Enum[] enums = (Enum[]) Class.forName(value.substring(value.indexOf("[") + 1, value.indexOf("]"))).getEnumConstants();
                    ArrayList<Enum> enumArrayList = new ArrayList<>();
                    for (String name : value.substring(value.indexOf("]") + 1).split(SEPARATOR)) {
                        for (Enum anEnum : enums) {
                            if (anEnum.name().equals(name)) {
                                enumArrayList.add(anEnum);
                            }
                        }
                    }
                    return enumArrayList;
                } catch (ClassNotFoundException e) {
                    Cat.e(e);
                    return null;
                }
            case STRING:
                return Arrays.asList(value.substring(1).split(SEPARATOR));
        }
        return Collections.emptyList();
    }


}
