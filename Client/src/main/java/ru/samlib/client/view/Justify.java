package ru.samlib.client.view;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.ScaleXSpan;
import android.text.style.UpdateLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Justify {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    public static final float DEFAULT_MAX_PROPORTION = 10f;

    /**
     * Adds ScaleX spans to expand widespaces and justify the lines.
     * @param justified the justified TextView.
     */
    public static void setupScaleSpans(final @NotNull Justify.Justified justified) {
        final TextView textView = justified.getTextView();
        final CharSequence text = textView.getText();

        // The text should be a spannable already because we set a movement method.
        if (!(text instanceof Spannable)) return;
        final Spannable spannable = (Spannable)text;
        final int length = spannable.length();
        if (length == 0) return;

        // We use the layout to get line widths before justification
        final Layout layout = textView.getLayout();
        if(layout == null) return;
        final int count = layout.getLineCount();
        if (count < 2) return;

        // Layout line widths do not include the padding
        final int want = textView.getMeasuredWidth() -
                textView.getCompoundPaddingLeft() - textView.getCompoundPaddingRight();

        // We won't justify lines if it requires expanding the spaces beyond the maximum proportion.
        final float maxProportion;
        if (textView instanceof Justify.Justified) {
            maxProportion = ((Justify.Justified)textView).getMaxProportion();
        }
        else {
            maxProportion = DEFAULT_MAX_PROPORTION;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(spannable);

        // Remove any existing ScaleXSpan (from a previous pass).
        final   ScaleSpan[] scaleSpans = builder.getSpans(0, builder.length(), ScaleSpan.class);
        if (scaleSpans != null) {
            for (final  ScaleSpan span: scaleSpans) {
                spannable.removeSpan(span);
                builder.removeSpan(span);
            }
        }

        Map<Integer, Integer> textViewSpanStarts = new LinkedHashMap<>();
        Map<Integer, Integer> textViewSpanEnds = new LinkedHashMap<>();
        List<ScaleSpan> spans = new ArrayList<>();
        for (int line=0; line<count; ++line) {

            final int lineStart = layout.getLineStart(line);
            final int lineEnd = line == count - 1 ? length : layout.getLineEnd(line);

            // Don't justify empty lines
            if (lineEnd == lineStart) continue;

            // Don't justify the last line or lines ending with a newline.
            if (lineEnd == length || builder.charAt(lineEnd - 1) == '\n') continue;

            // Don't include the trailing whitespace as an expandable whitespace.
            final int visibleLineEnd = layout.getLineVisibleEnd(line);
//      int visibleLineEnd = lineEnd;
//      while (visibleLineEnd > lineStart &&
//             Character.isWhitespace(builder.charAt(visibleLineEnd-1))) --visibleLineEnd;

            // Don't justify lines that only contain whitespace
            if (visibleLineEnd == lineStart) continue;

            // Layout line width
//      final float w = layout.getLineWidth(line);    // Works fine, but only for API > 11
//      final float w = layout.getLineMax(line);      // Doesn't work well
            final float w = Layout.getDesiredWidth(builder, lineStart, lineEnd, layout.getPaint());

            // Remaining space to fill
            int remaining = (int)Math.floor(want - w);
            if (remaining > 0) {
                // Make sure trailing whitespace doesn't use any space by setting its scaleX to 0
                if (visibleLineEnd < lineEnd) {
                    builder.setSpan(
                            new ScaleSpan(0f),
                            visibleLineEnd,
                            lineEnd,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }

                // Line text
                final CharSequence sub = builder.subSequence(lineStart, visibleLineEnd);

                // Accumulated total whitespace width
                float spaceWidth = 0f;
                // Number of whitespace sections
                int n = 0;

                // Find whitespace sections and store their start and end positions
                final Matcher matcher = WHITESPACE_PATTERN.matcher(sub);
                int previousStartIndent = 0;
                while (matcher.find()) {
                    final int matchStart = matcher.start();
                    final int matchEnd = matcher.end();
                    // If the line starts with whitespace, it's probably an indentation
                    // and we don't want to expand indentation space to preserve alignment
                    if (matchStart == 0 || previousStartIndent == matchStart) {
                        previousStartIndent = matchEnd;
                        continue;
                    }
                    // skip single thin and hair spaces, as well as a single non breaking space
                    if ((matchEnd - matchStart) == 1) {
                        final int c = sub.charAt(matchStart);
                        if (c == '\u200a' || c == '\u2009' || c == '\u00a0') continue;
                    }
                    if(layout.getPaint() == null) return;
                    final float matchWidth = measureSpannedText(builder, layout,lineStart + matchStart, lineStart + matchEnd);
                         //   layout.getPaint().measureText(builder, lineStart + matchStart, lineStart + matchEnd);

                    spaceWidth += matchWidth;

                    textViewSpanStarts.put(n, matchStart);
                    textViewSpanEnds.put(n, matchEnd);
                    ++n;
                }

                // Excess space is distributed evenly
                // (with the same proportions for all whitespace sections)
                final float proportion = (spaceWidth + remaining) / spaceWidth;

                // Don't justify the line if we can't do it without expanding whitespaces too much.
                if (proportion > maxProportion) continue;

                // Add ScaleX spans on the whitespace sections we want to expand.
                for (int span=0; span<n; ++span) {
                    ScaleSpan scaleSpan = new ScaleSpan(proportion);
                    spans.add(scaleSpan);
                    builder.setSpan(
                            scaleSpan,
                            lineStart + textViewSpanStarts.get(span),
                            lineStart + textViewSpanEnds.get(span),
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                // Compute the excess space.
                int excess = (int)Math.ceil(Layout.getDesiredWidth(builder,
                        lineStart, lineEnd,
                        layout.getPaint())) - want;
                // We might have added too much space because of rounding errors and because adding spans
                // can modify the kerning.
                // If that is the case, then we try to reduce the extra space slightly until there's no
                // excess space left.
                int loop = 0;
                while (excess > 0) {
                    if (++loop == 4) {
                        android.util.Log.e("ERROR",
                                "Could not compensate for excess space (" + excess + "px).");
                    }
                    // Clear the spans from the previous attempt.
                    for (int span=0; span<n; ++span) {
                        builder.removeSpan(spans.get(span));
                    }
                    spans.clear();
                    // Reduce the remaining space exponentially for each iteration.
                    remaining -= (excess + loop * loop);
                    // Set the spans with the new proportions.
                    final float reducedProportions = (spaceWidth + remaining) / spaceWidth;
                    for (int span=0; span<n; ++span) {
                        ScaleSpan scaleSpan = new ScaleSpan(reducedProportions);
                        spans.add(scaleSpan);
                        builder.setSpan(
                                scaleSpan,
                                lineStart + textViewSpanStarts.get(span),
                                lineStart + textViewSpanEnds.get(span),
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    // recompute the excess space.
                    excess = (int)Math.ceil(Layout.getDesiredWidth(builder,
                            lineStart, lineEnd,
                            layout.getPaint())) - want;
                }
            }
        }
        if(builder.length() > 0) {
            textView.setText(builder, TextView.BufferType.SPANNABLE);
        }
    }


    private static float measureSpannedText(CharSequence text, Layout layout, int start, int end) {
        StaticLayout tempLayout = new StaticLayout(text.subSequence(start, end), layout.getPaint(), Integer.MAX_VALUE, layout.getAlignment(), layout.getSpacingMultiplier(), layout.getSpacingAdd(), false);
        int lineCount = tempLayout.getLineCount();
        float textWidth =0;
        for(int i=0 ; i < lineCount ; i++){
            textWidth += tempLayout.getLineWidth(i);
        }
        return textWidth;
    }

    public static interface Justified {

        /**
         * Gets the TextView (usually the class implementing this interface).
         * @return the TextView.
         */
        @NotNull
        public TextView getTextView();

        /**
         * Gets the maximum stretching proportion allowed for whitespaces. Lines that require
         * expanding whitespace beyond this proportion to be justified will not be justified.
         * @return the maximum stretching proportion allowed.
         */
        public float getMaxProportion();

    }

    public static class ScaleSpan extends MetricAffectingSpan {

        private final float mProportion;

        public ScaleSpan(final float proportion) {
            mProportion = proportion;
        }

        @Override
        public void updateDrawState(final @NotNull TextPaint ds) {
            ds.setTextScaleX(ds.getTextScaleX() * mProportion);
        }

        @Override
        public void updateMeasureState(final @NotNull TextPaint ds) {
            ds.setTextScaleX(ds.getTextScaleX() * mProportion);
        }

    }

}
