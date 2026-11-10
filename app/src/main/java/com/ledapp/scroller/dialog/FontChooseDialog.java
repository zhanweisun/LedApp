package com.ledapp.scroller.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ledapp.scroller.Fonts;
import com.ledapp.scroller.R;

import java.util.Arrays;

/**
 * 字体选择对话框：列出 APK 自带的全部字体，点选后即时应用。
 */
public class FontChooseDialog extends BottomSheetDialog {

    public interface OnFontSelected {
        void onSelected(String fontName);
    }

    private final String[] fontNames = Fonts.names();
    private final OnFontSelected listener;
    private TextView preview;
    private String current;

    public FontChooseDialog(Context context, String current, OnFontSelected listener) {
        super(context);
        this.current = current;
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_font_choose;
    }

    @Override
    protected void onViewCreated(View content) {
        final Context context = getContext();
        final float density = context.getResources().getDisplayMetrics().density;
        LinearLayout root = (LinearLayout) content;
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(createTitle(R.string.font_type));

        preview = new TextView(context);
        preview.setText("Hello 123");
        preview.setTypeface(Fonts.get(context, current));
        preview.setTextSize(28);
        preview.setTextColor(getContext().getColor(R.color.dialog_title));
        preview.setGravity(Gravity.CENTER);
        int pad = (int) (16 * density);
        preview.setPadding(pad, pad / 2, pad, pad);
        root.addView(preview);

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (320 * density)));

        for (final String fontName : fontNames) {
            TextView row = new TextView(context);
            row.setText(Fonts.label(fontName));
            row.setTypeface(Fonts.get(context, fontName));
            row.setTextSize(20);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad / 2, pad, pad / 2);
            row.setTextColor(fontName.equals(current)
                    ? getContext().getColor(R.color.accent_blue) : getContext().getColor(R.color.dialog_text));
            if (fontName.equals(current)) {
                row.setTextColor(getContext().getColor(R.color.accent_blue));
            }
            row.setOnClickListener(v -> select(fontName));
            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void select(String fontName) {
        current = fontName;
        if (preview != null) {
            preview.setTypeface(Fonts.get(getContext(), fontName));
        }
        if (listener != null) {
            listener.onSelected(fontName);
        }
        dismiss();
    }
}