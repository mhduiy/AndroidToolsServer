package com.mhduiy.androidtoolsserver.http;

import com.mhduiy.androidtoolsserver.monitor.SystemMonitor;
import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.JsonBuilder;

// Android SDK导入
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP服务器，处理系统信息API请求
 */
public class HttpServer {
    private static final String TAG = "HttpServer";

    private final int port;
    private final SystemMonitor systemMonitor;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public HttpServer(int port, SystemMonitor systemMonitor) {
        this.port = port;
        this.systemMonitor = systemMonitor;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running.set(true);

        new Thread(this::serverLoop, "HttpServer").start();
        Logger.i(TAG, "HTTP Server started on port: " + port);
    }

    public void stop() {
        running.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error closing server socket: " + e.getMessage());
            }
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        Logger.i(TAG, "HTTP Server stopped");
    }

    private void serverLoop() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    Logger.e(TAG, "Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = reader.readLine();
            if (requestLine == null) return;

            Logger.d(TAG, "Request: " + requestLine);

            // 简单的HTTP请求解析
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String path = parts[1];

            // 跳过HTTP头部
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // 跳过头部行
            }

            if (!"GET".equals(method)) {
                sendErrorResponse(writer, 405, "Method Not Allowed");
                return;
            }

            handleGetRequest(path, writer);

        } catch (IOException e) {
            Logger.e(TAG, "Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleGetRequest(String path, PrintWriter writer) {
        String response = "";

        try {
            switch (path) {
                case "/":
                case "/status":
                    response = createStatusResponse();
                    break;
                case "/androidapi":
                    response = testAndroidApiAccess();
                    break;
                case "/cpu":
                    response = systemMonitor.getCpuInfo();
                    break;
                case "/gpu":
                    response = systemMonitor.getGpuInfo();
                    break;
                case "/memory":
                    response = systemMonitor.getMemoryInfo();
                    break;
                case "/battery":
                    response = systemMonitor.getBatteryInfo();
                    break;
                case "/display":
                    response = systemMonitor.getDisplayInfo();
                    break;
                case "/system":
                    response = systemMonitor.getSystemInfo();
                    break;
                case "/all":
                    response = systemMonitor.getAllInfo();
                    break;
                default:
                    sendErrorResponse(writer, 404, "Not Found");
                    return;
            }

            sendJsonResponse(writer, response);

        } catch (Exception e) {
            Logger.e(TAG, "Error processing request for path: " + path + ", error: " + e.getMessage());
            sendErrorResponse(writer, 500, "Internal Server Error");
        }
    }

    private String createStatusResponse() {
        return new JsonBuilder()
                .put("status", "running")
                .put("server", "SystemInfoServer")
                .put("version", "1.0.0")
                .put("timestamp", System.currentTimeMillis())
                .build();
    }

    private void sendJsonResponse(PrintWriter writer, String json) {
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: application/json; charset=utf-8");
        writer.println("Access-Control-Allow-Origin: *");
        writer.println("Connection: close");
        writer.println("Content-Length: " + json.getBytes().length);
        writer.println();
        writer.println(json);
    }

    private void sendErrorResponse(PrintWriter writer, int statusCode, String statusText) {
        String json = new JsonBuilder()
                .put("error", statusText)
                .put("code", statusCode)
                .put("timestamp", System.currentTimeMillis())
                .build();

        writer.println("HTTP/1.1 " + statusCode + " " + statusText);
        writer.println("Content-Type: application/json; charset=utf-8");
        writer.println("Access-Control-Allow-Origin: *");
        writer.println("Connection: close");
        writer.println("Content-Length: " + json.getBytes().length);
        writer.println();
        writer.println(json);
    }

    private String testAndroidApiAccess() {
        JsonBuilder result = new JsonBuilder();
        result.put("test", "Android API Access Test");
        result.put("timestamp", System.currentTimeMillis());

        // 测试Android Build类直接访问
        try {
            android.os.Build build = new android.os.Build();

            JsonBuilder buildInfo = new JsonBuilder();
            buildInfo.put("manufacturer", android.os.Build.MANUFACTURER);
            buildInfo.put("model", android.os.Build.MODEL);
            buildInfo.put("brand", android.os.Build.BRAND);
            buildInfo.put("device", android.os.Build.DEVICE);
            buildInfo.put("product", android.os.Build.PRODUCT);
            buildInfo.put("hardware", android.os.Build.HARDWARE);
            buildInfo.put("board", android.os.Build.BOARD);
            buildInfo.put("bootloader", android.os.Build.BOOTLOADER);
            buildInfo.put("fingerprint", android.os.Build.FINGERPRINT);
            buildInfo.put("host", android.os.Build.HOST);
            buildInfo.put("id", android.os.Build.ID);
            buildInfo.put("display", android.os.Build.DISPLAY);
            buildInfo.put("user", android.os.Build.USER);

            result.put("build_info", buildInfo.build());
            result.put("build_info_status", "success");
        } catch (Exception e) {
            result.put("build_info_status", "failed");
            result.put("build_info_error", e.getMessage());
        }

        return result.build();
    }
}
