package ru.samlib.client.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.squareup.picasso.Transformation;

/**
 * Created by Dmitry on 22.07.2015.
 */
public class PicassoTransformImage implements Transformation {

    final int width;
    final int height;
    final int maxWidth;
    final float density;


    public PicassoTransformImage(int width, int height, int maxWidth, float density) {
        this.width = width;
        this.height = height;
        this.maxWidth = maxWidth;
        this.density = density;
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
        scale *= density;
        if (scale * bitmapWidth > maxWidth) {
            scale = ((float) maxWidth / bitmapWidth);
        }
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
            return scaledBitmap;
        } else {
            return bitmap;
        }
    }

    @Override
    public String key() {
        return "transformation_" + "Width:" + Math.min(width * density, maxWidth) + "Height:" + height;
    }

}
