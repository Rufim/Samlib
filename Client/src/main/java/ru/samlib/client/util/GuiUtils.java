package ru.samlib.client.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Rufim
 * Date: 16.11.13
 * Time: 15:21
 * To change this template use File | Settings | File Templates.
 */
public class GuiUtils {

    public static class EmptyTextException extends Exception {
        static final String MESSAGE = "TextView do not contains string";

        @Override
        public String getMessage() {
            return MESSAGE;
        }
    }

    private static final String TAG = SystemUtils.getClassName();

    public static <A extends Activity> void startActivity(Context context, Class<A> aClass) {
        Intent intent = new Intent(context, aClass);
        context.startActivity(intent);
    }

    public static <A extends Activity, V extends Serializable> void startActivity(Context context, Class<A> aClass, HashMap<String, V> putValues) {
        Intent intent = new Intent(context, aClass);
        for (Map.Entry<String, V> entry : putValues.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        context.startActivity(intent);
    }

    public static <A extends Activity, V extends Serializable> void startActivity(Context context, Class<A> aClass, String key, V value) {
        Intent intent = new Intent(context, aClass);
        intent.putExtra(key, value);
        context.startActivity(intent);
    }

    public static void setImageToImageView(Activity activity, int resid, ImageView view) {
        int width = view.getWidth();
        int height = view.getHeight();
        view.setImageBitmap(GuiUtils.decodeSampleImage(activity, resid, width, height));
    }

    public static Bitmap decodeSampleImage(Context context, int resid, int width, int height) {
        try {
            System.gc(); // First of all free some memory

            // Decode image size

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resid, o);

            // The new size we want to scale to

            final int requiredWidth = width;
            final int requiredHeight = height;

            // Find the scale value (as a power of 2)

            int sampleScaleSize = 1;

            while (o.outWidth / sampleScaleSize / 2 >= requiredWidth && o.outHeight / sampleScaleSize / 2 >= requiredHeight)
                sampleScaleSize *= 2;

            // Decode with inSampleSize

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sampleScaleSize;

            return BitmapFactory.decodeResource(context.getResources(), resid, o2);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage()); // We don't want the application to just throw an exception
        }

