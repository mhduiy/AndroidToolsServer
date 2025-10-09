package com.mhduiy.androidtoolsserver.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class Utils {
    private static final String TAG = "utils";
    /**
     * 读取文件内容的通用方法
     */
    public static String readAllText(File file) {
        try {
            if (!file.exists() || !file.canRead()) {
                return "";
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[20480];
            int count = fileInputStream.read(bytes);
            fileInputStream.close();
            return new String(bytes, 0, count, Charset.defaultCharset()).trim();
        } catch (IOException ex) {
            Logger.w(TAG, "ReadAllText failed for " + file.getPath() + ": " + ex.getMessage());
            return "";
        }
    }

    /**
     * 将Drawable转换为Base64字符串，支持多种类型的Drawable
     */
    public static String drawableToBase64(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        try {
            Bitmap bitmap = null;

            // 处理不同类型的Drawable
            if (drawable instanceof BitmapDrawable) {
                // BitmapDrawable类型
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null) {
                    Logger.d(TAG, "BitmapDrawable - size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                }
            } else {
                // 其他类型的Drawable（包括AdaptiveIconDrawable）
                // 通过Canvas绘制到Bitmap
                int width = drawable.getIntrinsicWidth();
                int height = drawable.getIntrinsicHeight();

                // 设置默认尺寸，防止无效尺寸
                if (width <= 0 || height <= 0) {
                    width = height = 144; // 默认144x144像素
                }

                Logger.d(TAG, "Non-BitmapDrawable (" + drawable.getClass().getSimpleName() + ") - creating bitmap: " + width + "x" + height);

                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, width, height);
                drawable.draw(canvas);
            }

            if (bitmap == null) {
                Logger.w(TAG, "Failed to get bitmap from drawable");
                return null;
            }

            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes = baos.toByteArray();

            Logger.d(TAG, "Bitmap compressed to " + bytes.length + " bytes");
            return Base64.encodeToString(bytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Logger.e(TAG, "Error converting drawable to base64: " + e.getMessage(), e);
            return null;
        }
    }
}
