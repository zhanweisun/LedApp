package com.ledapp.scroller;

import android.content.Context;
import android.graphics.Typeface;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字体表。字体文件全部来自原 APK 的 res/font 目录（经识别对应关系如下）：
 * e.ttf -> Manrope ExtraLight Bold
 * h.ttf -> Manrope ExtraLight Medium
 * i.ttf -> Manrope ExtraLight Regular
 * k.ttf -> Rubik Bold
 * l.ttf -> Rubik Medium
 * m.ttf -> Rubik Regular
 * a.ttf -> Bebas Neue Regular
 * b.ttf -> Jersey 10
 * c.ttf -> Jersey 15
 * d.ttf -> Jersey 20
 */
public final class Fonts {

    public static final String MANROPE_BOLD = "MANROPE_BOLD";
    public static final String MANROPE_MEDIUM = "MANROPE_MEDIUM";
    public static final String MANROPE_REGULAR = "MANROPE_REGULAR";
    public static final String RUBIK_BOLD = "RUBIK_BOLD";
    public static final String RUBIK_MEDIUM = "RUBIK_MEDIUM";
    public static final String RUBIK_REGULAR = "RUBIK_REGULAR";
    public static final String BEBASNEUE_REGULAR = "BEBASNEUE_REGULAR";
    public static final String JERSEY10_REGULAR = "JERSEY10_REGULAR";
    public static final String JERSEY15_REGULAR = "JERSEY15_REGULAR";
    public static final String JERSEY20_REGULAR = "JERSEY20_REGULAR";

    private static final Map<String, Integer> RES = new LinkedHashMap<>();
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        RES.put(MANROPE_BOLD, R.font.manrope_bold);
        RES.put(MANROPE_MEDIUM, R.font.manrope_medium);
        RES.put(MANROPE_REGULAR, R.font.manrope_regular);
        RES.put(RUBIK_BOLD, R.font.rubik_bold);
        RES.put(RUBIK_MEDIUM, R.font.rubik_medium);
        RES.put(RUBIK_REGULAR, R.font.rubik_regular);
        RES.put(BEBASNEUE_REGULAR, R.font.bebas_neue);
        RES.put(JERSEY10_REGULAR, R.font.jersey_10);
        RES.put(JERSEY15_REGULAR, R.font.jersey_15);
        RES.put(JERSEY20_REGULAR, R.font.jersey_20);

        LABELS.put(MANROPE_BOLD, "Manrope Bold");
        LABELS.put(MANROPE_MEDIUM, "Manrope Medium");
        LABELS.put(MANROPE_REGULAR, "Manrope Regular");
        LABELS.put(RUBIK_BOLD, "Rubik Bold");
        LABELS.put(RUBIK_MEDIUM, "Rubik Medium");
        LABELS.put(RUBIK_REGULAR, "Rubik Regular");
        LABELS.put(BEBASNEUE_REGULAR, "Bebas Neue");
        LABELS.put(JERSEY10_REGULAR, "Jersey 10");
        LABELS.put(JERSEY15_REGULAR, "Jersey 15");
        LABELS.put(JERSEY20_REGULAR, "Jersey 20");
    }

    private Fonts() {
    }

    public static String[] names() {
        return RES.keySet().toArray(new String[0]);
    }

    public static String label(String name) {
        String label = LABELS.get(name);
        return label != null ? label : name;
    }

    /** 字体不含 emoji / 中日韩字符，这些字符交给系统字体渲染。 */
    public static Typeface get(Context context, String name) {
        Integer id = RES.get(name);
        if (id != null) {
            try {
                return Typeface.create(context.getResources().getFont(id), Typeface.NORMAL);
            } catch (Exception ignored) {
                // 忽略，回退到默认字体
            }
        }
        return Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    }
}