        return null;
    }

    public static Bitmap decodeSampleImage(File f, int width, int height) {
        try {
            System.gc(); // First of all free some memory

            // Decode image size

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to

            final int requiredWidth = width;
            final int requiredHeight = height;

            // Find the scale value (as a power of 2)

            int sampleScaleSize = 1;

            while (o.outWidth / sampleScaleSize / 2 >= requiredWidth && o.outHeight / sampleScaleSize / 2 >= requiredHeight)
                sampleScaleSize *= 2;

            // Decode with inSampleSize

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sampleScaleSize;

            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e); // We don't want the application to just throw an exception
        }

        return null;
    }

    public static Bitmap combineImages(Bitmap left, Bitmap right) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom

        System.gc();

        Bitmap cs = null;

        int width, height = 0;

        width = left.getWidth() + right.getWidth();

        if (left.getHeight() > right.getHeight()) {
            height = left.getHeight();
        } else {
            height = right.getHeight();
        }

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        Canvas comboImage = new Canvas(cs);
        comboImage.drawBitmap(left, 0f, 0f, null);
        comboImage.drawBitmap(right, left.getWidth(), 0f, null);
        return cs;
    }

    public static Bitmap getSafeBitmap(String path, int imageMaxSize) {
        try {
            return getSafeBitmap(new FileInputStream(path), imageMaxSize);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage() + " image path = " + path, e);
            return null;
        }
    }

    public static Bitmap getSafeBitmap(InputStream is, int imageMaxSize) throws IOException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BufferedInputStream inputStream = new BufferedInputStream(is);
        inputStream.markSupported();
        inputStream.mark(inputStream.available());
        BitmapFactory.decodeStream(inputStream, null, o);
        inputStream.reset();


        int scale = 1;
        while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                imageMaxSize) {
            scale++;
        }

        Bitmap b = null;
        if (scale > 1) {
            scale--;
            // scale to max possible inSampleSize that still yields an image
            // larger than target
            BitmapFactory.Options bfOptions = new BitmapFactory.Options();
            bfOptions.inDither = false;                     //Disable Dithering mode
            bfOptions.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            bfOptions.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
            bfOptions.inTempStorage = new byte[32 * 1024];
            bfOptions.inSampleSize = scale;
            bfOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            b = BitmapFactory.decodeStream(inputStream, null, bfOptions);

            if (b == null) {
                throw new IOException("Cannot decode stream!");
            }

            // resize to desired dimensions
            int height = b.getHeight();
            int width = b.getWidth();

            double y = Math.sqrt(imageMaxSize
                    / (((double) width) / height));
            double x = (y / height) * width;

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                    (int) y, true);
            b = scaledBitmap;

            System.gc();
        } else {
            b = BitmapFactory.decodeStream(inputStream);
            if (b == null) {
                throw new IOException("Cannot decode stream!");
            }
        }
        inputStream.close();

        Log.d(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                b.getHeight());
        return b;
    }

    public static Bitmap getResizedBitmap(String path, int reqWidth,
                                          int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        return bmp;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static <L extends ViewGroup> L getLayout(Context context, Integer id) {
        return (L) ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(id, null, false);
    }

    public static <V extends View> V getView(Activity activity, Integer id) {
        return (V) activity.findViewById(id);
    }

    public static <V extends View> V getView(View view, Integer id) {
        return (V) view.findViewById(id);
    }

    public static ArrayList<View> getAllChildren(View v) {
        if(v == null) {
            return new ArrayList<View>();
        }

        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();

        ViewGroup viewGroup = (ViewGroup) v;
        result.add(viewGroup);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {

            View child = viewGroup.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static int pxToDp(int px, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) px / density);
    }


    public static boolean isScreenHaveDpWidth(Context context, int dpWidth) {
        return getScreenSize(context).x >= dpWidth;
    }

    public static PointF getScreenSizeDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return new PointF(dpWidth, dpHeight);
    }

    public static boolean isLandscape(Context context) {
        int orientation = getScreenOrientation(context);
        return orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    public static int getScreenOrientation(Context context) {
        WindowManager wm = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        int rotation = wm.getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    public static ViewGroup getParent(View view) {
        return (ViewGroup) view.getParent();
    }

    public static void removeView(View view) {
        if (view == null) {
            return;
        }
        ViewGroup parent = getParent(view);
        if (parent != null) {
            parent.removeView(view);
        }
    }

    public static void replaceView(View currentView, View newView) {
        if(currentView == null) {
            return;
        }
        ViewGroup parent = getParent(currentView);
        if (parent == null) {
            return;
        }
        final int index = parent.indexOfChild(currentView);
        removeView(currentView);
        removeView(newView);
        parent.addView(newView, index);
        parent.invalidate();
    }

    public static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static void fadeOut(View view, int delay, int duration) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(delay);
        fadeOut.setDuration(duration);
        view.setAnimation(fadeOut);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.INVISIBLE);
            }
        }, delay);
    }

    public static void fadeIn(View view, int delay, int duration) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setStartOffset(delay);
        fadeIn.setDuration(duration);
        view.setAnimation(fadeIn);
        view.setVisibility(View.VISIBLE);
    }

    public static int animatedHide(View view, boolean vertical) {
        Animation animation;
        int size = 0;
        if (vertical) {
            size = view.getHeight();
            animation = new ResizeAnimation(view, 0, -view.getHeight());
        } else {
            size = view.getWidth();
            animation = new ResizeAnimation(view, -view.getWidth(), 0);
        }
        view.setAnimation(animation);
        animation.start();
        ((View) view.getParent()).invalidate();
        return size;
    }

    public static void animatedResize(View view, int deltaWidth, int deltaHeight) {
        Animation animation;
        animation = new ResizeAnimation(view, deltaWidth, deltaHeight);
        view.setAnimation(animation);
        animation.start();
        ((View) view.getParent()).invalidate();
    }

    public static void toast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void toast(Context context, int messageId) {
        toast(context, messageId, true);
    }

    public static void toast(Context context, int messageId, boolean shortDuration) {
        Toast toast = Toast.makeText(context, messageId, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static String getText(TextView textView) throws EmptyTextException {
        String text = textView.getText().toString();
        if (text == null || text.isEmpty()) {
            throw new EmptyTextException();
        }
        return text;
    }

    public static void setText(View textView, CharSequence text) {
        if (textView != null) {
            if (text != null) {
                if (textView instanceof TextView) {
                    ((TextView) textView).setText(text);
                }
            }
        }
    }

    public static void setTextOrHide(View textView, CharSequence text, View ... views) {
        if(views.length == 0) {
            views = new View[] {textView};
        }
        if(textView != null) {
            if (text != null) {
                if(textView instanceof TextView) {
                    ((TextView)textView).setText(text);
                    for (View view : views) {
                        view.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                for (View view : views) {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    public static void selectText(TextView textView, boolean erase, int start, int end, int color) {
        if (textView == null) {
            return;
        }

        Spannable raw = new SpannableString(textView.getText());

        if (erase) {
            BackgroundColorSpan[] spans = raw.getSpans(0,
                    raw.length(),
                    BackgroundColorSpan.class);

            if (spans.length > 0) {
                for (BackgroundColorSpan span : spans) {
                    raw.removeSpan(span);
                }
            }
        }
        if(end > raw.length()) {
            end = raw.length();
        }
        raw.setSpan(new BackgroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(raw);
    }

    public static void selectText(TextView textView, boolean erase, String query, int color) {
        if(textView == null) {
            return;
        }
        if(query != null) {
            query = query.trim().toLowerCase();
        }

        Spannable raw = new SpannableString(textView.getText());

        if(erase) {
            BackgroundColorSpan[] spans = raw.getSpans(0,
                    raw.length(),
                    BackgroundColorSpan.class);

            if (spans.length > 0) {
                for (BackgroundColorSpan span : spans) {
                    raw.removeSpan(span);
                }
                if (query == null) {
                    textView.setText(raw);
                    return;
                }
            }
        }

        if (query == null) {
            return;
        }

        if(query.isEmpty()) {
            raw.setSpan(new BackgroundColorSpan(color), 0, raw.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            int index = TextUtils.indexOf(raw.toString().toLowerCase(), query);
            while (index >= 0) {
                raw.setSpan(new BackgroundColorSpan(color), index, index + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = TextUtils.indexOf(raw.toString().toLowerCase(), query, index + query.length());
            }
        }
        textView.setText(raw);
    }

    public int getThemeColor(Context context, int id) {
        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(new int[]{id});
        int result = a.getColor(0, 0);
        a.recycle();
        return result;
    }

    public TypedArray getStyledAttrs(Context context) {
        int[] attrs = {android.R.attr.textColor,
                android.R.attr.textSize,
                android.R.attr.background,
                android.R.attr.textStyle,
                android.R.attr.textAppearance,
                android.R.attr.textColorLink,
                android.R.attr.orientation,
                android.R.attr.text};
        return context.obtainStyledAttributes(attrs);
    }

    public interface RunUIThread {
        void run(Object ... var);
    }

    public static void runInUI(final Context context, final RunUIThread uiThread, final Object... var) {
        if(context != null ) {
            Handler mainHandler = new Handler(context.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    uiThread.run(var);
                }
            };
            mainHandler.postAtFrontOfQueue(myRunnable);
        } else {
            Log.e(TAG, "Context is Null in method: runInUI");
        }
    }
}
