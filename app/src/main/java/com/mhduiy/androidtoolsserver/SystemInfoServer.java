package com.mhduiy.androidtoolsserver;

import com.mhduiy.androidtoolsserver.monitor.CPUMonitor;
import com.mhduiy.androidtoolsserver.monitor.FrontendAppMonitor;
import com.mhduiy.androidtoolsserver.monitor.GPUMonitor;
import com.mhduiy.androidtoolsserver.monitor.MemoryMonitor;
import com.mhduiy.androidtoolsserver.monitor.SystemMonitor;
import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.JsonBuilder;
import com.mhduiy.androidtoolsserver.http.HttpServer;

import java.util.Map;

/**
 * Android系统监控服务端主入口类
 * 通过app_process启动，提供HTTP API接口获取系统信息
 */
public class SystemInfoServer {
    private static final String TAG = "SystemInfoServer";
    private static final int DEFAULT_PORT = 18888;

    private SystemMonitor systemMonitor;
    private HttpServer httpServer;
    private int port;

    public SystemInfoServer() {
        this(DEFAULT_PORT);
    }

    public SystemInfoServer(int port) {
        this.port = port;
        Logger.i(TAG, "Initializing SystemInfoServer on port " + port);

        try {
            // 初始化系统监控器（使用文件系统访问）
            systemMonitor = new SystemMonitor();

            // 初始化HTTP服务器
            httpServer = new HttpServer(port, systemMonitor);

            Logger.i(TAG, "SystemInfoServer initialized successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Failed to initialize SystemInfoServer", e);
            throw new RuntimeException("SystemInfoServer initialization failed", e);
        }
    }

