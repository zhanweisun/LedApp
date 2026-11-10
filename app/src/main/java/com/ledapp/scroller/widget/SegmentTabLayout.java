package com.ledapp.scroller.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 分段选择控件（对应原版的 SegmentTabLayout / FlycoTabLayout）。
 * 用于字体颜色 / 背景颜色 / 背景效果等选项的分组切换。
 */
public class SegmentTabLayout extends LinearLayout {

    public interface OnTabSelectListener {
        void onTabSelect(int position);

        void onTabReselect(int position);
    }

    private final LinearLayout container;
    private String[] titles = new String[0];
    private int currentTab = 0;
    private OnTabSelectListener listener;

    private int indicatorColor = 0xFF006BDE;
    private int indicatorCornerRadius = 10;
    private int normalColor = 0xFFE4E6EB;
    private int selectedColor = Color.WHITE;
    private float textSize = 16f;

    public SegmentTabLayout(Context context) {
        this(context, null);
    }

    public SegmentTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setBackground(makeIndicator(0));
        addView(container, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private GradientDrawable makeIndicator(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(indicatorCornerRadius * getResources().getDisplayMetrics().density);
        return d;
    }

    public void setIndicatorColor(int color) {
        indicatorColor = color;
        container.setBackground(makeIndicator(color));
    }

    public void setIndicatorCornerRadius(float radius) {
        indicatorCornerRadius = (int) radius;
        container.setBackground(makeIndicator(indicatorColor));
    }

    public void setTabData(String[] data) {
        titles = data == null ? new String[0] : data;
        rebuild();
    }

    private void rebuild() {
        container.removeAllViews();
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            TextView tab = new TextView(getContext());
            tab.setText(titles[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(2, 2, 2, 2);
            tab.setLayoutParams(lp);
            tab.setOnClickListener(v -> {
                if (index == currentTab) {
                    if (listener != null) {
                        listener.onTabReselect(index);
                    }
                    return;
                }
                currentTab = index;
                updateStates();
                if (listener != null) {
                    listener.onTabSelect(index);
                }
            });
            container.addView(tab);
        }
        updateStates();
    }

    private void updateStates() {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView tab = (TextView) container.getChildAt(i);
            boolean selected = i == currentTab;
            tab.setTextColor(selected ? selectedColor : normalColor);
            if (selected) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(indicatorColor);
                bg.setCornerRadius(indicatorCornerRadius * getResources().getDisplayMetrics().density);
                tab.setBackground(bg);
            } else {
                tab.setBackground(null);
            }
        }
    }

    public void setCurrentTab(int index) {
        if (index < 0 || index >= titles.length) {
            return;
        }
        currentTab = index;
        updateStates();
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public void setOnTabSelectListener(OnTabSelectListener listener) {
        this.listener = listener;
    }
}