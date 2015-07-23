package ru.samlib.client.domain;

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

    public static String cleanupSlashes(String link) {
        return link.replaceAll("/+","/");
    }
}
