package com.ledapp.scroller.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

/**
 * 屏幕四周的流光边框，移植自原版 APP 的 EdgeLightView。
 * 使用 SweepGradient 绘制一圈可以旋转的颜色环，颜色与粗细可配置。
 */
public class EdgeLightView extends View {

    private final Path path = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final RectF rect = new RectF();

    private SweepGradient sweepGradient;
    private int[] colors = {-65536, 0xFFFF0000, -16711936, -16537100, -16776961, -65281, -65536};
    private float[] positions = new float[0];

    private float angle;
    private float animSpeed = 2f;
    private float strokeSize = 8f;
    private float halfStroke = 4f;
    private float cornerRadius = 0.2f;

    private boolean running;

    public EdgeLightView(Context context) {
        this(context, null);
    }

    public EdgeLightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    private void buildPositions() {
        positions = new float[colors.length];
        float step = colors.length > 1 ? 1f / (colors.length - 1) : 1f;
        for (int i = 0; i < colors.length; i++) {
            positions[i] = i * step;
        }
        if (positions.length > 0) {
            positions[positions.length - 1] = 1f;
        }
    }

    /** 生成圆角矩形路径（使用半径像素值）。 */
    private void buildPath() {
        float w = getWidth();
        float h = getHeight();
        float inset = halfStroke;
        float radius = cornerRadius > 0 && cornerRadius < 1 ? cornerRadius * Math.min(w, h) : cornerRadius;
        rect.set(inset, inset, w - inset, h - inset);
        path.reset();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
    }

    private void buildGradient() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        sweepGradient = new SweepGradient(getWidth() * 0.5f, getHeight() * 0.5f, colors, positions);
    }

    private void updateMatrix() {
        angle = (angle + animSpeed) % 360f;
        matrix.setRotate(angle, getWidth() * 0.5f, getHeight() * 0.5f);
        if (sweepGradient != null) {
            sweepGradient.setLocalMatrix(matrix);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (colors.length == 0) {
            return;
        }
        paint.setShader(sweepGradient != null ? sweepGradient : null);
        if (sweepGradient == null) {
            paint.setColor(colors[0]);
        }
        paint.setStrokeWidth(strokeSize);
        canvas.drawPath(path, paint);
        updateMatrix();
        if (running) {
            postInvalidateDelayed(30L);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath();
        buildPositions();
        buildGradient();
    }

    public void startAnimation() {
        running = true;
        invalidate();
    }

    public void stopAnimation() {
        running = false;
        invalidate();
    }

    public void setEdgeColors(int... edgeColors) {
        if (edgeColors == null || edgeColors.length == 0) {
            edgeColors = new int[]{-65536};
        }
        colors = edgeColors;
        buildPositions();
        buildGradient();
        invalidate();
    }

    public void setStrokeSize(float size) {
        strokeSize = size;
        halfStroke = size / 2f;
        paint.setStrokeWidth(size);
        buildPath();
        invalidate();
    }

    /** 圆角半径（像素） */
    public void setCornerRadius(float radius) {
        post(() -> {
            cornerRadius = radius;
            buildPath();
            invalidate();
        });
    }

    public void setAnimSpeed(float speed) {
        animSpeed = speed;
        invalidate();
    }
}