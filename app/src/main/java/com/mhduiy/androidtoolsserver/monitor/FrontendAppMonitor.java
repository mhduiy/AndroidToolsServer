package com.mhduiy.androidtoolsserver.monitor;

import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.ContextManager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FrontendAppMonitor {
    private static final String TAG = "FrontendAppMonitor";
    public static class FrontendAppInfo {
        public String packageName = "Unknown";
        public String appName = "Unknown";
        public String activityName = "Unknown";
        public String version = "Unknown";
        public String versionCode = "0";
        public long memoryUsageMB = 0;
        public double cpuUsage = 0.0;
        public int fps = 0;
        public int pid = 0;
        public int uid = 0;
        public boolean isSystemApp = false;
        public String iconBase64 = "";
        public long installTime = 0;
        public long lastUpdateTime = 0;
        public long timestamp = System.currentTimeMillis();
    }

    public FrontendAppInfo getInfo() {
        FrontendAppInfo appInfo =  new FrontendAppInfo();
        try {
            // 获取前台应用完整路径
            String foregroundAppPath = getForegroundAppPath();
            if (foregroundAppPath != null && !foregroundAppPath.isEmpty()) {
                // 从完整路径中提取包名
                String packageName = extractPackageNameFromPath(foregroundAppPath);
                appInfo.packageName = packageName;
                appInfo.activityName = foregroundAppPath; // 保存完整的Activity路径

                // 获取应用详细信息
                getAppDetailsViaReflection(appInfo);

                // 获取应用内存使用情况
                getAppMemoryUsageViaReflection(appInfo);
            }

        } catch (Exception e) {
            Logger.e(TAG, "Error getting current app info: " + e.getMessage(), e);
        }

        return appInfo;
    }

    /**
     * 从完整路径中提取包名
     * 例如: tv.danmaku.bilibilihd/tv.danmaku.bili.MainActivityV2 -> tv.danmaku.bilibilihd
     */
    private String extractPackageNameFromPath(String fullPath) {
        if (fullPath != null && fullPath.contains("/")) {
            return fullPath.substring(0, fullPath.indexOf("/"));
        }
        return fullPath;
    }

    /**
     * 通过反射获取应用详细信息
     */
    private void getAppDetailsViaReflection(FrontendAppInfo appInfo) {
        try {
            PackageManager pm = ContextManager.getPackageManager();
            if (pm == null) {
                Logger.w(TAG, "PackageManager is null");
                return;
            }

            PackageInfo pkgInfo = pm.getPackageInfo(appInfo.packageName, 0);
            ApplicationInfo androidAppInfo = pkgInfo.applicationInfo;

            appInfo.appName = pm.getApplicationLabel(androidAppInfo).toString();
            appInfo.version = pkgInfo.versionName;
            appInfo.versionCode = String.valueOf(pkgInfo.getLongVersionCode());
            appInfo.isSystemApp = (androidAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            appInfo.installTime = pkgInfo.firstInstallTime;
            appInfo.lastUpdateTime = pkgInfo.lastUpdateTime;
            appInfo.uid = androidAppInfo.uid;

            // 处理应用图标 - 支持多种类型的Drawable
            Drawable icon = pm.getApplicationIcon(appInfo.packageName);
            Logger.d(TAG, "Icon type: " + icon.getClass().getSimpleName());

            String iconBase64 = drawableToBase64(icon);
            if (iconBase64 != null) {
                appInfo.iconBase64 = iconBase64;
            } else {
                Logger.w(TAG, "Failed to convert icon to base64");
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get app details: " + e.getMessage());
        }
    }

    /**
     * 将Drawable转换为Base64字符串，支持多种类型的Drawable
     */
    private String drawableToBase64(Drawable drawable) {
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

    /**
     * 获取应用内存使用情况
     */
    private void getAppMemoryUsageViaReflection(FrontendAppInfo appInfo) {

    }


    /**
     * 获取前台应用完整路径 (包名/Activity名)
     */
    private String getForegroundAppPath() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    "sh", "-c", "dumpsys activity activities"
            });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String targetLine = null;

            // 读取所有行，寻找包含topResumedActivity的行
            while ((line = br.readLine()) != null) {
                if (line.contains("topResumedActivity=")) {
                    targetLine = line.trim();
                    break;
                }
            }
            br.close();
            p.waitFor();

            if (targetLine != null) {
                // 解析格式: topResumedActivity=ActivityRecord{b945e78 u0 tv.danmaku.bilibilihd/tv.danmaku.bili.MainActivityV2 t85}

                int u0Index = targetLine.indexOf(" u0 ");
                if (u0Index != -1) {
                    int pathStart = u0Index + 4; // " u0 " 长度为4
                    // 查找路径的结束位置 - 可能是空格或者右括号
                    int pathEnd = targetLine.indexOf(" ", pathStart);
                    if (pathEnd == -1) {
                        pathEnd = targetLine.indexOf("}", pathStart);
                    }

                    if (pathEnd != -1) {
                        String fullPath = targetLine.substring(pathStart, pathEnd);
                        return fullPath;
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error getting foreground app path", e);
        }

        Logger.w(TAG, "无法解析前台应用路径");
        return null;
    }
}
