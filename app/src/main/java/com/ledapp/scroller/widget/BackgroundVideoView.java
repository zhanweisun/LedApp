package com.ledapp.scroller.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * 视频背景（对应原版主界面中的 VideoView 层）。
 * 内部使用 {@link TextureView} + MediaPlayer：
 * 由 {@link #onSurfaceTextureSizeChanged} 换算“居中裁剪”的变换矩阵，
 * 所以无论父容器是什么比例，视频都能等比铺满而不变形。
 */
public class BackgroundVideoView extends TextureView {

    private android.media.MediaPlayer player; // 视频播放器
    private String dataSource;                // 视频地址（assets / 本地 / content）
    private boolean surfaceReady;             // TextureView 是否已就绪
    private boolean playing;                  // 是否处于播放状态（供 onResume/onPause 使用）

    public BackgroundVideoView(Context context) {
        this(context, null);
    }

    public BackgroundVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpaque(false);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surface, int w, int h) {
                surfaceReady = true;
                if (dataSource != null) {
                    play();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int w, int h) {
                updateTransform();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
                surfaceReady = false;
                releasePlayer();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {
            }
        });
    }

    /** 设置视频地址（支持 assets / 本地文件 / content:// 等 URI）。 */
    public void setVideoUri(String uri) {
        if (uri != null && uri.equals(dataSource) && player != null) {
            return;
        }
        dataSource = uri == null || uri.length() == 0 ? null : uri;
        if (dataSource == null) {
            releasePlayer();
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        if (surfaceReady) {
            play();
        }
    }

    private void play() {
        releasePlayer();
        try {
            player = new android.media.MediaPlayer();
            player.setSurface(new android.view.Surface(getSurfaceTexture()));
            player.setDataSource(getContext(), com.ledapp.scroller.MediaSources.toUri(getContext(), dataSource));
            player.setLooping(true);
            player.setVolume(0f, 0f);
            player.setOnVideoSizeChangedListener((mp, width, height) -> updateTransform());
            player.setOnPreparedListener(mp -> {
                if (playing) {
                    mp.start();
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            setVisibility(GONE);
        }
    }

    /** 开始 / 恢复播放，用于 Activity 的 onResume。 */
    public void start() {
        playing = true;
        if (player != null) {
            try {
                player.start();
            } catch (Exception ignored) {
            }
        }
    }

    /** 暂停播放，用于 Activity 的 onPause。 */
    public void pause() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.pause();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void release() {
        playing = false;
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    /** 居中裁剪：按视频与视图尺寸的较大缩放比铺满，并居中。 */
    private void updateTransform() {
        if (player == null) {
            return;
        }
        int vw = player.getVideoWidth();
        int vh = player.getVideoHeight();
        int viewW = getWidth();
        int viewH = getHeight();
        if (vw <= 0 || vh <= 0 || viewW <= 0 || viewH <= 0) {
            return;
        }
        float sx = viewW / (float) vw;
        float sy = viewH / (float) vh;
        float scale = Math.max(sx, sy);
        float scaledW = vw * scale;
        float scaledH = vh * scale;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate((viewW - scaledW) / 2f, (viewH - scaledH) / 2f);
        setTransform(matrix);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (dataSource != null) {
            setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }
}