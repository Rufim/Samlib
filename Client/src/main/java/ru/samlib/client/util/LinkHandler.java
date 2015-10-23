package ru.samlib.client.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;

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


    private class URLSpanNoUnderline extends URLSpan {

        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}
