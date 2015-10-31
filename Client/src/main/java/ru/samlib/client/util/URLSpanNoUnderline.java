package ru.samlib.client.util;

import android.text.TextPaint;
import android.text.style.URLSpan;

/**
 * Created by 0shad on 31.10.2015.
 */
public class URLSpanNoUnderline extends URLSpan {

    public URLSpanNoUnderline(String url) {
        super(url);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
    }
}
