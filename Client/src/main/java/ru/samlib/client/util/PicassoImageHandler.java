package ru.samlib.client.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.text.SpannableStringBuilder;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;
import ru.samlib.client.R;

/**
 * Created by Dmitry on 16.07.2015.
 */
public class PicassoImageHandler extends TagNodeHandler {

    final Picasso pablo;
    final TextView textView;
    final FragmentManager manager;
    final Resources resources;

    public PicassoImageHandler(final TextView textView, Resources resources, final Picasso pablo, final FragmentManager manager) {
        this.textView = textView;
        this.resources = resources;
        this.pablo = pablo;
        this.manager = manager;
    }

    @Override
    public void handleTagNode(TagNode tagNode, final SpannableStringBuilder builder, final int start, int end) {
        builder.append("￼");
        Drawable drawable = resources.getDrawable(R.drawable.ic_image_crop_original);
        int textSize = (int) (textView.getTextSize() * 1.25);
        drawable.setBounds(0, 0, textSize, textSize);
        final DynamicImageSpan imageSpan = new DynamicImageSpan(drawable);
        final int trueEnd = end;
        builder.setSpan(imageSpan, start, builder.length(), 33);
        new AsyncTask<TagNode, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(final TagNode... meh) {
                try {
                    if (meh.length > 0) {
                        TagNode tag = meh[0];
                        String src = tagNode.getAttributeByName("src");
                        if(src != null) {
                            int width = parseDimen(tagNode.getAttributeByName("width"));
                            int height = parseDimen(tagNode.getAttributeByName("height"));
                            return pablo.load(src).transform(new TransformImage(width, height, textView.getContext())).get();
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
                return null;
            }

            int parseDimen(String value) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                if (bitmap != null) {
                    Drawable drawable = new BitmapDrawable(resources, bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth() - 1, bitmap.getHeight() - 1);
                    imageSpan.setDrawable(drawable);
                    if (textView != null) {
                        textView.setText(textView.getText());
                    }
                }
            }

        }.execute(tagNode);
    }

    public static class TransformImage implements Transformation {

        final int width;
        final int height;
        final Context context;


        public TransformImage(int width, int height, Context context) {
            this.width = width;
            this.height = height;
            this.context = context;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            float scale = 1;
            if (width < 0 && height > 0) {
                scale = ((float) height / bitmapHeight);
            }
            if (width > 0 && height < 0) {
                scale = ((float) width / bitmapWidth);
            }
            scale *= context.getResources().getDisplayMetrics().density;
            Bitmap scaledBitmap;
            if (scale != 1) {
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                // Create a new bitmap and convert it to a format understood by the ImageView
                scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);
                if (scaledBitmap != bitmap) {
                    // Same bitmap is returned if sizes are the same
                    bitmap.recycle();
                }
            } else {
                scaledBitmap = bitmap;
            }
            return scaledBitmap;
        }

        @Override
        public String key() {
            return "transformation" + " desiredWidth";
        }
    }
}
