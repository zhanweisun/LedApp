package com.ledapp.scroller;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;

/**
 * 极简的 assets 访问 ContentProvider：
 * 把 <code>asset:///video/led_video_bg_0.mp4</code> 映射到 APK 内 assets，
 * 供 MediaPlayer / BitmapFactory / 视频缩略图直接使用。
 */
public class AssetProvider extends ContentProvider {

    public static final String AUTHORITY = "com.ledapp.scroller.assets";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        AssetFileDescriptor afd = openAsset(uri);
        return afd.getParcelFileDescriptor();
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        return openAsset(uri);
    }

    private AssetFileDescriptor openAsset(Uri uri) throws FileNotFoundException {
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            return getContext().getAssets().openFd(path);
        } catch (Exception e) {
            throw new FileNotFoundException(path);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}