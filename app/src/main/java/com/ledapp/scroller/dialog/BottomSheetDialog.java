package com.ledapp.scroller.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ledapp.scroller.R;

/**
 * 底部弹出对话框基类（对应原版 APP 的 BottomPopupView）。
 * 子类只需返回内容布局 id，并在 onViewCreated 中绑定控件。
 */
public abstract class BottomSheetDialog extends Dialog {

    private View contentView;

    protected BottomSheetDialog(Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        setCanceledOnTouchOutside(true);
    }

    /** 内容布局 id */
    protected abstract int getImplLayoutId();

    protected abstract void onViewCreated(View content);

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0.5f;
            window.setAttributes(lp);
        }

        float density = getContext().getResources().getDisplayMetrics().density;
        int radius = (int) (20 * density);

        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.WHITE);

        // 顶部拖拽条
        View handle = new View(getContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0xFFD0D0D0);
        handleBg.setCornerRadius(3 * density);
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(
                (int) (40 * density), (int) (5 * density));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.topMargin = (int) (10 * density);
        column.addView(handle, handleLp);

        int layout = getImplLayoutId();
        if (layout != 0) {
            contentView = LayoutInflater.from(getContext()).inflate(layout, column, false);
            if (contentView instanceof ViewGroup) {
                ((ViewGroup) contentView).setClipToPadding(false);
            }
            column.addView(contentView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            onViewCreated(contentView);
        }
        setContentView(column);

        column.setBackground(makeBackground(radius));
    }

    private GradientDrawable makeBackground(int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        return bg;
    }

    /** 内容视图（子类绑定控件用） */
    protected View getContentView() {
        return contentView;
    }

    protected TextView createTitle(int stringRes) {
        TextView title = new TextView(getContext());
        title.setText(stringRes);
        title.setTextSize(18);
        title.setTextColor(getContext().getColor(R.color.dialog_title));
        float density = getContext().getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        title.setPadding(pad, pad, pad, pad / 2);
        return title;
    }
}