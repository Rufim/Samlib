package ru.samlib.client.domain;

import android.content.Context;
import com.nd.android.sdp.im.common.widget.htmlview.css.StylableElement;
import ru.samlib.client.domain.entity.Link;

/**
 * Created by Rufim on 02.07.2015.
 */
public interface Linkable {
    public String getLink();

    public String getTitle();

    public default String getFullLink() {
        return Link.getBaseDomain() + cleanupSlashes(getLink());
    }

    public default boolean isWork() {
        return isWorkLink(getLink());
    }

    public default boolean isAuthor() {
        return isAuthorLink(getLink());
    }

    public static String cleanupSlashes(String link) {
        return link.replaceAll("/+", "/");
    }

    public static boolean isSamlibLink(String link) {
        return link.matches("/*[a-z]/+[a-z_]+((/*)|(/+[a-z-_0-9]+\\.shtml))?");
    }

    public static boolean isWorkLink(String link) {
        return link.matches("/*[a-z]/+[a-z_]+/+[a-z-_0-9]+\\.shtml");
    }

    public static boolean isAuthorLink(String link) {
        return link.matches("/*[a-z]/+[a-z_]+(/*)");
    }
}


