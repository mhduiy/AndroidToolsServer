package com.mhduiy.androidtoolsserver.util;

/**
 * 简单的日志工具类
 */
public class Logger {
    private static final String TAG_PREFIX = "SystemInfo";
    private static boolean debugMode = true;

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }

    public static void d(String tag, String message) {
        if (debugMode) {
            System.out.println(formatLog("D", tag, message));
        }
    }

    public static void i(String tag, String message) {
        System.out.println(formatLog("I", tag, message));
    }

    public static void w(String tag, String message) {
        System.out.println(formatLog("W", tag, message));
    }

    public static void e(String tag, String message) {
        System.err.println(formatLog("E", tag, message));
    }

    public static void e(String tag, String message, Throwable throwable) {
        System.err.println(formatLog("E", tag, message));
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    private static String formatLog(String level, String tag, String message) {
        long timestamp = System.currentTimeMillis();
        return String.format("[%d] %s/%s-%s: %s",
                timestamp, level, TAG_PREFIX, tag, message);
    }
}
