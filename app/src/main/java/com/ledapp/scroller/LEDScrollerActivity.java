package com.ledapp.scroller;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.ledapp.scroller.widget.BackgroundVideoView;
import com.ledapp.scroller.widget.EdgeLightView;
import com.ledapp.scroller.widget.LEDScrollerView;

/**
 * 全屏 LED 播放页（对应原版 LEDScrollerActivity）：
 * 进入后全屏横屏显示 LED 文本（可叠加背景视频/图片），点击屏幕显示关闭按钮，5 秒后自动隐藏。
 */
public class LEDScrollerActivity extends AppCompatActivity {

    private LEDScrollerView ledView;
    private EdgeLightView edgeLight;
    private BackgroundVideoView videoView;
    private ImageView imageView;
    private ImageView closeView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideCloseRunnable = new Runnable() {
        @Override
        public void run() {
            if (closeView != null) {
                closeView.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_led_scroller);

        ledView = findViewById(R.id.led_scroller_view);
        edgeLight = findViewById(R.id.edge_light_view);
        videoView = findViewById(R.id.background_video);
        imageView = findViewById(R.id.preview_background_video);
        closeView = findViewById(R.id.close_button);

        Settings settings = Settings.get(this);

        int[] fontColors = Settings.toIntColors(settings.getFontColors());
        ledView.setTextColor(fontColors.length > 0 ? fontColors : new int[]{getColor(R.color.led_default_text)});
        int[] bgColors = Settings.toIntColors(settings.getBgColors());
        ledView.setBgColor(bgColors.length > 0 ? bgColors : new int[]{Color.BLACK});

        int[] edgeColors = Settings.toIntColors(settings.getEdgeColors());
        edgeLight.setEdgeColors(edgeColors.length > 0 ? edgeColors : new int[]{Color.RED});
        edgeLight.setCornerRadius(settings.getEdgeRadiusFraction());
        edgeLight.setStrokeSize(settings.getEdgeSize());
        edgeLight.setVisibility(settings.isEdgeEnable() ? View.VISIBLE : View.GONE);

        String direction = settings.getDirection();
        if (Settings.DIR_PAUSE.equals(direction)) {
            ledView.setDirection(LEDScrollerView.DIRECTION_CENTER);
        } else if (Settings.DIR_RTL.equals(direction)) {
            ledView.setDirection(LEDScrollerView.DIRECTION_RTL);
        } else {
            ledView.setDirection(LEDScrollerView.DIRECTION_LTR);
        }
        ledView.setFontPercent(settings.getFontSize());
        ledView.setSpeed(settings.getSpeed());
        ledView.setBlinkPercent(settings.getBlink());
        ledView.setMarkSpace(settings.getLedSize());
        ledView.setTextFont(Fonts.get(this, settings.getFontType()));
        ledView.setText(settings.getLedText());
        // 背景由 LED 控件合成，和主界面预览保持一致
        ledView.setBackgroundVideo(videoView);
        ledView.setBackgroundImage(imageView);

        setBrightness(settings.getBrightness());
        applyBackground(settings);

        if (closeView != null) {
            closeView.setOnClickListener(v -> finish());
        }
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.setOnClickListener(v -> showClose());
        }
        showClose();
    }

    /** 按设置应用背景：视频模式播视频，图片模式显示图片，未选择时保持纯色。 */
    private void applyBackground(Settings settings) {
        boolean videoMode = settings.getEffectMode() == 0;
        String videoUrl = settings.getVideoEffectUrl();
        String imageUrl = settings.getImageEffectUrl();

        if (videoMode && videoUrl != null) {
            videoView.setVideoUri(videoUrl);
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            imageView.setImageDrawable(null);
        } else if (!videoMode && imageUrl != null) {
            Bitmap bitmap = Backgrounds.decode(this, imageUrl, 1920, 1920);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
            videoView.release();
            videoView.setVideoUri(null);
            videoView.setVisibility(View.GONE);
        } else {
            videoView.release();
            videoView.setVideoUri(null);
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
        }
        ledView.invalidate();
    }

    private void showClose() {
        if (closeView == null) {
            return;
        }
        closeView.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideCloseRunnable);
        handler.postDelayed(hideCloseRunnable, 5000);
    }

    private void setBrightness(float brightness) {
        try {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.screenBrightness = Math.max(0.01f, Math.min(1f, brightness));
            getWindow().setAttributes(attributes);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSystemUi();
        ledView.start();
        videoView.start();
        if (edgeLight.getVisibility() == View.VISIBLE) {
            edgeLight.startAnimation();
            edgeLight.setAnimSpeed(360f / Math.max(1, ledView.getFrameCount()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ledView.stop();
        edgeLight.stopAnimation();
        videoView.pause();
    }

    private void updateSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        videoView.release();
        super.onDestroy();
    }
}