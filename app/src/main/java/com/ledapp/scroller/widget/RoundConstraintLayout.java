package com.ledapp.scroller.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

/** 原版的圆角 ConstraintLayout，这里保留类名以便直接复用原布局。 */
public class RoundConstraintLayout extends ConstraintLayout {
    public RoundConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        setWillNotDraw(false);
    }
}
