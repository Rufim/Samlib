package com.nd.android.sdp.im.common.widget.htmlview.view;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.felipecsl.gifimageview.library.GifImageView;
import com.koushikdutta.ion.Ion;
import com.nd.android.sdp.im.common.widget.htmlview.R;
import com.nd.android.sdp.im.common.widget.htmlview.css.Style;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

/**
 * Wraps a "regular" android view with HTML margins, padding and borders.
 */
@SuppressLint("ViewConstructor")
class NativeElementView extends AbstractElementView implements View.OnClickListener {

    private View nativeView;
    private static Executor executor = new ThreadPoolExecutor(0, 4,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());
    private static HashMap<Integer, ImageView> pendingImageViews = new HashMap<>();
    ;

    public abstract static class PostCallback<T> implements Callable {
        protected final T t[];

        public PostCallback(T... t) {
            this.t = t;
        }
    }

    private final static int MATCH_PARENT = -1;
    private final static int WRAP_CONTENT = -2;


    public static class ProportionalImageView extends ImageView {

        public ProportionalImageView(Context context) {
            super(context);
        }

        public ProportionalImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ProportionalImageView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            Drawable d = getDrawable();
            if (d != null) {
                int w = MeasureSpec.getSize(widthMeasureSpec);
                int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
                setMeasuredDimension(w, h);
            } else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }


