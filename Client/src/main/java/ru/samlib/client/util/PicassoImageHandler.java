package ru.samlib.client.util;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;
import ru.kazantsev.template.util.DynamicImageSpan;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Dmitry on 16.07.2015.
 */
public class  PicassoImageHandler extends TagNodeHandler {

    final TextView textView;
    final Picasso picasso;

    public PicassoImageHandler(final TextView textView) {
        this.textView = textView;
        this.picasso = Picasso.with(textView.getContext());
    }

    @Override
    public void handleTagNode(TagNode tagNode, final SpannableStringBuilder builder, final int start, int end, SpanStack stack) {
        builder.append("￼");
        Drawable drawable = textView.getResources().getDrawable(R.drawable.ic_image_crop_original);
        int textSize = (int) (textView.getTextSize() * 1.25);
        int width = parseDimen(tagNode.getAttributeByName("width"));
        int height = parseDimen(tagNode.getAttributeByName("height"));
        if(width < 0) {
            width = textSize;
            height = textSize;
        } else if(height < 0) {
            height = width;
        }
        drawable.setBounds(0, 0, width, height);
        final DynamicImageSpan imageSpan = new DynamicImageSpan(drawable);
        stack.pushSpan(imageSpan, start, builder.length());
        new AsyncTask<TagNode, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(final TagNode... meh) {
                try {
                    if (meh.length > 0) {
                        TagNode tag = meh[0];
                        String src = tagNode.getAttributeByName("src");
                        URL url;
                        try {
                            url = new URL(src);
                        } catch (MalformedURLException ex) {
                            url = new URL(Constants.Net.BASE_DOMAIN + "/" + src);
                        }
                        if(src != null) {
                            int loc[] = new int[2];
                            textView.getLocationOnScreen(loc);
                            float density = textView.getResources().getDisplayMetrics().density;
                            int maxWidth = textView.getResources().getDisplayMetrics().widthPixels
                                    - loc[0];
                            int textViewWidth = textView.getWidth();
                            if(textViewWidth > 0) {
                                maxWidth = Math.min(maxWidth, textViewWidth);
                            }
                            int width = parseDimen(tagNode.getAttributeByName("width"));
                            int height = parseDimen(tagNode.getAttributeByName("height"));
                            return picasso.load(url.toString()).transform(new PicassoTransformImage(width, height, maxWidth, density)).get();
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                if (bitmap != null) {
                    Drawable drawable = new BitmapDrawable(textView.getResources(), bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth() - 1, bitmap.getHeight() - 1);
                    imageSpan.setDrawable(drawable);
                    if (textView != null) {
                        textView.setText(textView.getText());
                    }
                }
            }

        }.execute(tagNode);
    }

    int parseDimen(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

}
