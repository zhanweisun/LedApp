package com.ledapp.scroller.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ledapp.scroller.R;

/**
 * 应用启动页：显示 APK 自带的启动图标，淡入后自动跳转。
 */
public class SplashDialog extends Dialog {

    public SplashDialog(Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    public void showThen(final Runnable after) {
        FrameLayout root = new FrameLayout(getContext());
        root.setBackgroundColor(Color.BLACK);

        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        float density = getContext().getResources().getDisplayMetrics().density;
        int size = (int) (96 * density);
        ImageView icon = new ImageView(getContext());
        Bitmap bmp = BitmapFactory.decodeResource(getContext().getResources(), R.mipmap.ic_launcher);
        if (bmp != null) {
            icon.setImageBitmap(bmp);
        } else {
            icon.setImageResource(R.mipmap.ic_launcher);
        }
        column.addView(icon, new LinearLayout.LayoutParams(size, size));

        TextView label = new TextView(getContext());
        label.setText(R.string.app_name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (16 * density);
        column.addView(label, lp);

        root.addView(column);
        setContentView(root);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isShowing()) {
                    dismiss();
                }
                if (after != null) {
                    after.run();
                }
            }
        }, 600);
    }
}