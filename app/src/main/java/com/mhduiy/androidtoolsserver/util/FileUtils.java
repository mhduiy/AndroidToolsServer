package com.mhduiy.androidtoolsserver.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 文件操作工具类
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容，如果读取失败返回null
     */
    public static String readFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                return null;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead = fis.read(buffer);

                if (bytesRead > 0) {
                    return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                }
            }
        } catch (IOException e) {
            Logger.d(TAG, "Failed to read file: " + filePath + ", error: " + e.getMessage());
        }
        return null;
    }

    /**
     * 读取文件所有行
     * @param filePath 文件路径
     * @return 文件行数组，如果读取失败返回null
     */
    public static String[] readLines(String filePath) {
        String content = readFile(filePath);
        if (content != null) {
            return content.split("\n");
        }
        return null;
    }

    /**
     * 写入文件
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 是否写入成功
     */
    public static boolean writeFile(String filePath, String content) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            Logger.e(TAG, "Failed to write file: " + filePath + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查文件是否存在且可读
     * @param filePath 文件路径
     * @return 是否存在且可读
     */
    public static boolean isReadable(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.canRead();
    }

    /**
     * 执行shell命令并获取输出
     * @param command 命令
     * @return 命令输出，如果执行失败返回null
     */
    public static String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                process.waitFor();
                return output.toString().trim();
            }
        } catch (Exception e) {
            Logger.d(TAG, "Failed to execute command: " + command + ", error: " + e.getMessage());
            return null;
        }
    }
}
