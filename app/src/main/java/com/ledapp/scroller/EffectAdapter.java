package com.ledapp.scroller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 背景素材列表适配器（对应原版 RecyclerView + item_led_media_picker）。
 * 素材较多（视频 24 个 + 图片 26 个），缩略图在后台线程解码后回填。
 */
public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.Holder> {

    public interface OnEffectSelected {
        void onSelected(Backgrounds.Effect effect);
    }

    private final Context context;
    private final boolean videoMode;
    private final OnEffectSelected listener;
    private final List<Backgrounds.Effect> items = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private String selectedUrl;

    public EffectAdapter(Context context, boolean videoMode, OnEffectSelected listener) {
        this.context = context;
        this.videoMode = videoMode;
        this.listener = listener;
    }

    public void submit(List<Backgrounds.Effect> data, String selected) {
        items.clear();
        items.addAll(data);
        selectedUrl = selected;
        notifyDataSetChanged();
    }

    public void setSelected(String url) {
        selectedUrl = url;
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_effect, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Backgrounds.Effect effect = items.get(position);
        boolean selected = effect.isNone()
                ? (selectedUrl == null)
                : (effect.url != null && effect.url.equals(selectedUrl));
        holder.bind(effect, selected);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onViewRecycled(Holder holder) {
        holder.cancel();
        super.onViewRecycled(holder);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ImageView thumb;
        private final ImageView check;
        private String boundUrl;
        private boolean cancelled;

        Holder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.effect_thumb);
            check = itemView.findViewById(R.id.effect_check);
        }

        void cancel() {
            cancelled = true;
            boundUrl = null;
        }

        void bind(final Backgrounds.Effect effect, boolean selected) {
            cancelled = false;
            boundUrl = effect.url;
            thumb.setImageDrawable(null);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            check.setVisibility(selected ? View.VISIBLE : View.GONE);

            // 选中描边
            if (selected) {
                GradientDrawable border = new GradientDrawable();
                border.setColor(Color.TRANSPARENT);
                border.setStroke((int) (2 * context.getResources().getDisplayMetrics().density),
                        context.getColor(R.color.accent_blue));
                itemView.setBackground(border);
            } else {
                itemView.setBackground(null);
            }

            if (effect.isNone()) {
                thumb.setScaleType(ImageView.ScaleType.CENTER);
                thumb.setImageResource(R.drawable.ic_close_circle);
                thumb.setColorFilter(context.getColor(R.color.text_primary));
                thumb.setAlpha(1f);
            } else {
                thumb.setColorFilter(null);
                thumb.setAlpha(0.85f);
                final String url = effect.thumb != null ? effect.thumb : effect.url;
                final String bindUrl = effect.url;
                executor.execute(() -> {
                    Bitmap bitmap = Backgrounds.decode(context, url, 200, 200);
                    main.post(() -> {
                        if (!cancelled && bindUrl != null && bindUrl.equals(boundUrl) && bitmap != null) {
                            thumb.setImageBitmap(bitmap);
                        }
                    });
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSelected(effect);
                }
            });
        }
    }
}