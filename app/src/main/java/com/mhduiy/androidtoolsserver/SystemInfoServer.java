package com.mhduiy.androidtoolsserver;

import com.mhduiy.androidtoolsserver.http.HttpServer;
import com.mhduiy.androidtoolsserver.monitor.SystemMonitor;
import com.mhduiy.androidtoolsserver.util.Logger;

/**
 * Android系统监控服务端主入口类
 * 通过app_process启动，提供HTTP API接口获取系统信息
 */
public final class SystemInfoServer {

    private static final String TAG = "SystemInfoServer";
    private static final int DEFAULT_PORT = 18888;

    private SystemInfoServer() {
        // 不可实例化
    }

    public static void main(String[] args) {
        System.out.println("SystemInfoServer starting...");
        System.out.flush();

        try {
            Logger.i(TAG, "SystemInfoServer initializing...");

            int port = DEFAULT_PORT;

            // 解析命令行参数
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                    if (port < 1024 || port > 65535) {
                        Logger.e(TAG, "Invalid port: " + port + ", using default port: " + DEFAULT_PORT);
                        port = DEFAULT_PORT;
                    }
                } catch (NumberFormatException e) {
                    Logger.e(TAG, "Invalid port format: " + args[0] + ", using default port: " + DEFAULT_PORT);
                }
            }

            Logger.i(TAG, "Using port: " + port);

            // 初始化系统监控器
            SystemMonitor systemMonitor = new SystemMonitor();

            // 创建并启动HTTP服务器
            HttpServer httpServer = new HttpServer(port, systemMonitor);
            httpServer.start();

            Logger.i(TAG, "SystemInfoServer started successfully on port: " + port);
            System.out.println("Server is running on port " + port);
            System.out.println("Access the API at: http://device_ip:" + port + "/");

            // 保持主线程运行
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.i(TAG, "SystemInfoServer shutting down...");
                httpServer.stop();
            }));

            // 使用简单的循环保持程序运行
            try {
                while (true) {
                    Thread.sleep(5000); // 每5秒输出一次状态
                    System.out.println("Server running... (Port: " + port + ")");
                }
            } catch (InterruptedException e) {
                Logger.i(TAG, "SystemInfoServer interrupted, shutting down...");
            }

        } catch (Exception e) {
            System.err.println("Error starting SystemInfoServer: " + e.getMessage());
            Logger.e(TAG, "Failed to start SystemInfoServer: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
