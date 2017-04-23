package ru.samlib.client.util;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.R;

/**
 * Created by 0shad on 23.04.2017.
 */
public class SamlibGuiUtils {

    public static CharSequence generateText(Context context, String title, String addition, @ColorRes int colorRes, float prop) {
        if(TextUtils.isEmpty(addition)) return title;
        String result = title + " " + addition;
        int from = title.length() + 1;
        int to = result.length();
        SpannableStringBuilder spannable = GuiUtils.coloredText(context, result, from, to, colorRes);
        spannable.setSpan(new RelativeSizeSpan(prop), from, to, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

}
