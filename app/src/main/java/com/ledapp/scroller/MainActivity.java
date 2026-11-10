package com.ledapp.scroller;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.ledapp.scroller.dialog.ColorPickerDialog;
import com.ledapp.scroller.dialog.FontChooseDialog;
import com.ledapp.scroller.widget.BackgroundVideoView;
import com.ledapp.scroller.widget.EdgeLightView;
import com.ledapp.scroller.widget.LEDScrollerView;
import com.ledapp.scroller.widget.SegmentTabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面。
 * <p>功能与原版 LED Scroller 一致：输入文字 -> 实时预览 LED 点阵效果（可带背景视频/图片动画）
 * -> 调整方向 / 速度 / 闪烁 / 亮度 / LED 大小 / 字体 / 颜色 / 边缘灯光 -> 全屏播放。</p>
 */
public class MainActivity extends AppCompatActivity {

    /** 方向按钮选中时的底色（与原版一致） */
    private static final int SELECTED_COLOR = 0xFF006BDE;
    /** 方向按钮未选中时的图标色 */
    private static final int NORMAL_COLOR = 0xFFE4E6EB;

    /** 各设置项的滑动条 id（与 activity_main.xml 中的 @+id 一一对应） */
    private static final int SEEK_SPEED = R.id.speed_seek;
    private static final int SEEK_BLINK = R.id.blink_seek;
    private static final int SEEK_BRIGHTNESS = R.id.brightness_seek;
    private static final int SEEK_LED_SIZE = R.id.led_size_seek;
    private static final int SEEK_FONT_SIZE = R.id.font_size_seek;
    private static final int SEEK_EDGE_RADIUS = R.id.edge_radius_seek;
    private static final int SEEK_EDGE_SIZE = R.id.edge_size_seek;

    private Settings settings;

    // -------- 预览区 --------
    private LEDScrollerView ledView;       // LED 点阵文字
    private EdgeLightView edgeLight;       // 边缘流光
    private BackgroundVideoView videoView; // 背景视频
    private ImageView imageView;           // 背景图片

    // -------- 输入区 --------
    private EditText input;                // 文字输入框
    private View clearLayout;              // 清除按钮
    private ImageView menuIcon;            // 菜单按钮
    private ImageView emojiIcon;           // 表情按钮

    // -------- 方向 --------
    private ImageView dirLtr;              // 左到右
    private ImageView dirPause;            // 静止
    private ImageView dirRtl;              // 右到左

    // -------- 背景效果 --------
    private SegmentTabLayout effectTab;    // 视频 / 图片切换
    private LinearLayout videoRow;         // 视频素材行
    private LinearLayout imageRow;         // 图片素材行
    private RecyclerView videoList;        // 视频素材列表
    private RecyclerView imageList;        // 图片素材列表
    private EffectAdapter videoAdapter;
    private EffectAdapter imageAdapter;

    private TextView fontTypeValue;        // 当前字体名
    private DrawerLayout drawer;           // 主抽屉容器

    /** 当前选择的颜色（ARGB），设置里以字符串保存 */
    private final List<Integer> fontColors = new ArrayList<>(); // 文字颜色（支持渐变）
    private final List<Integer> bgColors = new ArrayList<>();   // 背景颜色（支持渐变）
    private final List<Integer> edgeColors = new ArrayList<>(); // 边缘流光颜色

    /** 从相册导入背景素材 */
    private ActivityResultLauncher<String> pickVideo;
    private ActivityResultLauncher<String> pickImage;

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = s == null ? "" : s.toString();
            settings.setLedText(text);
            ledView.setText(text);
            if (clearLayout != null) {
                clearLayout.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        settings = Settings.get(this);

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        registerPickers();

        setContentView(R.layout.activity_main);
        bindViews();
        applyStoredValues();
        setupListeners();
        setupBackground();
        setBrightness(settings.getBrightness());
    }

    // ------------------------------------------------------------------ 选择背景

