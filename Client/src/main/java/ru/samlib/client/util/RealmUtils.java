package ru.samlib.client.util;

import io.realm.RealmList;
import io.realm.RealmObject;
import ru.samlib.client.domain.entity.realm.RealmString;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 10.07.2015.
 */
public class RealmUtils {

    public static <R extends  RealmObject, T extends R> List<T> fromRealmList(RealmList<R> realmList, Class<T> targetClass) throws InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>(realmList.size());
        for (R r : realmList) {
            list.add(ReflectionUtils.castObject(r, targetClass));
        }
        return list;
    }

    public static <R extends RealmObject, S extends R> RealmList<R> toRealmList(List<S> sourceList, Class<R> targetClass) throws InstantiationException, IllegalAccessException {
        RealmList<R> list = new RealmList<R>();
        for (S source : sourceList) {
            list.add(ReflectionUtils.castObject(source, targetClass));
        }
        return list;
    }

    public static List<String> fromRealmStringList(RealmList<RealmString> realmList) {
        List<String> list = new ArrayList<>(realmList.size());
        for (RealmString realmString : realmList) {
            list.add(realmString.getValue());
        }
        return list;
    }

    public static RealmList<RealmString> toRealmStringList(List<String> strings) {
        RealmList<RealmString> list = new RealmList<>();
        for (String string : strings) {
            list.add(new RealmString(string));
        }
        return list;
    }




}
