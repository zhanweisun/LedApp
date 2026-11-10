package com.ledapp.scroller;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 背景素材清单与工具方法（对应原版的 EffectInfo）。
 * <p>素材全部来自原版 APP：</p>
 * <ul>
 *   <li>图片：assets/image/led_image_bg_0 ~ 25（25 张，1920x1080）；</li>
 *   <li>视频：assets/video/led_video_bg_0 ~ 24（24 个循环动画，含缩略图）。</li>
 * </ul>
 * 另外支持用户从相册导入的素材（复制到应用私有目录）。
 */
public final class Backgrounds {

    private static final String TAG = "Backgrounds";
    private static final String ASSET = "file:///android_asset/";

    /** 背景效果条目。 */
    public static class Effect {
        public final String url;
        public final String thumb;
        public final boolean video;
        public final boolean none;

        Effect(String url, String thumb, boolean video, boolean none) {
            this.url = url;
            this.thumb = thumb;
            this.video = video;
            this.none = none;
        }

        public boolean isNone() {
            return none;
        }
    }

    private Backgrounds() {
    }

    private static Effect assetVideo(int index) {
        return new Effect(ASSET + "video/led_video_bg_" + index + ".mp4",
                ASSET + "video_thumb/led_video_bg_" + index + "_thumb.webp", true, false);
    }

    private static Effect assetImage(int index) {
        String file = index == 0 ? "led_image_bg_0.webp" : "led_image_bg_" + index + ".png";
        String thumb = index == 0 ? "led_image_bg_0_thumb.webp" : "led_image_bg_" + index + "_thumb.webp";
        return new Effect(ASSET + "image/" + file, ASSET + "image_thumb/" + thumb, false, false);
    }

    /** 内置视频背景（原版 LED Scroller 自带的全部视频）。 */
    public static List<Effect> builtInVideos() {
        List<Effect> list = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            list.add(assetVideo(i));
        }
        return list;
    }

    /** 内置图片背景（原版 LED Scroller 自带的全部图片）。 */
    public static List<Effect> builtInImages() {
        List<Effect> list = new ArrayList<>();
        for (int i = 0; i <= 25; i++) {
            list.add(assetImage(i));
        }
        return list;
    }

    /** 默认视频（原版默认选中的背景）。 */
    public static String defaultVideo() {
        return ASSET + "video/led_video_bg_0.mp4";
    }

    // ------------------------------------------------------------------ 导入

    /**
     * 把用户选择的 uri 复制到应用私有目录，返回 file:// 路径。
     */
    public static String persist(Context context, Uri uri, boolean video) {
        if (uri == null) {
            return null;
        }
        if ("file".equals(uri.getScheme()) && uri.getPath() != null
                && uri.getPath().startsWith(context.getFilesDir().getAbsolutePath())) {
            return uri.toString();
        }
        String ext = guessExtension(context, uri, video);
        File dir = new File(context.getFilesDir(), "backgrounds");
        if (!dir.exists() && !dir.mkdirs()) {
            return uri.toString();
        }
        File target = new File(dir, UUID.randomUUID().toString() + ext);
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.w(TAG, "persist failed", e);
            return uri.toString();
        }
        return Uri.fromFile(target).toString();
    }

    private static String guessExtension(Context context, Uri uri, boolean video) {
        String name = null;
        try (Cursor cursor = context.getContentResolver()
                .query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } catch (Exception ignored) {
        }
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        }
        return video ? ".mp4" : ".jpg";
    }

    // ------------------------------------------------------------------ 读取

    /** 取视频第一帧作为缩略图。 */
    public static Bitmap videoThumbnail(Context context, String url) {
        if (url == null || url.length() == 0) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, MediaSources.toUri(context, url));
            return retriever.getFrameAtTime(0);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    /** 解码图片（支持 assets / 本地文件 / content）。 */
    public static Bitmap decode(Context context, String url) {
        return decode(context, url, 0, 0);
    }

    /** 解码图片并按最大尺寸采样，避免列表滚动时内存占用过大。 */
    public static Bitmap decode(Context context, String url, int reqWidth, int reqHeight) {
        if (url == null || url.length() == 0) {
            return null;
        }
        try {
            if (reqWidth <= 0 || reqHeight <= 0) {
                try (InputStream in = open(context, url)) {
                    return BitmapFactory.decodeStream(in);
                }
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream in = open(context, url)) {
                BitmapFactory.decodeStream(in, null, options);
            }
            int sample = 1;
            while (options.outWidth / (sample * 2) >= reqWidth
                    && options.outHeight / (sample * 2) >= reqHeight) {
                sample *= 2;
            }
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sample;
            try (InputStream in = open(context, url)) {
                return BitmapFactory.decodeStream(in, null, decodeOptions);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static InputStream open(Context context, String url) throws Exception {
        if (MediaSources.isAsset(url)) {
            return context.getAssets().open(MediaSources.assetPath(url));
        }
        if (url.startsWith("file://")) {
            return new java.io.FileInputStream(url.substring("file://".length()));
        }
        return context.getContentResolver().openInputStream(Uri.parse(url));
    }
}