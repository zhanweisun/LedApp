package com.ledapp.scroller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 启动页（对应原版 SplashActivity）：短暂显示启动图后进入主界面。
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        com.ledapp.scroller.dialog.SplashDialog splash = new com.ledapp.scroller.dialog.SplashDialog(this);
        splash.showThen(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}