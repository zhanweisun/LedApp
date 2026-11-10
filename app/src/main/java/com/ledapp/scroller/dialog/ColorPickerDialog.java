package com.ledapp.scroller.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ledapp.scroller.R;

/**
 * 颜色选择对话框：提供一组常用色板 + 十六进制颜色输入。
 */
public class ColorPickerDialog extends BottomSheetDialog {

    public interface OnColorChanged {
        void onColorChanged(int color);
    }

    private static final int[] PRESETS = {
            0xFFFF0000, 0xFFFF4081, 0xFFE040FB, 0xFF7C4DFF, 0xFF536DFE, 0xFF448AFF,
            0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF9E9E9E,
            0xFF607D8B, 0xFFFFFFFF, 0xFFFFD180, 0xFFCF8933, 0xFF000000, 0xFF00FF00
    };

    private final OnColorChanged listener;
    private EditText hexInput;

    public ColorPickerDialog(Context context, int initialColor, OnColorChanged listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_color_picker;
    }

    @Override
    protected void onViewCreated(View content) {
        final ViewGroup root = (ViewGroup) content;
        float density = getContext().getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        root.setPadding(pad, pad, pad, pad);
        root.removeAllViews();

        TextView title = new TextView(getContext());
        title.setText(R.string.pick_color);
        title.setTextSize(18);
        title.setTextColor(getContext().getColor(R.color.dialog_title));
        root.addView(title);

        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = pad / 2;
        root.addView(grid, gridLp);

        int perRow = 6;
        LinearLayout row = null;
        for (int i = 0; i < PRESETS.length; i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                grid.addView(row);
            }
            final int color = PRESETS[i];
            android.widget.ImageView swatch = new android.widget.ImageView(getContext());
            GradientDrawable d = new GradientDrawable();
            d.setColor(color);
            d.setShape(GradientDrawable.OVAL);
            swatch.setBackground(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
            lp.setMargins((int) (4 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
            swatch.setLayoutParams(lp);
            swatch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorChanged(color);
                }
                dismiss();
            });
            row.addView(swatch);
        }

        hexInput = new EditText(getContext());
        hexInput.setHint("#RRGGBB");
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = pad / 2;
        root.addView(hexInput, inputLp);

        android.widget.Button apply = new android.widget.Button(getContext());
        apply.setText(R.string.apply);
        apply.setGravity(Gravity.CENTER);
        apply.setOnClickListener(v -> {
            int c = parseHex(hexInput.getText().toString());
            if (listener != null) {
                listener.onColorChanged(c);
            }
            dismiss();
        });
        root.addView(apply);
    }

    private static int parseHex(String raw) {
        String s = raw == null ? "" : raw.trim().replace("#", "");
        if (s.length() == 6) {
            s = "FF" + s;
        }
        try {
            return (int) Long.parseLong(s, 16);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }
}