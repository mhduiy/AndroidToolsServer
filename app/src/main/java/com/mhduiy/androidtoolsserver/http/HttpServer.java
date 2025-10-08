package com.mhduiy.androidtoolsserver.http;

import com.mhduiy.androidtoolsserver.monitor.CPUMonitor;
import com.mhduiy.androidtoolsserver.monitor.FrontendAppMonitor;
import com.mhduiy.androidtoolsserver.monitor.GPUMonitor;
import com.mhduiy.androidtoolsserver.monitor.MemoryMonitor;
import com.mhduiy.androidtoolsserver.monitor.SystemMonitor;
import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.JsonBuilder;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP服务器，处理系统信息API请求
 */
public class HttpServer {
    private static final String TAG = "HttpServer";

    private final int port;
    private final SystemMonitor systemMonitor;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    public HttpServer(int port, SystemMonitor systemMonitor) {
        this.port = port;
        this.systemMonitor = systemMonitor;
        this.executor = Executors.newFixedThreadPool(10);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Logger.i(TAG, "HTTP Server started on port " + port);

        // 启动接受连接的线程
        new Thread(this::acceptConnections).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdown();
            }
        } catch (IOException e) {
            Logger.e(TAG, "Error stopping HTTP server", e);
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    Logger.e(TAG, "Error accepting connection", e);
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // 读取HTTP请求行
            String requestLine = reader.readLine();
            if (requestLine == null) return;

            Logger.d(TAG, "Request: " + requestLine);

            // 跳过HTTP头部
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // 读取并忽略HTTP头部
            }

            // 解析请求
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length >= 2) {
                String method = requestParts[0];
                String path = requestParts[1];

                if ("GET".equals(method)) {
                    handleGetRequest(writer, path);
                } else {
                    sendErrorResponse(writer, 405, "Method Not Allowed");
                }
            } else {
                sendErrorResponse(writer, 400, "Bad Request");
            }

        } catch (IOException e) {
            Logger.e(TAG, "Error handling client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.w(TAG, "Error closing client socket", e);
            }
        }
    }

    private void handleGetRequest(PrintWriter writer, String path) {
        String response;
        String contentType = "application/json";

        try {
            switch (path) {
                case "/":
                case "/status":
                    response = new JsonBuilder()
                        .add("status", "running")
                        .add("service", "AndroidToolsServer")
                        .add("version", "1.0.0")
                        .add("timestamp", System.currentTimeMillis())
                        .build();
                    break;

                case "/cpu":
                    CPUMonitor.CpuInfo cpuInfo = systemMonitor.getCpuInfo();
                    JsonBuilder cpuJson = new JsonBuilder();
                    cpuJson.add("model", cpuInfo.model);
                    cpuJson.add("architecture", cpuInfo.architecture);
                    cpuJson.add("coreCount", cpuInfo.coreCount);
                    cpuJson.add("currentUsage", Math.round(cpuInfo.currentUsage * 100.0) / 100.0);

                    // 添加各个核心的详细信息作为JSON对象数组
                    StringBuilder coresJson = new StringBuilder();
                    coresJson.append("[");
                    for (int i = 0; i < cpuInfo.cores.size(); i++) {
                        CPUMonitor.CpuCoreInfo core = cpuInfo.cores.get(i);
                        if (i > 0) coresJson.append(",");
                        coresJson.append("{")
                                .append("\"coreId\":").append(core.coreId).append(",")
                                .append("\"usage\":").append(Math.round(core.usage * 100.0) / 100.0).append(",")
                                .append("\"frequency\":").append(core.frequency)
                                .append("}");
                    }
                    coresJson.append("]");

                    cpuJson.add("cores", coresJson.toString(), false); // false表示不加引号，直接作为JSON对象
                    cpuJson.add("temperature", cpuInfo.temperature);
                    cpuJson.add("maxFrequency", cpuInfo.maxFrequency);
                    cpuJson.add("minFrequency", cpuInfo.minFrequency);
                    cpuJson.add("timestamp", System.currentTimeMillis());
                    response = cpuJson.build();
                    break;

                case "/memory":
                    MemoryMonitor.MemInfo memInfo = systemMonitor.getMemoryInfo();
                    JsonBuilder memJson = new JsonBuilder();
                    memJson.add("totalMemory", memInfo.totalMemory);
                    memJson.add("availableMemory", memInfo.availableMemory);
                    memJson.add("usedMemory", memInfo.usedMemory);
                    memJson.add("memoryUsageRatio", Math.round(memInfo.memoryUsageRatio * 10000.0) / 100.0);
                    memJson.add("threshold", memInfo.threshold);
                    memJson.add("lowMemory", memInfo.lowMemory);
                    memJson.add("totalStorage", memInfo.totalStorage);
                    memJson.add("availableStorage", memInfo.availableStorage);
                    memJson.add("usedStorage", memInfo.usedStorage);
                    memJson.add("timestamp", System.currentTimeMillis());
                    response = memJson.build();
                    break;

                case "/gpu":
                    GPUMonitor.GpuInfo gpuInfo = systemMonitor.getGpuInfo();
                    JsonBuilder gpuJson = new JsonBuilder();
                    gpuJson.add("name", gpuInfo.name);
                    gpuJson.add("vendor", gpuInfo.vendor);
                    gpuJson.add("renderer", gpuInfo.renderer);
                    gpuJson.add("version", gpuInfo.version);
                    gpuJson.add("currentFrequency", gpuInfo.currentFrequency);
                    gpuJson.add("maxFrequency", gpuInfo.maxFrequency);
                    gpuJson.add("minFrequency", gpuInfo.minFrequency);
                    gpuJson.add("usage", Math.round(gpuInfo.usage * 100.0) / 100.0);
                    gpuJson.add("temperature", gpuInfo.temperature);
                    gpuJson.add("timestamp", System.currentTimeMillis());
                    response = gpuJson.build();
                    break;

                case "/current-app":
                case "/current":
                    FrontendAppMonitor.FrontendAppInfo currentApp = systemMonitor.getFrontendAppInfo();
                    JsonBuilder currentAppJson = new JsonBuilder();
                    currentAppJson.add("packageName", currentApp.packageName);
                    currentAppJson.add("appName", currentApp.appName);
                    currentAppJson.add("activityName", currentApp.activityName);
                    currentAppJson.add("version", currentApp.version);
                    currentAppJson.add("versionCode", currentApp.versionCode);
                    currentAppJson.add("memoryUsageMB", currentApp.memoryUsageMB);
                    currentAppJson.add("cpuUsage", Math.round(currentApp.cpuUsage * 100.0) / 100.0);
                    currentAppJson.add("fps", currentApp.fps);
                    currentAppJson.add("pid", currentApp.pid);
                    currentAppJson.add("uid", currentApp.uid);
                    currentAppJson.add("isSystemApp", currentApp.isSystemApp);
                    currentAppJson.add("iconBase64", currentApp.iconBase64);
                    currentAppJson.add("installTime", currentApp.installTime);
                    currentAppJson.add("lastUpdateTime", currentApp.lastUpdateTime);
                    currentAppJson.add("timestamp", currentApp.timestamp);
                    response = currentAppJson.build();
                    break;

                case "/system":
                case "/summary":
                    Map<String, Object> summary = systemMonitor.getSystemSummary();
                    response = JsonBuilder.fromMap(summary);
                    break;

                case "/api":
                    contentType = "text/html";
                    response = getApiDocumentation();
                    break;

                default:
                    sendErrorResponse(writer, 404, "Not Found");
                    return;
            }

            sendSuccessResponse(writer, response, contentType);

        } catch (Exception e) {
            Logger.e(TAG, "Error processing request: " + path, e);
            sendErrorResponse(writer, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void sendSuccessResponse(PrintWriter writer, String content, String contentType) {
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType + "; charset=utf-8");
        writer.println("Content-Length: " + content.getBytes().length);
        writer.println("Access-Control-Allow-Origin: *");
        writer.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
        writer.println("Access-Control-Allow-Headers: Content-Type");
        writer.println();
        writer.println(content);
        writer.flush();
    }

    private void sendErrorResponse(PrintWriter writer, int statusCode, String statusText) {
        String errorJson = new JsonBuilder()
            .add("error", statusText)
            .add("code", statusCode)
            .add("timestamp", System.currentTimeMillis())
            .build();

        writer.println("HTTP/1.1 " + statusCode + " " + statusText);
        writer.println("Content-Type: application/json; charset=utf-8");
        writer.println("Content-Length: " + errorJson.getBytes().length);
        writer.println("Access-Control-Allow-Origin: *");
        writer.println();
        writer.println(errorJson);
        writer.flush();
    }

    private String getApiDocumentation() {
        return "<!DOCTYPE html>" +
               "<html><head><title>AndroidToolsServer API</title></head>" +
               "<body>" +
               "<h1>AndroidToolsServer API Documentation</h1>" +
               "<h2>Available Endpoints:</h2>" +
               "<ul>" +
               "<li><strong>GET /</strong> - Server status</li>" +
               "<li><strong>GET /status</strong> - Server status (same as /)</li>" +
               "<li><strong>GET /cpu</strong> - CPU information (model, usage, frequency, temperature)</li>" +
               "<li><strong>GET /gpu</strong> - GPU information (name, usage, frequency, temperature)</li>" +
               "<li><strong>GET /memory</strong> - Memory information (total, available, swap)</li>" +
               "<li><strong>GET /foreground</strong> - Foreground app information (primary app, top processes)</li>" +
               "<li><strong>GET /app</strong> - Foreground app information (same as /foreground)</li>" +
               "<li><strong>GET /current-app</strong> - Current foreground app detailed information</li>" +
               "<li><strong>GET /current</strong> - Current foreground app detailed information (same as /current-app)</li>" +
               "<li><strong>GET /processes</strong> - 当前运行的所有进程信息</li>" +
               "<li><strong>GET /system</strong> - Complete system summary</li>" +
               "<li><strong>GET /summary</strong> - Complete system summary (same as /system)</li>" +
               "<li><strong>GET /api</strong> - This API documentation</li>" +
               "</ul>" +
               "<h2>Response Format:</h2>" +
               "<p>All responses are in JSON format with CORS headers enabled.</p>" +
               "<h2>Example Usage:</h2>" +
               "<pre>" +
               "curl http://localhost:" + port + "/cpu\n" +
               "curl http://localhost:" + port + "/gpu\n" +
               "curl http://localhost:" + port + "/memory\n" +
               "curl http://localhost:" + port + "/foreground\n" +
               "curl http://localhost:" + port + "/system" +
               "</pre>" +
               "</body></html>";
    }
}