    static NativeElementView createFresco(Context context, Element child) {
        GenericDraweeHierarchyBuilder builder =
                new GenericDraweeHierarchyBuilder(context.getResources());
        RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
        roundingParams.setBorder(Color.TRANSPARENT, 1);
        roundingParams.setRoundAsCircle(true);
        GenericDraweeHierarchy hierarchy = builder
                .setFadeDuration(300)
                .setPlaceholderImage(context.getResources().getDrawable(R.drawable.ic_image_crop_original))
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_START)
                .setRoundingParams(roundingParams)
                .build();
        final SimpleDraweeView imageView = new SimpleDraweeView(context, hierarchy);
        String src = child.getAttributeValue("src");
        if(src != null) {
            final int width = child.getAttributeInt("width", MATCH_PARENT);
            final int height = child.getAttributeInt("height", MATCH_PARENT);
            imageView.setLayoutParams(new LayoutParams(width, height));
            imageView.setAspectRatio(1.33f);
            imageView.setImageURI(Uri.parse(src));
            imageView.setAdjustViewBounds(true);

          /*  Postprocessor myPostprocessor = new Postprocessor() {

                @Override
                public CloseableReference<Bitmap> process(Bitmap bitmap, PlatformBitmapFactory platformBitmapFactory) {
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();
                    float scale = 1;
                    if (width == WRAP_CONTENT && height != WRAP_CONTENT) {
                        scale = ((float) height / bitmapHeight);
                    }
                    if (width != WRAP_CONTENT && height == WRAP_CONTENT) {
                        scale = ((float) width / bitmapWidth);
                    }
                    scale *= imageView.getResources().getDisplayMetrics().density;
                    CloseableReference<Bitmap> bitmapRef = platformBitmapFactory.createBitmap(
                            (int)(bitmapWidth * scale),
                            (int)(bitmapHeight * scale));
                    return CloseableReference.cloneOrNull(bitmapRef);
                }

                @Override
                public String getName() {
                    return "Scaled bitmap image processor";
                }
            };
            ImageRequest request = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(src))
                    .setAutoRotateEnabled(true)
                    .setLocalThumbnailPreviewsEnabled(true)
                    .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                    .setProgressiveRenderingEnabled(false)
                    .setPostprocessor(myPostprocessor)
                    .build();
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(request)
                    .setTapToRetryEnabled(true)
                    .build();
            imageView.setHierarchy(hierarchy);
            imageView.setController(controller); */
        }
        return new NativeElementView(context, child, false, imageView);
    }

    public static class TransformImage implements Transformation {

        final int width;
        final int height;
        final String src;
        final ImageView imageView;


        public TransformImage(int width, int height, String src, ImageView imageView) {
            this.width = width;
            this.height = height;
            this.src = src;
            this.imageView = imageView;
        }

        @Override
        public Bitmap transform(Bitmap bitmap) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            int loc [] = new int[2];
            imageView.getLocationOnScreen(loc);
            float density = imageView.getResources().getDisplayMetrics().density;
            int maxWidth = imageView.getResources().getDisplayMetrics().widthPixels
                    - loc[0];
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
            return "transformation" + " desiredWidth";
        }
    }

    static NativeElementView createImg(Context context, Element child) {
        // TODO: Focus / click handling for buttons in a link?
        String src = child.getAttributeValue("src");
        final GifImageView imageView = new GifImageView(context);
        if (src != null) {
            final int width = child.getAttributeInt("width", WRAP_CONTENT);
            final int height = child.getAttributeInt("height", WRAP_CONTENT);
            imageView.setLayoutParams(new LayoutParams(width, height));
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_START);
            Picasso.with(context)
                    .load(src)
                    .placeholder(R.drawable.ic_image_crop_original)
                    .transform(new TransformImage(width, height, src, imageView))
                    .into(imageView);
        }
        return new NativeElementView(context, child, false, imageView);
    }

    public static View createInclude(Context context, Element element) {
        String layoutName = element.getAttributeValue("layout");
        int lid = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(lid, null);
        return new NativeElementView(context, element, false, view);
    }

    static NativeElementView createInput(final Context context, final Element element) {
        View content = null; // null should not be necessary
        int textSize = element.getScaledPx(Style.FONT_SIZE);
        String type = element.getAttributeValue("type");
        if ("checkbox".equals(type)) {
            content = new CheckBox(context);
        } else if ("radio".equals(type)) {
            content = new RadioButton(context);
        } else if ("submit".equals(type) || "reset".equals(type)) {
            String value = element.getAttributeValue("value");
            Button button = new Button(context);
            button.setText(value == null ? type : value);
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            content = button;
        } else if("text".equals(type) || "password".equals(type)) {
            EditText editText = new EditText(context);
            if ("password".equals(type)) {
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            content = editText;
        } else if("hidden".equals(type)) {
            TextView textView = new TextView(context);
            textView.setVisibility(GONE);
            content = textView;
        } else {
            content = new View(context); // unsupported
        }
        if("true".equals(element.getAttributeValue("hidden"))) {
            content.setVisibility(GONE);
        }
        NativeElementView result = new NativeElementView(context, element, false, content);
        if (content instanceof Button) {
            content.setOnClickListener(result);
        }
        result.reset();
        return result;
    }


    static NativeElementView createSelect(final Context context, final Element element) {
        boolean multiple = element.getAttributeBoolean("multiple");
        ArrayList<Element> options = new ArrayList<Element>();
        for (int i = 0; i < element.getChildCount(); i++) {
            if (element.getChildType(i) == Element.ELEMENT) {
                Element child = element.getElement(i);
                if (child.getName().equals("option")) {
                    options.add(child);
                }
            }
        }
        SelectAdapter adapter = new SelectAdapter(context, element, multiple, options);
        adapter.reset(); // needed here: performs measurement
        adapter.view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        if (!element.getComputedStyle().isSet(Style.WIDTH)) {
            element.getComputedStyle().set(Style.WIDTH, Math.round(
                    (adapter.width + adapter.view.getMeasuredWidth()) * 1000 / element.htmlView.pixelScale), Style.PX);
        }
        if (!element.getComputedStyle().isSet(Style.HEIGHT)) {
            element.getComputedStyle().set(Style.HEIGHT, Math.round(element.getFont(context).getFontMetricsInt(null) *
                    (1 + element.getAttributeInt("size", 1) * 2000 / element.htmlView.pixelScale)), Style.PX);
        }
        return new NativeElementView(context, element, false, adapter.view);
    }


    static NativeElementView createTextArea(final Context context, final Element element) {
        int textSize = element.getScaledPx(Style.FONT_SIZE);
        EditText editText = new EditText(context);
        editText.setSingleLine(false);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        editText.setGravity(Gravity.TOP);
        // TODO: Calculate lines based on height if fixed.
        editText.setLines(element.getAttributeInt("rows", 2));
        editText.setVerticalScrollBarEnabled(true);
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(params);
        NativeElementView result = new NativeElementView(context, element, false, editText);
        result.reset();
        return result;
    }


    static void reset(Element element) {
        if (element.nativeView != null) {
            ((NativeElementView) element.nativeView.getParent()).reset();
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (element.getChildType(i) == Element.ELEMENT) {
                reset(element.getElement(i));
            }
        }
    }

    static void resetRadioGroup(Element element, String name) {
        if (element.nativeView instanceof RadioButton &&
                name.equals(element.getAttributeValue("name"))) {
            ((RadioButton) element.nativeView).setChecked(false);
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (element.getChildType(i) == Element.ELEMENT) {
                resetRadioGroup(element.getElement(i), name);
            }
        }
    }

    static void readValues(Element element, List<Map.Entry<String, String>> result) {
        if (element.nativeView != null) {
            ((NativeElementView) element.nativeView.getParent()).readValue(result);
        } else if ("input".equals(element.getName())) {
            // Hidden element data.
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (element.getChildType(i) == Element.ELEMENT) {
                readValues(element.getElement(i), result);
            }
        }
    }

    NativeElementView(Context context, Element element, boolean traversable, View content) {
        super(context, element, traversable);
        this.nativeView = element.nativeView = content;
        addView(content);
    }

    /**
     * If name is null, reset this view to the state specified in the DOM.
     * Otherwise, if the name matches, reset to unchecked.
     */
    void reset() {
        if (nativeView instanceof Checkable) {
            ((Checkable) nativeView).setChecked(element.getAttributeValue("checked") != null);
        } else if (nativeView instanceof TextView) {
            TextView editText = (TextView) nativeView;
            if (element.getName().equals("textarea")) {
                editText.setText(element.getText());
            } else {
                editText.setText(element.getAttributeValue("value"));
            }
        } else if (nativeView instanceof AdapterView<?>) {
            Object adapter = ((AdapterView<?>) nativeView).getAdapter();
            if (adapter instanceof SelectAdapter) {
                SelectAdapter select = (SelectAdapter) adapter;
                select.reset();
            }
        }
        invalidate();
    }

    /**
     * If this view has an input value, set it in the given map.
     */
    void readValue(List<Map.Entry<String, String>> result) {
        String name = element.getAttributeValue("name");
        if (name != null) {
            if (nativeView instanceof Checkable && ((Checkable) nativeView).isChecked()) {
                String value = element.getAttributeValue("value");
                addEntry(result, name, value == null ? "" : value);
            } else if (nativeView instanceof EditText) {
                addEntry(result, name, ((EditText) nativeView).getText().toString());
            } else if (nativeView instanceof AdapterView<?>) {
                Object adapter = ((AdapterView<?>) nativeView).getAdapter();
                if (adapter instanceof SelectAdapter) {
                    ((SelectAdapter) adapter).readValue(result);
                }
            }
        }
    }


    @Override
    void measureBlock(int outerMaxWidth, int viewportWidth,
                      LayoutContext parentLayoutContext, boolean shrinkWrap) {
        Style style = element.getComputedStyle();
        boolean fixedWidth = style.isLengthFixedOrPercent(Style.WIDTH);
        boolean fixedHeight = isHeightFixed();
        if (!(fixedHeight && fixedWidth)) {
            nativeView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }
        int innerWidth = fixedWidth ? element.getScaledPx(Style.WIDTH, outerMaxWidth)
                : nativeView.getMeasuredWidth();
        int innerHeight = fixedHeight ? getFixedInnerHeight() : nativeView.getMeasuredHeight();
        if (fixedHeight || fixedWidth) {
            nativeView.measure(MeasureSpec.EXACTLY | innerWidth, MeasureSpec.EXACTLY | innerHeight);
        }
        marginLeft = element.getScaledPx(Style.MARGIN_LEFT, outerMaxWidth);
        marginRight = element.getScaledPx(Style.MARGIN_RIGHT, outerMaxWidth);

        boxHeight = innerHeight +
                element.getScaledPx(Style.MARGIN_TOP) + element.getScaledPx(Style.MARGIN_BOTTOM) +
                element.getScaledPx(Style.BORDER_TOP_WIDTH) +
                element.getScaledPx(Style.BORDER_BOTTOM_WIDTH) +
                element.getScaledPx(Style.PADDING_TOP) + element.getScaledPx(Style.PADDING_BOTTOM);

        boxWidth = innerWidth +
                element.getScaledPx(Style.MARGIN_LEFT) + element.getScaledPx(Style.MARGIN_RIGHT) +
                element.getScaledPx(Style.BORDER_LEFT_WIDTH) +
                element.getScaledPx(Style.BORDER_RIGHT_WIDTH) +
                element.getScaledPx(Style.PADDING_LEFT) + element.getScaledPx(Style.PADDING_RIGHT);

        setMeasuredDimension(boxWidth, boxHeight);
        if (parentLayoutContext != null) {
            parentLayoutContext.advance(boxHeight);
        }
    }

    @Override
    void calculateWidth(int containerWidth) {
        int border = element.getScaledPx(Style.BORDER_LEFT_WIDTH) +
                element.getScaledPx(Style.BORDER_RIGHT_WIDTH) +
                element.getScaledPx(Style.MARGIN_LEFT) + element.getScaledPx(Style.MARGIN_RIGHT) +
                element.getScaledPx(Style.PADDING_LEFT) + element.getScaledPx(Style.PADDING_RIGHT);

        Style style = element.getComputedStyle();
        int innerWidth;
        if (style.isLengthFixedOrPercent(Style.WIDTH)) {
            innerWidth = element.getScaledPx(Style.WIDTH, containerWidth);
        } else {
            nativeView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            innerWidth = nativeView.getMeasuredWidth();
        }
        minimumWidth = maximumWidth = innerWidth + border;
        widthValid = true;
    }

    static Element findForm(Element current) {
        while (current != null && !"form".equals(current.getName())) {
            current = current.getParent();
        }
        return current;
    }

    @Override
    public void onClick(View v) {
        Log.d("HtmlView", "onClick " + element.toString());
        if ("input".equals(element.getName())) {
            String type = element.getAttributeValue("type");
            Element form = findForm(element);
            if (form != null) {
                if ("reset".equals(type)) {
                    reset(form);
                } else if ("submit".equals(type)) {
                    List<Map.Entry<String, String>> formData = new ArrayList<Map.Entry<String, String>>();
                    String name = element.getAttributeValue("name");
                    String value = element.getAttributeValue("value");
                    if (name != null && value != null) {
                        addEntry(formData, name, value);
                    }
                    readValues(form, formData);
                    URI uri = element.htmlView.getBaseUrl();
                    String action = element.getAttributeValue("action");
                    if (action != null) {
                        uri = uri.resolve(action);
                    }
                    String method = element.getAttributeValue("method");
                    if (method == null) {
                        method = "get";
                    }
                    element.htmlView.requestHandler.submitForm(element.htmlView, element, uri, "post".equalsIgnoreCase(method), formData);
                } else if ("radio".equals(type)) {
                    String name = element.getAttributeValue("name");
                    if (name != null) {
                        resetRadioGroup(form, name);
                    }
                    ((Checkable) nativeView).setChecked(true);
                }
            }
        }
    }

    static class SelectAdapter extends ArrayAdapter<Element> {
        BitSet selected;
        boolean multiple;
        ListView listView;
        Spinner spinner;
        Element select;
        private int width;
        // Why is AdapterView parameterized in the first place?
        @SuppressWarnings("rawtypes")
        AdapterView view;
        // Needed because notifyDataSetChanged does not work as expected, see below.
        List<View> knownViews = new ArrayList<View>();

        SelectAdapter(Context context, Element select, boolean multiple, ArrayList<Element> options) {
            super(context, android.R.layout.simple_list_item_1, options);
            this.multiple = multiple;
            if (multiple) {
                this.listView = new ListView(context);
                this.listView.setAdapter(this);
                this.view = listView;
                selected = new BitSet();
            } else {
                this.spinner = new Spinner(context);
                this.spinner.setAdapter(this);
                this.view = spinner;
            }
            this.select = select;
        }

        void readValue(List<Map.Entry<String, String>> result) {
            if (multiple) {
                for (int i = 0; i < getCount(); i++) {
                    if (selected.get(i)) {
                        readOptionValue(getItem(i), select.getAttributeValue("name"), result);
                    }
                }
            } else {
                if (spinner.getSelectedItemPosition() >= 0) {
                    readOptionValue(getItem(spinner.getSelectedItemPosition()),
                            select.getAttributeValue("name"), result);
                }
            }
        }

        private void readOptionValue(Element option, String name, List<Map.Entry<String, String>> result) {
            String value = option.getAttributeValue("value");
            addEntry(result, name, value == null ? option.getText() : value);
        }

        @Override
        public View getDropDownView(final int position, View reuse, final ViewGroup parent) {
            // TODO: Why is this nonsense needed :((
            TextView tv = reuse instanceof TextView ? (TextView) reuse : new TextView(getContext()) {
                public boolean onTouchEvent(MotionEvent event) {
                    spinner.setSelection(position, true);
                    return super.onTouchEvent(event);
                }
            };
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, select.getScaledPx(Style.FONT_SIZE));
            tv.setText(getItem(position).getText());
            int ts = (int) tv.getTextSize();
            tv.setPadding(ts / 4, ts / 2, ts / 4, ts / 2);
            return tv;
        }

        @Override
        public View getView(final int position, View reuse, final ViewGroup group) {
            TextView tv = reuse instanceof TextView ? (TextView) reuse : new TextView(getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, select.getScaledPx(Style.FONT_SIZE));
            tv.setText(getItem(position).getText());
            if (multiple) {
                while (knownViews.size() <= position) {
                    knownViews.add(null);
                }
                knownViews.set(position, tv);
                int ts = (int) tv.getTextSize();
                tv.setPadding(ts / 4, ts / 2, ts / 4, ts / 2);
                tv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selected.set(position, !selected.get(position));
                        applySelection(v, position);
                    }
                });
            }
            return tv;
        }

        // This should not be needed, but notifyDataSetChanged does not really work as
        // expected...
        public void applySelection(View v, int position) {
            v.setBackgroundColor(selected.get(position) ? 0xff8888ff : 0);
            v.invalidate();
        }

        public void reset() {
            if (multiple) {
                selected.clear();
            } else {
                spinner.setSelection(0, true);
            }
            width = 0;
            for (int i = 0; i < getCount(); i++) {
                Element option = getItem(i);
                String text = option.getText();
                int wi = HtmlUtils.measureText(select.getFont(getContext()), text, 0, text.length());
                if (wi > width) {
                    width = wi;
                }

                if (option.getAttributeBoolean("selected")) {
                    if (multiple) {
                        selected.set(getCount());
                    } else {
                        spinner.setSelection(i, true);
                    }
                }
            }
            notifyDataSetChanged();
            // Sad... :(
            // view.setAdapter (recommended on stackOverflow) hides the list.
            if (multiple) {
                for (int i = 0; i < knownViews.size(); i++) {
                    View v = knownViews.get(i);
                    if (v != null) {
                        applySelection(v, i);
                    }
                }
            }
        }
    }

    static void addEntry(List<Map.Entry<String, String>> result, final String key, final String value) {
        result.add(new Map.Entry<String, String>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public String toString() {
                return key + '=' + value;
            }

            @Override
            public String setValue(String object) {
                throw new UnsupportedOperationException();
            }
        });
    }

}
