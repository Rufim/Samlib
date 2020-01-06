package ru.samlib.client.domain;

import android.annotation.SuppressLint;
import ru.kazantsev.template.util.TextUtils;

/**
 * Created by Rufim on 02.07.2015.
 */
public interface Linkable {
    public String getLink();

    public String getTitle();

    public String getAnnotation();

    @SuppressLint("NewApi")
    public default String getFullLink() {
        return Constants.Net.BASE_DOMAIN + TextUtils.cleanupSlashes(getLink());
    }

    @SuppressLint("NewApi")
    public default boolean isWork() {
        return isWorkLink(getLink());
    }

    @SuppressLint("NewApi")
    public default boolean isAuthor() {
        return isAuthorLink(getLink());
    }


    public static boolean isSamlibLink(String link) {
        if(link == null) return false;
        return isAuthorLink(link) || isWorkLink(link) || isCommentsLink(link) || isIllustrationsLink(link);
    }

    public static boolean isWorkLink(String link) {
        if(link == null) return false;
        return link.matches(Constants.Pattern.WORK_URL_REGEXP);
    }

    public static boolean isAuthorLink(String link) {
        if(link == null) return false;
        return link.matches(Constants.Pattern.AUTHOR_URL_REGEXP);
    }

    public static boolean isCommentsLink(String link) {
        if(link == null) return false;
        return link.matches(Constants.Pattern.COMMENTS_URL_REGEXP);
    }

    public static boolean isIllustrationsLink(String link) {
        if(link == null) return false;
        return link.matches(Constants.Pattern.ILLUSTRATIONS_URL_REGEXP);
    }
}


