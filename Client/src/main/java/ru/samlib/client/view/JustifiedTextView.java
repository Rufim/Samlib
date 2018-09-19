package ru.samlib.client.view;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import net.vrallev.android.cat.Cat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class JustifiedTextView extends android.support.v7.widget.AppCompatTextView implements Justify.Justified {

    String lastString;

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context) {
        super(context);
        super.setMovementMethod(new LinkMovementMethod());
    }

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context, final AttributeSet attrs) {
        super(context, attrs);
        if (getMovementMethod() == null) super.setMovementMethod(new LinkMovementMethod());
    }

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context,
                             final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        if (getMovementMethod() == null) super.setMovementMethod(new LinkMovementMethod());
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Make sure we don't call setupScaleSpans again if the measure was triggered
        // by setupScaleSpans itself.
        if (!mMeasuring) {
            final Typeface typeface = getTypeface();
            final float textSize = getTextSize();
            final float textScaleX = getTextScaleX();
            final boolean fakeBold = getPaint().isFakeBoldText();
            final int width = MeasureSpec.getSize(widthMeasureSpec);
            if (mTypeface != typeface ||
                    mTextSize != textSize ||
                    mTextScaleX != textScaleX ||
                    mFakeBold != fakeBold ||
                    mWidth != width) {
                if (width > 0) {
                    mTypeface = typeface;
                    mTextSize = textSize;
                    mTextScaleX = textScaleX;
                    mFakeBold = fakeBold;
                    mWidth = width;
                    mMeasuring = true;
                    try {
                        justify();
                    } finally {
                        mMeasuring = false;
                    }
                }
            }
        }
    }

    private void justify() {
        // Setup ScaleXSpans on whitespaces to justify the text.
        try {
            Justify.setupScaleSpans(this);
        } catch (Throwable ex) {
            Cat.e(ex);
        }
    }



    @Override
    protected void onTextChanged(final CharSequence text,
                                 final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        final Layout layout = getLayout();
        if (layout != null) {
            if (lastString == null || !lastString.equals(text.toString())) {
                lastString = getText().toString();
                justify();
            }
        }
    }

    public boolean isJustified() {
        CharSequence text = getText();
        if (text instanceof SpannableString) {
            return ((SpannableString) text).getSpans(0, text.length(), Justify.ScaleSpan.class).length > 0;
        }
        return false;
    }


    @Override
    @NotNull
    public TextView getTextView() {
        return this;
    }

    @Override
    public float getMaxProportion() {
        return Justify.DEFAULT_MAX_PROPORTION;
    }

    private static final int MAX_SPANS = 512;

    private boolean mMeasuring = false;

    private Typeface mTypeface = null;
    private float mTextSize = 0f;
    private float mTextScaleX = 0f;
    private boolean mFakeBold = false;
    private int mWidth = 0;
}
