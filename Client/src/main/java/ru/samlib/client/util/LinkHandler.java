package ru.samlib.client.util;

import android.text.SpannableStringBuilder;
import android.widget.TextView;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;
import ru.kazantsev.template.util.URLSpanNoUnderline;

/**
 * Created by Dmitry on 21.10.2015.
 */
public class LinkHandler extends TagNodeHandler {

    final TextView textView;

    public LinkHandler(final TextView textView) {
        this.textView = textView;
    }

    public void handleTagNode(TagNode node, SpannableStringBuilder builder, int start, int end) {
        String href = node.getAttributeByName("href");
        builder.setSpan(new URLSpanNoUnderline(href), start, end, 33);
    }


}
