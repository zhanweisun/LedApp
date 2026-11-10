package com.ledapp.scroller;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表情选择器，数据来自 APK 内置的 assets/emoji.json。
 */
public class EmojiPickerDialog extends Dialog {

    public interface OnEmojiSelected {
        void onSelected(String emoji);
    }

    private final OnEmojiSelected listener;
    private final Map<String, List<String>> categories = new LinkedHashMap<>();
    private LinearLayout content;

    public EmojiPickerDialog(Context context, OnEmojiSelected listener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.listener = listener;
        loadEmoji(context);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0.5f;
            window.setAttributes(lp);
        }
    }

    private void loadEmoji(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("emoji.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String category = object.optString("category", "Other");
                String emoji = object.optString("emoji", "");
                if (emoji.isEmpty()) {
                    continue;
                }
                List<String> list = categories.get(category);
                if (list == null) {
                    list = new ArrayList<>();
                    categories.put(category, list);
                }
                list.add(emoji);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (categories.isEmpty()) {
            List<String> fallback = new ArrayList<>();
            String[] basics = {"\uD83D\uDE00", "\uD83D\uDE02", "\u2764\uFE0F", "\uD83D\uDC4D",
                    "\uD83D\uDE0E", "\uD83D\uDD25", "\uD83C\uDF89", "\u2B50"};
            for (String s : basics) {
                fallback.add(s);
            }
            categories.put("Emoji", fallback);
        }
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float density = getContext().getResources().getDisplayMetrics().density;
        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.WHITE);
        content.setPadding((int) (8 * density), (int) (12 * density), (int) (8 * density), (int) (8 * density));

        // 分类标签
        android.widget.HorizontalScrollView tabs = new android.widget.HorizontalScrollView(getContext());
        LinearLayout tabRow = new LinearLayout(getContext());
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabs.addView(tabRow);
        content.addView(tabs);

        final android.widget.ScrollView scroll = new android.widget.ScrollView(getContext());
        final LinearLayout list = new LinearLayout(getContext());
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (280 * density));
        content.addView(scroll, scrollLp);

        int index = 0;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            final List<String> emojis = entry.getValue();
            TextView tab = new TextView(getContext());
            tab.setText(emojis.size() > 0 ? emojis.get(0) : entry.getKey());
            tab.setTextSize(20);
            tab.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
            final List<String> current = emojis;
            tab.setOnClickListener(v -> fillGrid(list, current));
            tabRow.addView(tab);
            if (index == 0) {
                fillGrid(list, emojis);
            }
            index++;
        }

        setContentView(content);
    }

    private void fillGrid(LinearLayout list, List<String> emojis) {
        float density = getContext().getResources().getDisplayMetrics().density;
        list.removeAllViews();
        int perRow = 7;
        LinearLayout row = null;
        for (int i = 0; i < emojis.size(); i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                list.addView(row);
            }
            final String emoji = emojis.get(i);
            TextView cell = new TextView(getContext());
            cell.setText(emoji);
            cell.setTextSize(24);
            cell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
            cell.setLayoutParams(lp);
            cell.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSelected(emoji);
                }
                dismiss();
            });
            row.addView(cell);
        }
    }
}