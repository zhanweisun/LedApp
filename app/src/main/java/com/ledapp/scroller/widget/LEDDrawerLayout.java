package com.ledapp.scroller.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.drawerlayout.widget.DrawerLayout;

/** 主界面使用的抽屉布局（原版同名类，屏蔽了边缘手势，只能点击按钮打开）。 */
public class LEDDrawerLayout extends DrawerLayout {
    public LEDDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }
}
