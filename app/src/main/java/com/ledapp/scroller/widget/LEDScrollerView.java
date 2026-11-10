package com.ledapp.scroller.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.TextureView;
import android.view.View;

/**
 * LED 滚动显示控件，移植自原版 APP 的 LEDScrollerView。
 * <p>
 * 绘制顺序（与原版一致）：渐变背景 -> 背景图层（视频/图片）-> 渐变文字 -> 点阵遮罩，
 * 遮罩会把文字和背景一起“切”成 LED 灯珠，从而实现原版的背景动画效果。
 * 支持三种方向：静止居中 / 左到右 / 右到左，以及闪烁效果。
 */
public class LEDScrollerView extends View implements Choreographer.FrameCallback {

    private static final int DEFAULT_TEXT_COLOR = 0xFFCF8933;
    private static final int DEFAULT_BG_COLOR = Color.BLACK;

    /** 把 ALPHA_8 遮罩里的不透明部分统一染成黑色 */
    private static final android.graphics.ColorFilter MASK_COLOR_FILTER =
            new android.graphics.PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String text = "";
    private boolean isRunning;

    private float textX;
    private float textWidth;
    private float textY;

    private int direction = DIRECTION_LTR;
    private float scrollSpeed;
    private int blinkValue;

    private Typeface typeface = Typeface.DEFAULT_BOLD;
    private float fontPercent;
    private int markSpace;

    private int[] textColors = {DEFAULT_TEXT_COLOR};
    private int[] bgColors = {DEFAULT_BG_COLOR};
    private LinearGradient textGradient;
    private LinearGradient bgGradient;

    private float cornerRadius;

    /** 背景视频（原版主界面的 VideoView 层），由本控件在绘制时合成。 */
    private TextureView backgroundVideo;

    /** 背景图片（原版主界面的 ImageFilterView 层），由本控件在绘制时合成。 */
    private View backgroundImage;

    /** 点阵遮罩缓存 */
    private Bitmap maskBitmap;
    private int maskWidth;
    private int maskHeight;
    private int maskStep = -1;

    public static final int DIRECTION_CENTER = 0;
    public static final int DIRECTION_LTR = 1;
    public static final int DIRECTION_RTL = 2;

    public LEDScrollerView(Context context) {
        this(context, null);
    }