    /**
     * 获取当前应用信息的JSON字符串
     */
    public String getCurrentAppInfoJson() {
        try {
            FrontendAppMonitor.FrontendAppInfo currentAppInfo = systemMonitor.getFrontendAppInfo();

            JsonBuilder json = new JsonBuilder();
            json.add("packageName", currentAppInfo.packageName);
            json.add("appName", currentAppInfo.appName);
            json.add("activityName", currentAppInfo.activityName);
            json.add("version", currentAppInfo.version);
            json.add("versionCode", currentAppInfo.versionCode);
            json.add("memoryUsageMB", currentAppInfo.memoryUsageMB);
            json.add("cpuUsage", Math.round(currentAppInfo.cpuUsage * 100.0) / 100.0);
            json.add("fps", currentAppInfo.fps);
            json.add("pid", currentAppInfo.pid);
            json.add("uid", currentAppInfo.uid);
            json.add("isSystemApp", currentAppInfo.isSystemApp);
            json.add("iconBase64", currentAppInfo.iconBase64);
            json.add("installTime", currentAppInfo.installTime);
            json.add("lastUpdateTime", currentAppInfo.lastUpdateTime);
            json.add("timestamp", currentAppInfo.timestamp);

            return json.build();
        } catch (Exception e) {
            Logger.e(TAG, "Error getting current app info JSON", e);
            return new JsonBuilder()
                .add("error", "Failed to get current app info: " + e.getMessage())
                .add("timestamp", System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 获取CPU信息的JSON字符串
     */
    public String getCpuInfoJson() {
        try {
            CPUMonitor.CpuInfo cpuInfo = systemMonitor.getCpuInfo();

            JsonBuilder json = new JsonBuilder();
            json.add("model", cpuInfo.model);
            json.add("architecture", cpuInfo.architecture);
            json.add("coreCount", cpuInfo.coreCount);
            json.add("currentUsage", Math.round(cpuInfo.currentUsage * 100.0) / 100.0);
            json.add("coreUsages", cpuInfo.coreUsages);
            json.add("frequencies", cpuInfo.frequencies);
            json.add("temperature", cpuInfo.temperature);
            json.add("maxFrequency", cpuInfo.maxFrequency);
            json.add("minFrequency", cpuInfo.minFrequency);
            json.add("timestamp", System.currentTimeMillis());

            return json.build();
        } catch (Exception e) {
            Logger.e(TAG, "Error getting CPU info JSON", e);
            return new JsonBuilder()
                .add("error", "Failed to get CPU info: " + e.getMessage())
                .add("timestamp", System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 获取GPU信息的JSON字符串
     */
    public String getGpuInfoJson() {
        try {
            GPUMonitor.GpuInfo gpuInfo = systemMonitor.getGpuInfo();

            JsonBuilder json = new JsonBuilder();
            json.add("name", gpuInfo.name);
            json.add("vendor", gpuInfo.vendor);
            json.add("renderer", gpuInfo.renderer);
            json.add("version", gpuInfo.version);
            json.add("currentFrequency", gpuInfo.currentFrequency);
            json.add("maxFrequency", gpuInfo.maxFrequency);
            json.add("minFrequency", gpuInfo.minFrequency);
            json.add("usage", Math.round(gpuInfo.usage * 100.0) / 100.0);
            json.add("temperature", gpuInfo.temperature);
            json.add("timestamp", System.currentTimeMillis());

            return json.build();
        } catch (Exception e) {
            Logger.e(TAG, "Error getting GPU info JSON", e);
            return new JsonBuilder()
                .add("error", "Failed to get GPU info: " + e.getMessage())
                .add("timestamp", System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 获取内存信息的JSON字符串
     */
    public String getMemoryInfoJson() {
        try {
            MemoryMonitor.MemInfo memInfo = systemMonitor.getMemoryInfo();

            JsonBuilder json = new JsonBuilder();
            json.add("totalMemory", memInfo.totalMemory);
            json.add("availableMemory", memInfo.availableMemory);
            json.add("usedMemory", memInfo.usedMemory);
            json.add("memoryUsageRatio", Math.round(memInfo.memoryUsageRatio * 10000.0) / 100.0);
            json.add("threshold", memInfo.threshold);
            json.add("lowMemory", memInfo.lowMemory);
            json.add("totalStorage", memInfo.totalStorage);
            json.add("availableStorage", memInfo.availableStorage);
            json.add("usedStorage", memInfo.usedStorage);
            json.add("timestamp", System.currentTimeMillis());

            return json.build();
        } catch (Exception e) {
            Logger.e(TAG, "Error getting memory info JSON", e);
            return new JsonBuilder()
                .add("error", "Failed to get memory info: " + e.getMessage())
                .add("timestamp", System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 获取系统信息摘要的JSON字符串
     */
    public String getSystemSummaryJson() {
        try {
            Map<String, Object> summary = systemMonitor.getSystemSummary();
            return JsonBuilder.fromMap(summary);
        } catch (Exception e) {
            Logger.e(TAG, "Error getting system summary JSON", e);
            return new JsonBuilder()
                .add("error", "Failed to get system summary: " + e.getMessage())
                .add("timestamp", System.currentTimeMillis())
                .build();
        }
    }

    /**
     * 获取SystemMonitor实例
     */
    public SystemMonitor getSystemMonitor() {
        return systemMonitor;
    }

    public void start() {
        try {
            Logger.i(TAG, "Starting SystemInfoServer...");
            httpServer.start();
            Logger.i(TAG, "SystemInfoServer started on port " + port);
        } catch (Exception e) {
            Logger.e(TAG, "Failed to start SystemInfoServer", e);
            throw new RuntimeException("SystemInfoServer start failed", e);
        }
    }

    public void stop() {
        try {
            Logger.i(TAG, "Stopping SystemInfoServer...");
            if (httpServer != null) {
                httpServer.stop();
            }
            Logger.i(TAG, "SystemInfoServer stopped");
        } catch (Exception e) {
            Logger.e(TAG, "Error stopping SystemInfoServer", e);
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Invalid port number, using default: " + DEFAULT_PORT);
            }
        }

        try {
            Logger.i(TAG, "Starting SystemInfoServer via app_process...");

            SystemInfoServer server = new SystemInfoServer(port);
            server.start();

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.i(TAG, "Shutting down SystemInfoServer...");
                server.stop();
            }));

            Logger.i(TAG, "SystemInfoServer is running on port " + port + ". Press Ctrl+C to stop.");

            // 保持程序运行
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logger.i(TAG, "SystemInfoServer interrupted, shutting down...");
                    server.stop();
                    break;
                }
            }

        } catch (Exception e) {
            Logger.e(TAG, "Failed to start SystemInfoServer: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}

