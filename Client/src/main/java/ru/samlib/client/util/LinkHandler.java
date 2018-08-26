package ru.samlib.client.util;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.widget.TextView;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;
import ru.kazantsev.template.util.URLSpanNoUnderline;

/**
 * Created by Dmitry on 21.10.2015.
 */
public class LinkHandler extends net.nightwhistler.htmlspanner.handlers.LinkHandler {


    @Override
    protected URLSpan newSpan(String url) {
        return new URLSpanNoUnderline(url);
    }

    @Override
    protected String getHref(TagNode node) {
        String href = node.getAttributeByName("href");
        if(href == null) {
            href = node.getAttributeByName("l:href"); // FB2 ref
        }
        return href;
    }

}