    public LEDScrollerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);
        textPaint.setDither(true);
        textPaint.setFilterBitmap(true);
        textPaint.setColor(DEFAULT_TEXT_COLOR);
        textPaint.setTextSize(0f);
        bgPaint.setDither(true);
        bgPaint.setFilterBitmap(true);
        bgPaint.setColor(DEFAULT_BG_COLOR);
        maskPaint.setFilterBitmap(false);
        setClipToOutline(true);
        setLayerType(View.LAYER_TYPE_NONE, null);
    }

    /** 绑定背景图片层（图片模式下与视频一样参与点阵遮罩）。 */
    public void setBackgroundImage(View image) {
        backgroundImage = image;
        invalidate();
    }

    /**
     * 绑定背景视频层：每帧把 TextureView 的当前画面画到本控件上，
     * 这样背景会和文字一起被点阵遮罩覆盖（与原版叠加顺序一致）。
     */
    public void setBackgroundVideo(TextureView video) {
        backgroundVideo = video;
        invalidate();
    }

    // ------------------------------------------------------------ 计算文本

    /** 字体大小 = 字体比例 * 屏幕短边 */
    private float getLedFontSize() {
        return fontPercent * Math.min(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
    }

    private StaticLayout buildStaticLayout() {
        String[] lines = text.split("\n", -1);
        float max = 0f;
        for (String line : lines) {
            max = Math.max(max, textPaint.measureText(line));
        }
        int width = max > 0 ? (int) max : getWidth();

        if (Build.VERSION.SDK_INT >= 23) {
            return StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, Math.max(1, width))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(true)
                    .build();
        }
        //noinspection deprecation
        return new StaticLayout(text, textPaint, Math.max(1, width), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true);
    }

    // ------------------------------------------------------------ 颜色

    public void setTextColor(int... colors) {
        if (colors.length == 0) {
            colors = new int[]{DEFAULT_TEXT_COLOR};
        }
        textColors = colors;
        post(this::applyTextColor);
    }

    public void setBgColor(int... colors) {
        if (colors.length == 0) {
            colors = new int[]{0};
        }
        bgColors = colors;
        post(this::applyBgColor);
    }

    private void applyTextColor() {
        int[] colors = textColors;
        if (colors.length < 2) {
            textGradient = null;
            textPaint.setColor(colors.length > 0 ? colors[0] : DEFAULT_TEXT_COLOR);
            textPaint.setShader(null);
            return;
        }
        float[] positions = new float[colors.length];
        float step = 1f / colors.length;
        for (int i = 0; i < colors.length; i++) {
            positions[i] = i * step;
        }
        positions[positions.length - 1] = 1f;
        textGradient = new LinearGradient(0f, getHeight() * 0.5f, Math.max(1, textWidth),
                getHeight() * 0.5f, colors, positions, Shader.TileMode.CLAMP);
    }

    private void applyBgColor() {
        int[] colors = bgColors;
        if (colors.length < 2) {
            bgGradient = null;
            bgPaint.setColor(colors.length > 0 ? colors[0] : DEFAULT_BG_COLOR);
            bgPaint.setShader(null);
            return;
        }
        float[] positions = new float[colors.length];
        float step = 1f / (colors.length - 1);
        for (int i = 0; i < colors.length; i++) {
            positions[i] = i * step;
        }
        bgGradient = new LinearGradient(0f, getHeight() * 0.5f, getWidth(), getHeight() * 0.5f,
                colors, positions, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------ 点阵遮罩

    private Bitmap getMaskBitmap(int width, int height, int step) {
        if (step <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        if (maskBitmap != null && maskWidth == width && maskHeight == height && maskStep == step) {
            return maskBitmap;
        }
        maskBitmap = createMaskBitmap(width, height, step);
        maskWidth = width;
        maskHeight = height;
        maskStep = step;
        return maskBitmap;
    }

    /**
     * 生成点阵遮罩：整张画布为黑色，只在灯珠（页面倾斜 45° 排列的圆点）位置挖空，
     * 挖空处透出下面的文字与背景。
     */
    private static Bitmap createMaskBitmap(int width, int height, int step) {
        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        } catch (OutOfMemoryError e) {
            return null;
        }
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // ALPHA_8：0 = 灯珠（透出文字/背景），255 = 黑色遮罩
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        float radius = Math.max(1.2f, step * 0.34f);
        for (int y = -step; y < height + step; y += step) {
            for (int x = -step; x < width + step; x += step) {
                canvas.save();
                canvas.rotate(45f, x, y);
                canvas.drawCircle(x, y, radius, paint);
                canvas.restore();
            }
        }
        return bitmap;
    }

    // ------------------------------------------------------------ 绘制

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. 背景（圆角矩形 + 渐变）
        float radius = cornerRadius;
        if (bgGradient != null) {
            bgPaint.setShader(bgGradient);
        }
        RectF bgRect = new RectF(0f, 0f, getWidth(), getHeight());
        canvas.drawRoundRect(bgRect, radius, radius, bgPaint);

        // 2. 背景图层（视频画面）
        drawBackgroundVideo(canvas);

        // 3. 文字
        textPaint.setTextSize(getLedFontSize());
        textPaint.setTypeface(typeface);
        textPaint.setShader(textGradient);
        drawStaticText(canvas);

        // 4. 点阵遮罩（灯珠处透出文字与背景）
        Bitmap mask = getMaskBitmap(getWidth(), getHeight(), markSpace);
        if (mask != null) {
            maskPaint.setColorFilter(MASK_COLOR_FILTER);
            canvas.drawBitmap(mask, 0f, 0f, maskPaint);
            maskPaint.setColorFilter(null);
        }

        updateScroll();
        textY = (getHeight() / 2f) - (measureStatic().getHeight() / 2f);
    }

    private void drawBackgroundVideo(Canvas canvas) {
        View image = backgroundImage;
        if (image != null && image.getVisibility() == View.VISIBLE && getWidth() > 0 && getHeight() > 0) {
            int save = canvas.save();
            canvas.scale(getWidth() / (float) Math.max(1, image.getWidth()),
                    getHeight() / (float) Math.max(1, image.getHeight()));
            image.draw(canvas);
            canvas.restoreToCount(save);
            return;
        }
        TextureView video = backgroundVideo;
        if (video == null || video.getVisibility() != View.VISIBLE || !video.isAvailable()) {
            return;
        }
        try {
            Bitmap frame = video.getBitmap(Math.max(1, getWidth() / 2), Math.max(1, getHeight() / 2));
            if (frame != null) {
                canvas.drawBitmap(frame, null, new RectF(0, 0, getWidth(), getHeight()), bitmapPaint);
            }
        } catch (Exception ignored) {
        }
    }

    private StaticLayout measureStatic() {
        textPaint.setTextSize(getLedFontSize());
        textPaint.setTypeface(typeface);
        return buildStaticLayout();
    }

    private void drawStaticText(Canvas canvas) {
        StaticLayout layout = measureStatic();
        int save = canvas.save();
        canvas.translate(textX, textY);
        layout.draw(canvas);
        canvas.restoreToCount(save);
    }

    private void updateScroll() {
        StaticLayout layout = measureStatic();
        textWidth = layout.getWidth();
        switch (direction) {
            case DIRECTION_CENTER:
                textX = (getWidth() - textWidth) / 2f;
                break;
            case DIRECTION_LTR:
                if (textX <= getWidth()) {
                    textX += scrollSpeed;
                } else {
                    textX = -textWidth;
                }
                break;
            case DIRECTION_RTL:
            default:
                if (textX >= -textWidth) {
                    textX -= scrollSpeed;
                } else {
                    textX = getWidth();
                }
                break;
        }
    }

    // ------------------------------------------------------------ 帧回调

    @Override
    public void doFrame(long frameTimeNanos) {
        if (isRunning) {
            Choreographer.getInstance().postFrameCallback(this);
        }
        if (blinkValue > 0) {
            blinkValue = (blinkValue + 1) % 280;
        }
        postInvalidate();
    }

    public void start() {
        try {
            isRunning = true;
            Choreographer.getInstance().postFrameCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            isRunning = false;
            Choreographer.getInstance().removeFrameCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maskBitmap = null;
        resetPosition();
        applyTextColor();
        applyBgColor();
    }

    private void resetPosition() {
        StaticLayout layout = measureStatic();
        textWidth = layout.getWidth();
        textY = (getHeight() / 2f) - (layout.getHeight() / 2f);
        switch (direction) {
            case DIRECTION_CENTER:
                textX = (getWidth() - textWidth) / 2f;
                break;
            case DIRECTION_LTR:
                textX = -textWidth + getWidth();
                break;
            case DIRECTION_RTL:
            default:
                textX = getWidth();
                break;
        }
    }

    // ------------------------------------------------------------ 属性

    public void setText(String text) {
        this.text = text == null ? "" : text;
        resetPosition();
        invalidate();
    }

    public int getFrameCount() {
        if (direction == DIRECTION_CENTER) {
            return 1;
        }
        return (int) (((getWidth() + textWidth) / Math.max(scrollSpeed, 0.01f)) + 2);
    }

    public void setDirection(int direction) {
        this.direction = direction;
        resetPosition();
    }

    public void setFontPercent(float percent) {
        this.fontPercent = percent;
    }

    public void setBlinkPercent(float percent) {
        this.blinkValue = Math.round(percent * 10) << 1;
    }

    public void setMarkSpace(int step) {
        this.markSpace = step;
        maskBitmap = null;
        invalidate();
    }

    public void setSpeed(float speed) {
        float density = getResources().getDisplayMetrics().density;
        this.scrollSpeed = Math.max(1f, 8 * density * speed);
    }

    public void setTextFont(Typeface typeface) {
        this.typeface = typeface;
        resetPosition();
    }

    public void setCornerRadius(float radius) {
        post(() -> {
            cornerRadius = radius;
            invalidate();
        });
    }

    public void setCornerRadiusInt(int radius) {
        post(() -> {
            cornerRadius = radius;
            invalidate();
        });
    }
}