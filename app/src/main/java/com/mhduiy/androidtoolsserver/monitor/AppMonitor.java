package com.mhduiy.androidtoolsserver.monitor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mhduiy.androidtoolsserver.util.ContextManager;
import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AppMonitor {
    private static final String TAG = "AppMonitor";

    public static class AppBaseInfo {
        public String packageName;
        public String appName;
        public String versionName;
        public int versionCode;
        public boolean isSystemApp;
        public boolean isEnabled;
        public long firstInstallTime;
        public long lastUpdateTime;
    }

    public List<AppBaseInfo> getAllApps() {
        List<AppBaseInfo> apps = new ArrayList<>();
        Context context = ContextManager.getContext();

        if (context == null) {
            Log.e(TAG, "Context is null, cannot get installed apps");
            return apps;
        }

        try {
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);

            for (PackageInfo packageInfo : packages) {
                AppBaseInfo appInfo = new AppBaseInfo();

                // 基本包信息
                appInfo.packageName = packageInfo.packageName;
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = packageInfo.versionCode;
                appInfo.firstInstallTime = packageInfo.firstInstallTime;
                appInfo.lastUpdateTime = packageInfo.lastUpdateTime;

                // 获取应用名称
                try {
                    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                    appInfo.appName = pm.getApplicationLabel(applicationInfo).toString();
                    appInfo.isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    appInfo.isEnabled = applicationInfo.enabled;
                } catch (Exception e) {
                    appInfo.appName = packageInfo.packageName;
                    appInfo.isSystemApp = false;
                    appInfo.isEnabled = true;
                }

                apps.add(appInfo);
            }

            Log.i(TAG, "Successfully retrieved " + apps.size() + " installed apps");

        } catch (Exception e) {
            Log.e(TAG, "Failed to get installed apps: " + e.getMessage());
        }

        return apps;
    }

    /**
     * 获取用户安装的应用（排除系统应用）
     */
    public List<AppBaseInfo> getUserApps() {
        List<AppBaseInfo> allApps = getAllApps();
        List<AppBaseInfo> userApps = new ArrayList<>();

        for (AppBaseInfo app : allApps) {
            if (!app.isSystemApp) {
                userApps.add(app);
            }
        }

        return userApps;
    }

    public static String getIconBase64(String packageName) {
        Logger.e(TAG, "Getting icon for package: " + packageName);
        Context context = ContextManager.getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, cannot get app icon");
            return null;
        }

        try {
            PackageManager pm = context.getPackageManager();
            Drawable icon = pm.getApplicationIcon(packageName);
            return Utils.drawableToBase64(icon);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get app icon for " + packageName + ": " + e.getMessage());
        }

        return null;
    }
}
