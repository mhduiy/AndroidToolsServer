package com.mhduiy.androidtoolsserver.util;

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
}
