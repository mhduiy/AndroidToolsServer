package com.mhduiy.androidtoolsserver.util;

import android.content.Context;

/**
 * 全局Context管理器
 * 用于在app_process环境中管理系统Context
 */
public class ContextManager {
    private static final String TAG = "ContextManager";
    private static Context systemContext;
    private static boolean initialized = false;

    /**
     * 初始化系统Context
     * @param context 系统Context
     */
    public static synchronized void initialize(Context context) {
        if (context == null) {
            Logger.w(TAG, "Trying to initialize with null context");
            return;
        }

        systemContext = context;
        initialized = true;
        Logger.i(TAG, "ContextManager initialized with system context");
    }

    /**
     * 获取系统Context
     * @return 系统Context，如果未初始化则返回null
     */
    public static Context getContext() {
        if (!initialized || systemContext == null) {
            Logger.w(TAG, "ContextManager not initialized or context is null");
            return null;
        }
        return systemContext;
    }

    /**
     * 检查Context是否已初始化
     * @return true如果已初始化，false否则
     */
    public static boolean isInitialized() {
        return initialized && systemContext != null;
    }

    /**
     * 清理Context（一般不需要调用）
     */
    public static synchronized void clear() {
        systemContext = null;
        initialized = false;
        Logger.i(TAG, "ContextManager cleared");
    }

    /**
     * 获取PackageManager
     * @return PackageManager实例，如果Context未初始化则返回null
     */
    public static android.content.pm.PackageManager getPackageManager() {
        Context context = getContext();
        if (context != null) {
            return context.getPackageManager();
        }
        Logger.w(TAG, "Cannot get PackageManager: Context not initialized");
        return null;
    }

    /**
     * 获取ActivityManager
     * @return ActivityManager实例，如果Context未初始化则返回null
     */
    public static android.app.ActivityManager getActivityManager() {
        Context context = getContext();
        if (context != null) {
            return (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        Logger.w(TAG, "Cannot get ActivityManager: Context not initialized");
        return null;
    }
}