    private void registerPickers() {
        pickVideo = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String local = Backgrounds.persist(this, uri, true);
                if (local != null) {
                    settings.selectVideoEffect(local);
                    refreshPreviewItem();
                    applyBackground();
                }
            }
        });
        pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String local = Backgrounds.persist(this, uri, false);
                if (local != null) {
                    settings.selectImageEffect(local);
                    refreshPreviewItem();
                    applyBackground();
                }
            }
        });
    }

    // ------------------------------------------------------------------ 视图绑定

    private void bindViews() {
        drawer = findViewById(R.id.main_drawer);
        ledView = findViewById(R.id.led_scroller_view);
        edgeLight = findViewById(R.id.edge_light_view);
        input = findViewById(R.id.input_text);
        clearLayout = findViewById(R.id.clear_button);
        menuIcon = findViewById(R.id.menu_button);
        emojiIcon = findViewById(R.id.emoji_button);
        dirLtr = findViewById(R.id.direction_ltr);
        dirPause = findViewById(R.id.direction_pause);
        dirRtl = findViewById(R.id.direction_rtl);
        fontTypeValue = findViewById(R.id.font_type_value);

        videoView = findViewById(R.id.preview_background_video);
        imageView = findViewById(R.id.preview_background_image);
        effectTab = findViewById(R.id.effect_mode_tabs);
        videoRow = findViewById(R.id.video_effect_row);
        imageRow = findViewById(R.id.image_effect_row);
        videoList = findViewById(R.id.video_effect_list);
        imageList = findViewById(R.id.image_effect_list);
        videoList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoAdapter = new EffectAdapter(this, true, this::onVideoEffectSelected);
        imageAdapter = new EffectAdapter(this, false, this::onImageEffectSelected);
        videoList.setAdapter(videoAdapter);
        imageList.setAdapter(imageAdapter);
    }

    /** 把上次保存的所有设置回填到界面与预览 */
    private void applyStoredValues() {
        input.setText(settings.getLedText());
        input.setSelection(input.getText().length());
        ledView.setText(settings.getLedText());
        clearLayout.setVisibility(input.getText().length() > 0 ? View.VISIBLE : View.GONE);

        reloadColors();

        String dir = settings.getDirection();
        applyDirection(dir);

        ledView.setTextFont(Fonts.get(this, settings.getFontType()));
        fontTypeValue.setText(Fonts.label(settings.getFontType()));
        ledView.setSpeed(settings.getSpeed());
        ledView.setBlinkPercent(settings.getBlink());
        ledView.setFontPercent(settings.getFontSize());
        ledView.setMarkSpace(settings.getLedSize());
        // 让背景图层跟文字一起参与点阵遮罩
        ledView.setBackgroundVideo(videoView);
        ledView.setBackgroundImage(imageView);

        edgeLight.setEdgeColors(Settings.toIntColors(settings.getEdgeColors()));
        edgeLight.setStrokeSize(settings.getEdgeSize());
        edgeLight.setCornerRadius(settings.getEdgeRadiusFraction());
        edgeLight.setVisibility(settings.isEdgeEnable() ? View.VISIBLE : View.GONE);

        setSeekProgress(SEEK_SPEED, Math.round(settings.getSpeed() * 100) - 1);
        setSeekProgress(SEEK_BLINK, Math.round(settings.getBlink() * 10));
        setSeekProgress(SEEK_BRIGHTNESS, Math.round(settings.getBrightness() * 100));
        setSeekProgress(SEEK_LED_SIZE, settings.getLedSize());
        setSeekProgress(SEEK_FONT_SIZE, Math.round(settings.getFontSize() * 100));
        setSeekProgress(SEEK_EDGE_RADIUS, Math.round(settings.getEdgeRadiusFraction() * 100));
        setSeekProgress(SEEK_EDGE_SIZE, Math.round(settings.getEdgeSize()));

        SwitchCompat edgeSwitch = findViewById(R.id.edge_switch);
        edgeSwitch.setChecked(settings.isEdgeEnable());
        SwitchCompat fullScreenSwitch = findViewById(R.id.full_screen_switch);
        fullScreenSwitch.setChecked(true);
    }

    /** 重新把三组颜色应用到控件（LED 文字 / LED 背景 / 边缘流光） */
    private void reloadColors() {
        fontColors.clear();
        addAll(fontColors, Settings.toIntColors(settings.getFontColors()));
        bgColors.clear();
        addAll(bgColors, Settings.toIntColors(settings.getBgColors()));
        edgeColors.clear();
        addAll(edgeColors, Settings.toIntColors(settings.getEdgeColors()));

        ledView.setTextColor(fontColors.isEmpty()
                ? new int[]{getColor(R.color.led_default_text)} : toIntArray(fontColors));
        ledView.setBgColor(bgColors.isEmpty() ? new int[]{Color.BLACK} : toIntArray(bgColors));
        edgeLight.setEdgeColors(edgeColors.isEmpty() ? new int[]{Color.RED} : toIntArray(edgeColors));
    }

    // ------------------------------------------------------------------ 背景效果

    /** 初始化「背景效果」分组：视频 / 图片切换 + 素材列表 */
    private void setupBackground() {
        effectTab.setTabData(new String[]{getString(R.string.effect_video), getString(R.string.effect_image)});
        effectTab.setCurrentTab(settings.getEffectMode());
        effectTab.setOnTabSelectListener(new SegmentTabLayout.OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                settings.setEffectMode(position);
                updateEffectRows();
            }

            @Override
            public void onTabReselect(int position) {
            }
        });
        updateEffectRows();
        applyBackground();
    }

    /** 在视频 / 图片两组之间切换显示。 */
    private void updateEffectRows() {
        boolean videoMode = settings.getEffectMode() == 0;
        videoRow.setVisibility(videoMode ? View.VISIBLE : View.GONE);
        imageRow.setVisibility(videoMode ? View.GONE : View.VISIBLE);
        buildEffectItems();
    }

    /**
     * 刷新两个素材列表（视频 / 图片），选中项来自设置。
     * 对应原版 RecyclerView + item_led_media_picker。
     */
    private void buildEffectItems() {
        videoAdapter.submit(Backgrounds.builtInVideos(), settings.getVideoEffectUrl());
        imageAdapter.submit(Backgrounds.builtInImages(), settings.getImageEffectUrl());
    }

    private void onVideoEffectSelected(Backgrounds.Effect effect) {
        if (effect.isNone()) {
            settings.selectVideoEffect(null);
        } else {
            settings.selectVideoEffect(effect.url);
        }
        videoAdapter.setSelected(effect.isNone() ? null : effect.url);
        applyBackground();
    }

    private void onImageEffectSelected(Backgrounds.Effect effect) {
        if (effect.isNone()) {
            settings.selectImageEffect(null);
        } else {
            settings.selectImageEffect(effect.url);
        }
        imageAdapter.setSelected(effect.isNone() ? null : effect.url);
        applyBackground();
    }

    /** 把设置应用到预览区。 */
    /** 按当前设置把视频或图片背景应用到预览区 */
    private void applyBackground() {
        boolean videoMode = settings.getEffectMode() == 0;
        String videoUrl = settings.getVideoEffectUrl();
        String imageUrl = settings.getImageEffectUrl();

        if (videoMode && videoUrl != null) {
            videoView.setVideoUri(videoUrl);
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            imageView.setImageDrawable(null);
        } else if (!videoMode && imageUrl != null) {
            Bitmap bitmap = Backgrounds.decode(this, imageUrl, 1080, 1080);
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

    private void refreshPreviewItem() {
        buildEffectItems();
    }

    // ------------------------------------------------------------------ 监听

    /** 所有交互事件（输入、按钮、滑动条、开关） */
    private void setupListeners() {
        input.addTextChangedListener(textWatcher);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
        clearLayout.setOnClickListener(v -> input.setText(""));

        menuIcon.setOnClickListener(this::showMenu);
        emojiIcon.setOnClickListener(v -> showEmojiPicker());

        dirLtr.setOnClickListener(v -> setDirection(Settings.DIR_LTR));
        dirPause.setOnClickListener(v -> setDirection(Settings.DIR_PAUSE));
        dirRtl.setOnClickListener(v -> setDirection(Settings.DIR_RTL));

        setupSeek(SEEK_SPEED, progress -> {
            float speed = Math.max(0.01f, Math.min(0.98f, (progress + 1) / 100f));
            ledView.setSpeed(speed);
            settings.setSpeed(speed);
        });
        setupSeek(SEEK_BLINK, progress -> {
            float blink = progress / 10f;
            ledView.setBlinkPercent(blink);
            settings.setBlink(blink);
        });
        setupSeek(SEEK_BRIGHTNESS, progress -> {
            float brightness = Math.max(0.01f, progress / 100f);
            setBrightness(brightness);
            settings.setBrightness(brightness);
        });
        setupSeek(SEEK_LED_SIZE, progress -> {
            ledView.setMarkSpace(progress);
            settings.setLedSize(progress);
        });
        setupSeek(SEEK_FONT_SIZE, progress -> {
            float percent = Math.max(0.05f, progress / 100f);
            ledView.setFontPercent(percent);
            settings.setFontSize(percent);
        });
        setupSeek(SEEK_EDGE_RADIUS, progress -> {
            float fraction = progress / 100f;
            edgeLight.setCornerRadius(fraction);
            ledView.setCornerRadius(fraction * 200);
            settings.setEdgeRadiusFraction(fraction);
        });
        setupSeek(SEEK_EDGE_SIZE, progress -> {
            edgeLight.setStrokeSize(progress);
            settings.setEdgeSize(progress);
        });

        View fontTypeRow = findViewById(R.id.font_type_row);
        fontTypeRow.setOnClickListener(v -> new FontChooseDialog(this, settings.getFontType(), font -> {
            settings.setFontType(font);
            ledView.setTextFont(Fonts.get(this, font));
            fontTypeValue.setText(Fonts.label(font));
        }).show());

        View colorFont = findViewById(R.id.font_color_picker);
        colorFont.setOnClickListener(v -> new ColorPickerDialog(this,
                fontColors.isEmpty() ? Color.YELLOW : fontColors.get(0), color -> {
            fontColors.clear();
            fontColors.add(color);
            ledView.setTextColor(color);
            settings.setFontColors(Settings.toColorStrings(fontColors));
        }).show());

        View colorBg = findViewById(R.id.background_color_picker);
        colorBg.setOnClickListener(v -> new ColorPickerDialog(this,
                bgColors.isEmpty() ? Color.BLACK : bgColors.get(0), color -> {
            bgColors.clear();
            bgColors.add(color);
            ledView.setBgColor(color);
            settings.setBgColors(Settings.toColorStrings(bgColors));
        }).show());

        View colorEdge = findViewById(R.id.edge_color_picker);
        colorEdge.setOnClickListener(v -> new ColorPickerDialog(this,
                edgeColors.isEmpty() ? Color.RED : edgeColors.get(0), color -> {
            edgeColors.clear();
            edgeColors.add(color);
            edgeLight.setEdgeColors(color);
            edgeLight.setVisibility(View.VISIBLE);
            edgeLight.startAnimation();
            settings.setEdgeColors(Settings.toColorStrings(edgeColors));
            settings.setEdgeEnable(true);
            SwitchCompat sw = findViewById(R.id.edge_switch);
            sw.setChecked(true);
        }).show());

        SwitchCompat edgeSwitch = findViewById(R.id.edge_switch);
        edgeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setEdgeEnable(isChecked);
            edgeLight.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                edgeLight.startAnimation();
            }
        });

        MaterialButton play = findViewById(R.id.play_button);
        play.setOnClickListener(v -> startActivity(new Intent(this, LEDScrollerActivity.class)));
    }

    private void setupSeek(int id, final OnSeek callback) {
        SeekBar bar = findViewById(id);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    callback.onSeek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private interface OnSeek {
        void onSeek(int progress);
    }

    private void setSeekProgress(int id, int progress) {
        SeekBar bar = findViewById(id);
        bar.setProgress(Math.max(0, Math.min(bar.getMax(), progress)));
    }

    // ------------------------------------------------------------------ 方向

    /** 切换滚动方向并持久化 */
    private void setDirection(String direction) {
        settings.setDirection(direction);
        applyDirection(direction);
    }

    private void applyDirection(String direction) {
        if (Settings.DIR_PAUSE.equals(direction)) {
            ledView.setDirection(LEDScrollerView.DIRECTION_CENTER);
        } else if (Settings.DIR_RTL.equals(direction)) {
            ledView.setDirection(LEDScrollerView.DIRECTION_RTL);
        } else {
            ledView.setDirection(LEDScrollerView.DIRECTION_LTR);
        }
        tint(dirLtr, Settings.DIR_LTR.equals(direction));
        tint(dirPause, Settings.DIR_PAUSE.equals(direction));
        tint(dirRtl, Settings.DIR_RTL.equals(direction));
    }

    private void tint(ImageView view, boolean selected) {
        view.setSelected(selected);
        view.setColorFilter(new PorterDuffColorFilter(selected ? SELECTED_COLOR : NORMAL_COLOR,
                PorterDuff.Mode.SRC_IN));
    }

    /** 设置窗口亮度（只有本页面生效，离开后恢复系统亮度） */
    private void setBrightness(float brightness) {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        try {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.screenBrightness = Math.max(0.01f, Math.min(1f, brightness));
            window.setAttributes(attributes);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ 菜单 / 表情

    /** 左上角菜单：颜色 / 字体 / 表情的快捷入口 */
    private void showMenu(View anchor) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.font_color);
        menu.getMenu().add(0, 2, 1, R.string.background_color);
        menu.getMenu().add(0, 3, 2, R.string.edge_color);
        menu.getMenu().add(0, 4, 3, R.string.font_type);
        menu.getMenu().add(0, 5, 4, R.string.emoji);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    new ColorPickerDialog(this, fontColors.isEmpty() ? Color.YELLOW : fontColors.get(0), color -> {
                        fontColors.clear();
                        fontColors.add(color);
                        ledView.setTextColor(color);
                        settings.setFontColors(Settings.toColorStrings(fontColors));
                    }).show();
                    return true;
                case 2:
                    new ColorPickerDialog(this, bgColors.isEmpty() ? Color.BLACK : bgColors.get(0), color -> {
                        bgColors.clear();
                        bgColors.add(color);
                        ledView.setBgColor(color);
                        settings.setBgColors(Settings.toColorStrings(bgColors));
                    }).show();
                    return true;
                case 3:
                    new ColorPickerDialog(this, edgeColors.isEmpty() ? Color.RED : edgeColors.get(0), color -> {
                        edgeColors.clear();
                        edgeColors.add(color);
                        edgeLight.setEdgeColors(color);
                        edgeLight.setVisibility(View.VISIBLE);
                        edgeLight.startAnimation();
                        settings.setEdgeColors(Settings.toColorStrings(edgeColors));
                        settings.setEdgeEnable(true);
                    }).show();
                    return true;
                case 4:
                    new FontChooseDialog(this, settings.getFontType(), font -> {
                        settings.setFontType(font);
                        ledView.setTextFont(Fonts.get(this, font));
                        fontTypeValue.setText(Fonts.label(font));
                    }).show();
                    return true;
                case 5:
                    showEmojiPicker();
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void showEmojiPicker() {
        new EmojiPickerDialog(this, emoji -> {
            int start = Math.max(0, input.getSelectionStart());
            input.getText().insert(start, emoji);
        }).show();
    }

    // ------------------------------------------------------------------ 生命周期

    @Override
    protected void onResume() {
        super.onResume();
        ledView.start();
        videoView.start();
        if (settings.isEdgeEnable()) {
            edgeLight.startAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ledView.stop();
        videoView.pause();
        edgeLight.stopAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.release();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    private static void addAll(List<Integer> target, int[] values) {
        for (int value : values) {
            target.add(value);
        }
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
