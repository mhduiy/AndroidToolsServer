package com.mhduiy.androidtoolsserver.util;

import android.util.Log;

/**
 * 简单的日志工具类
 */
public class Logger {
    private static boolean DEBUG = true;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
            System.out.println("[DEBUG] " + tag + ": " + message);
        }
    }

    public static void i(String tag, String message) {
        if (DEBUG) {
            Log.i(tag, message);
            System.out.println("[INFO] " + tag + ": " + message);
        }
    }

    public static void w(String tag, String message) {
        if (DEBUG) {
            Log.w(tag, message);
            System.out.println("[WARN] " + tag + ": " + message);
        }
    }

    public static void w(String tag, String message, Throwable throwable) {
        if (DEBUG) {
            Log.w(tag, message);
            System.out.println("[WARN] " + tag + ": " + message);
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        System.err.println("[ERROR] " + tag + ": " + message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        System.err.println("[ERROR] " + tag + ": " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
