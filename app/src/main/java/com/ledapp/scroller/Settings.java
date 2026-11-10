package com.ledapp.scroller;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * LED 显示的全局配置，负责读写 SharedPreferences。
 * 对应原版 APP 中的 Settings 单例（方向 / 速度 / 闪烁 / 亮度 / 字体 / 颜色……）。
 */
public class Settings {

    public static final String DIR_PAUSE = "SCROLL_PAUSE";
    public static final String DIR_LTR = "LEFT_TO_RIGHT";
    public static final String DIR_RTL = "RIGHT_TO_LEFT";

    private static Settings sInstance;
    private final SharedPreferences sp;

    private Settings(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("led_settings", Context.MODE_PRIVATE);
    }

    public static synchronized Settings get(Context context) {
        if (sInstance == null) {
            sInstance = new Settings(context);
        }
        return sInstance;
    }

    // ---------------------------------------------------------------- 文本
    public String getLedText() {
        return sp.getString("led_text", "\uD83D\uDE00Hello world.\uD83C\uDF0D");
    }

    public void setLedText(String text) {
        sp.edit().putString("led_text", text == null ? "" : text).apply();
    }

    // ---------------------------------------------------------------- 方向
    public String getDirection() {
        return sp.getString("direction", DIR_LTR);
    }

    public void setDirection(String direction) {
        sp.edit().putString("direction", direction).apply();
    }

    // --------------------------------------------------- 速度 / 闪烁 / 亮度
    public float getSpeed() {
        return sp.getFloat("speed", 0.2f);
    }

    public void setSpeed(float v) {
        sp.edit().putFloat("speed", Math.max(0.01f, Math.min(0.99f, v))).apply();
    }

    public float getBlink() {
        return sp.getFloat("blink", 0f);
    }

    public void setBlink(float v) {
        sp.edit().putFloat("blink", v).apply();
    }

    public float getBrightness() {
        return sp.getFloat("brightness", 0.5f);
    }

    public void setBrightness(float v) {
        sp.edit().putFloat("brightness", v).apply();
    }

    // ------------------------------------------------------- LED 点阵 / 字号
    /** LED 点距（px），对应界面上的 “LED 大小” */
    public int getLedSize() {
        return sp.getInt("led_size", 3);
    }

    public void setLedSize(int v) {
        sp.edit().putInt("led_size", v).apply();
    }

    /** 字体占屏幕短边的比例 */
    public float getFontSize() {
        return sp.getFloat("font_size", 0.5f);
    }

    public void setFontSize(float v) {
        sp.edit().putFloat("font_size", v).apply();
    }

    // ---------------------------------------------------------------- 颜色
    public String[] getFontColors() {
        return parseColorArray(sp.getString("font_colors", ""));
    }

    public void setFontColors(String[] colors) {
        sp.edit().putString("font_colors", join(colors)).apply();
    }

    public String[] getBgColors() {
        return parseColorArray(sp.getString("background_colors", ""));
    }

    public void setBgColors(String[] colors) {
        sp.edit().putString("background_colors", join(colors)).apply();
    }

    public String[] getEdgeColors() {
        return parseColorArray(sp.getString("edge_colors",
                "-65536,-256,-16711936,-16537100,-16776961,-65281,-65536"));
    }

    public void setEdgeColors(String[] colors) {
        sp.edit().putString("edge_colors", join(colors)).apply();
    }

    // ---------------------------------------------------------------- 边缘
    public boolean isEdgeEnable() {
        return sp.getBoolean("edge_enable", false);
    }

    public void setEdgeEnable(boolean v) {
        sp.edit().putBoolean("edge_enable", v).apply();
    }

    public float getEdgeRadiusFraction() {
        return sp.getFloat("edge_radius_fraction", 0.2f);
    }

    public void setEdgeRadiusFraction(float v) {
        sp.edit().putFloat("edge_radius_fraction", v).apply();
    }

    public float getEdgeSize() {
        return sp.getFloat("edge_size", 20f);
    }

    public void setEdgeSize(float v) {
        sp.edit().putFloat("edge_size", v).apply();
    }

    // ---------------------------------------------------------------- 字体
    public String getFontType() {
        return sp.getString("font_type", Fonts.RUBIK_MEDIUM);
    }

    public void setFontType(String type) {
        sp.edit().putString("font_type", type).apply();
    }

    // ------------------------------------------------------------ 背景效果
    /** 不使用背景效果时的占位值（与原版 EffectInfo.blockInfo 对应）。 */
    public static final String EFFECT_NONE = "none";
    public static final String ASSET_VIDEO_0 = "file:///android_asset/video/led_video_bg_0.mp4";
    public static final String ASSET_VIDEO_THUMB_0 = "file:///android_asset/video/led_video_bg_0_thumb.webp";
    public static final String ASSET_IMAGE_0 = "file:///android_asset/image/led_image_bg_0.webp";

    /** 背景效果类型：0 = 视频，1 = 图片（与原版 effectTabLayout 一致）。 */
    public int getEffectMode() {
        return sp.getInt("effect_mode", 0);
    }

    public void setEffectMode(int mode) {
        sp.edit().putInt("effect_mode", mode).apply();
    }

    /** 是否启用背景效果（视频或图片任选其一）。 */
    public boolean isEffectEnabled() {
        return getVideoEffectUrl() != null || getImageEffectUrl() != null;
    }

    public String getVideoEffectUrl() {
        return sp.getString("video_effect", ASSET_VIDEO_0);
    }

    public void setVideoEffectUrl(String url) {
        sp.edit().putString("video_effect", url).apply();
    }

    public String getImageEffectUrl() {
        return sp.getString("image_effect", null);
    }

    public void setImageEffectUrl(String url) {
        sp.edit().putString("image_effect", url == null ? "" : url).commit();
    }

    /** 选择视频背景时清空图片背景，反之亦然（与原版 n()/o() 行为一致）。 */
    public void selectVideoEffect(String url) {
        setVideoEffectUrl(url);
        setImageEffectUrl(null);
        setEffectMode(0);
    }

    public void selectImageEffect(String url) {
        setImageEffectUrl(url);
        setVideoEffectUrl(null);
        setEffectMode(1);
    }

    private static String join(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private static String[] parseColorArray(String raw) {
        if (raw == null || raw.length() == 0) {
            return new String[0];
        }
        return raw.split(",");
    }

    public static int[] toIntColors(String[] values) {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                out[i] = Integer.parseInt(values[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0xFFFFFFFF;
            }
        }
        return out;
    }

    public static String[] toColorStrings(java.util.List<Integer> values) {
        String[] out = new String[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = String.valueOf(values.get(i));
        }
        return out;
    }
}