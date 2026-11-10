package com.ledapp.scroller;

import android.content.Context;
import android.net.Uri;

/**
 * 背景素材地址解析工具。
 * <p>assets 里的素材需要转换为 <code>asset:///</code> 地址或通过 AssetFileDescriptor 播放，
 * 直接给 MediaPlayer 传 <code>file:///android_asset/xxx</code> 会被当成普通文件路径。</p>
 */
public final class MediaSources {

    private static final String ASSET_PREFIX = "file:///android_asset/";

    private MediaSources() {
    }

    /** 是否为 APK 内置 assets 素材。 */
    public static boolean isAsset(String url) {
        return url != null && url.startsWith(ASSET_PREFIX);
    }

    public static String assetPath(String url) {
        return url.substring(ASSET_PREFIX.length());
    }

    /** 图片/文件等通过流读取时的真实 Uri。 */
    public static Uri toUri(Context context, String url) {
        if (isAsset(url)) {
            // 通过自定义 scheme 交给 ContentResolver 读取 assets
            return Uri.parse("content://" + AssetProvider.AUTHORITY + "/" + assetPath(url));
        }
        return Uri.parse(url);
    }

    /** ActivityResult 选择素材时持久化的临时权限（仅用于 content://）。 */
    public static void takePersistablePermission(Context context, Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
    }
}