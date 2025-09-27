package com.mhduiy.androidtoolsserver.monitor;

import com.mhduiy.androidtoolsserver.util.JsonBuilder;
import com.mhduiy.androidtoolsserver.util.Logger;
import com.mhduiy.androidtoolsserver.util.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统监控器，负责收集各种系统信息
 */
public class SystemMonitor {
    private static final String TAG = "SystemMonitor";

    // CPU相关文件路径
    private static final String PROC_STAT = "/proc/stat";
    private static final String PROC_CPUINFO = "/proc/cpuinfo";
    private static final String CPU_FREQ_PATH = "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq";
    private static final String CPU_TEMP_PATH = "/sys/class/thermal/thermal_zone0/temp";

    // GPU相关文件路径
    private static final String GPU_LOAD_PATH = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage";
    private static final String GPU_FREQ_PATH = "/sys/class/kgsl/kgsl-3d0/gpuclk";
    private static final String GPU_TEMP_PATH = "/sys/class/thermal/thermal_zone1/temp";

    // 内存相关文件路径
    private static final String PROC_MEMINFO = "/proc/meminfo";

    // 电池相关路径
    private static final String BATTERY_PATH = "/sys/class/power_supply/battery/";

    // 应用相关路径
    private static final String PROC_PATH = "/proc/";

    private long lastCpuTotal = 0;
    private long lastCpuIdle = 0;
    private Map<Integer, Long[]> lastCpuCoreStats = new HashMap<>();

    public SystemMonitor() {
        try {
            Logger.i(TAG, "SystemMonitor initializing...");

            // 测试基本的文件访问能力
            String testResult = FileUtils.readFile("/proc/version");
            if (testResult != null) {
                Logger.i(TAG, "Basic file access works");
            } else {
                Logger.w(TAG, "Cannot access /proc/version");
            }

            Logger.i(TAG, "SystemMonitor initialized successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Error initializing SystemMonitor: " + e.getMessage(), e);
            throw new RuntimeException("SystemMonitor initialization failed", e);
        }
    }

    /**
     * 获取CPU信息
     */
    public String getCpuInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            // CPU整体使用率
            double cpuUsage = getCpuUsage();
            json.put("overall_usage", cpuUsage);

            // CPU温度
            double cpuTemp = getCpuTemperature();
            json.put("temperature", cpuTemp);

            // 各核心信息
            JsonBuilder coresJson = new JsonBuilder();
            int coreCount = getCpuCoreCount();

            for (int i = 0; i < coreCount; i++) {
                JsonBuilder coreJson = new JsonBuilder();
                coreJson.put("frequency", getCpuCoreFrequency(i));
                coreJson.put("usage", getCpuCoreUsage(i));
                coresJson.put("core_" + i, coreJson.buildObject());
            }
            json.put("cores", coresJson.buildObject());

            json.put("core_count", coreCount);
            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting CPU info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取GPU信息
     */
    public String getGpuInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            // GPU使用率
            double gpuUsage = getGpuUsage();
            json.put("usage", gpuUsage);

            // GPU频率
            long gpuFreq = getGpuFrequency();
            json.put("frequency", gpuFreq);

            // GPU温度
            double gpuTemp = getGpuTemperature();
            json.put("temperature", gpuTemp);

            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting GPU info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取内存信息
     */
    public String getMemoryInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            Map<String, Long> memInfo = parseMemInfo();

            long totalMem = memInfo.getOrDefault("MemTotal", 0L) * 1024; // Convert to bytes
            long availableMem = memInfo.getOrDefault("MemAvailable", 0L) * 1024;
            long freeMem = memInfo.getOrDefault("MemFree", 0L) * 1024;
            long buffers = memInfo.getOrDefault("Buffers", 0L) * 1024;
            long cached = memInfo.getOrDefault("Cached", 0L) * 1024;

            long usedMem = totalMem - availableMem;
            double usagePercent = totalMem > 0 ? (double) usedMem / totalMem * 100 : 0;

            json.put("total", totalMem);
            json.put("used", usedMem);
            json.put("free", freeMem);
            json.put("available", availableMem);
            json.put("buffers", buffers);
            json.put("cached", cached);
            json.put("usage_percent", usagePercent);
            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting memory info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取电池信息
     */
    public String getBatteryInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            Map<String, String> batteryInfo = getBatteryStatus();

            json.put("level", Integer.parseInt(batteryInfo.getOrDefault("level", "0")));
            json.put("temperature", Integer.parseInt(batteryInfo.getOrDefault("temperature", "0")) / 10.0);
            json.put("voltage", Integer.parseInt(batteryInfo.getOrDefault("voltage", "0")) / 1000.0);
            json.put("current", Integer.parseInt(batteryInfo.getOrDefault("current_now", "0")) / 1000.0);
            json.put("power", calculateBatteryPower(batteryInfo));
            json.put("status", batteryInfo.getOrDefault("status", "Unknown"));
            json.put("health", batteryInfo.getOrDefault("health", "Unknown"));
            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting battery info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取显示信息（FPS、当前应用等）
     */
    public String getDisplayInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            // 实时FPS
            double fps = getCurrentFPS();
            json.put("current_fps", fps);

            // 1% LOW帧率（简化实现）
            double lowFps = fps * 0.8; // 简化计算
            json.put("one_percent_low", lowFps);

            // 当前前台应用信息
            JsonBuilder appJson = getCurrentAppInfo();
            json.put("current_app", appJson.buildObject());

            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting display info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取系统信息
     */
    public String getSystemInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            // 从系统属性文件读取信息
            Map<String, String> buildProps = getBuildProperties();

            json.put("android_version", buildProps.getOrDefault("ro.build.version.release", "Unknown"));
            json.put("api_level", buildProps.getOrDefault("ro.build.version.sdk", "0"));
            json.put("device_model", buildProps.getOrDefault("ro.product.model", "Unknown"));
            json.put("device_manufacturer", buildProps.getOrDefault("ro.product.manufacturer", "Unknown"));
            json.put("device_brand", buildProps.getOrDefault("ro.product.brand", "Unknown"));
            json.put("device_product", buildProps.getOrDefault("ro.product.name", "Unknown"));
            json.put("kernel_version", getKernelVersion());
            json.put("uptime", getSystemUptime());
            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting system info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    /**
     * 获取所有信息
     */
    public String getAllInfo() {
        JsonBuilder json = new JsonBuilder();

        try {
            // 解析各个部分的JSON并合并
            String cpuJson = getCpuInfo();
            String gpuJson = getGpuInfo();
            String memoryJson = getMemoryInfo();
            String batteryJson = getBatteryInfo();
            String displayJson = getDisplayInfo();
            String systemJson = getSystemInfo();

            json.putRaw("cpu", cpuJson);
            json.putRaw("gpu", gpuJson);
            json.putRaw("memory", memoryJson);
            json.putRaw("battery", batteryJson);
            json.putRaw("display", displayJson);
            json.putRaw("system", systemJson);
            json.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting all info: " + e.getMessage());
            json.put("error", e.getMessage());
        }

        return json.build();
    }

    // === 私有辅助方法 ===

    private double getCpuUsage() {
        try {
            String statContent = FileUtils.readFile(PROC_STAT);
            if (statContent == null) return -1;

            String[] lines = statContent.split("\n");
            if (lines.length == 0) return -1;

            String cpuLine = lines[0];
            String[] values = cpuLine.split("\\s+");

            if (values.length < 8) return -1;

            long user = Long.parseLong(values[1]);
            long nice = Long.parseLong(values[2]);
            long system = Long.parseLong(values[3]);
            long idle = Long.parseLong(values[4]);
            long iowait = Long.parseLong(values[5]);
            long irq = Long.parseLong(values[6]);
            long softirq = Long.parseLong(values[7]);

            long totalCpu = user + nice + system + idle + iowait + irq + softirq;

            if (lastCpuTotal != 0) {
                long totalDiff = totalCpu - lastCpuTotal;
                long idleDiff = idle - lastCpuIdle;

                if (totalDiff > 0) {
                    double usage = (double) (totalDiff - idleDiff) / totalDiff * 100;
                    lastCpuTotal = totalCpu;
                    lastCpuIdle = idle;
                    return usage;
                }
            }

            lastCpuTotal = totalCpu;
            lastCpuIdle = idle;
            return 0;

        } catch (Exception e) {
            Logger.e(TAG, "Error calculating CPU usage: " + e.getMessage());
            return -1;
        }
    }

    private double getCpuTemperature() {
        try {
            String tempStr = FileUtils.readFile(CPU_TEMP_PATH);
            if (tempStr != null) {
                return Double.parseDouble(tempStr.trim()) / 1000.0; // Convert to Celsius
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not read CPU temperature: " + e.getMessage());
        }
        return -1;
    }

    private int getCpuCoreCount() {
        try {
            String cpuInfo = FileUtils.readFile(PROC_CPUINFO);
            if (cpuInfo == null) return Runtime.getRuntime().availableProcessors();

            int count = 0;
            String[] lines = cpuInfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("processor")) {
                    count++;
                }
            }
            return count > 0 ? count : Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            return Runtime.getRuntime().availableProcessors();
        }
    }

    private long getCpuCoreFrequency(int core) {
        try {
            String freqPath = String.format(CPU_FREQ_PATH, core);
            String freqStr = FileUtils.readFile(freqPath);
            if (freqStr != null) {
                return Long.parseLong(freqStr.trim()); // in kHz
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not read CPU core " + core + " frequency: " + e.getMessage());
        }
        return -1;
    }

    private double getCpuCoreUsage(int core) {
        // 简化实现，实际需要读取每个核心的stat信息
        return -1; // 暂时返回-1表示不可用
    }

    private double getGpuUsage() {
        try {
            String usageStr = FileUtils.readFile(GPU_LOAD_PATH);
            if (usageStr != null) {
                return Double.parseDouble(usageStr.trim());
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not read GPU usage: " + e.getMessage());
        }
        return -1;
    }

    private long getGpuFrequency() {
        try {
            String freqStr = FileUtils.readFile(GPU_FREQ_PATH);
            if (freqStr != null) {
                return Long.parseLong(freqStr.trim());
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not read GPU frequency: " + e.getMessage());
        }
        return -1;
    }

    private double getGpuTemperature() {
        try {
            String tempStr = FileUtils.readFile(GPU_TEMP_PATH);
            if (tempStr != null) {
                return Double.parseDouble(tempStr.trim()) / 1000.0;
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not read GPU temperature: " + e.getMessage());
        }
        return -1;
    }

    private Map<String, Long> parseMemInfo() throws IOException {
        Map<String, Long> memInfo = new HashMap<>();
        String content = FileUtils.readFile(PROC_MEMINFO);
        if (content == null) return memInfo;

        String[] lines = content.split("\n");
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String valueStr = parts[1].trim().replaceAll("[^0-9]", "");
                if (!valueStr.isEmpty()) {
                    memInfo.put(key, Long.parseLong(valueStr));
                }
            }
        }
        return memInfo;
    }

    private Map<String, String> getBatteryStatus() {
        Map<String, String> batteryInfo = new HashMap<>();

        try {
            // 尝试从系统文件读取电池信息
            String[] batteryFiles = {
                BATTERY_PATH + "capacity",
                BATTERY_PATH + "temp",
                BATTERY_PATH + "voltage_now",
                BATTERY_PATH + "current_now",
                BATTERY_PATH + "status",
                BATTERY_PATH + "health"
            };

            String[] keys = {"level", "temperature", "voltage", "current_now", "status", "health"};

            for (int i = 0; i < batteryFiles.length; i++) {
                String value = FileUtils.readFile(batteryFiles[i]);
                if (value != null) {
                    batteryInfo.put(keys[i], value.trim());
                } else {
                    batteryInfo.put(keys[i], "0");
                }
            }

        } catch (Exception e) {
            Logger.e(TAG, "Error reading battery status: " + e.getMessage());
            // 设置默认值
            batteryInfo.put("level", "0");
            batteryInfo.put("temperature", "0");
            batteryInfo.put("voltage", "0");
            batteryInfo.put("current_now", "0");
            batteryInfo.put("status", "Unknown");
            batteryInfo.put("health", "Unknown");
        }

        return batteryInfo;
    }

    private double calculateBatteryPower(Map<String, String> batteryInfo) {
        try {
            double voltage = Double.parseDouble(batteryInfo.getOrDefault("voltage", "0")) / 1000000.0; // uV to V
            double current = Double.parseDouble(batteryInfo.getOrDefault("current_now", "0")) / 1000000.0; // uA to A
            return Math.abs(voltage * current); // Watts
        } catch (Exception e) {
            return 0;
        }
    }

    private double getCurrentFPS() {
        try {
            // 尝试通过dumpsys SurfaceFlinger获取FPS
            String output = FileUtils.executeCommand("dumpsys SurfaceFlinger --latency SurfaceView");
            if (output != null && !output.isEmpty()) {
                // 简化的FPS计算
                return 60.0; // 默认返回60fps，实际需要解析dumpsys输出
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not get FPS: " + e.getMessage());
        }
        return 60.0; // 默认返回60fps
    }

    private JsonBuilder getCurrentAppInfo() {
        JsonBuilder appJson = new JsonBuilder();

        try {
            // 通过ActivityManager获取前台应用
            String output = FileUtils.executeCommand("dumpsys activity activities | grep 'mResumedActivity'");
            if (output != null) {
                // 解析前台应用包名
                Pattern pattern = Pattern.compile("\\{[^}]*\\s+([^/\\s]+)/");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    String packageName = matcher.group(1);
                    appJson.put("package_name", packageName);
                    appJson.put("app_name", getAppName(packageName));
                    appJson.put("memory_usage", getAppMemoryUsage(packageName));
                } else {
                    appJson.put("package_name", "unknown");
                    appJson.put("app_name", "Unknown App");
                    appJson.put("memory_usage", 0);
                }
            }

            appJson.put("icon", ""); // 图标数据暂时为空

        } catch (Exception e) {
            Logger.e(TAG, "Error getting current app info: " + e.getMessage());
            appJson.put("package_name", "error");
            appJson.put("app_name", "Error");
            appJson.put("memory_usage", 0);
            appJson.put("icon", "");
        }

        return appJson;
    }

    private String getAppName(String packageName) {
        try {
            String output = FileUtils.executeCommand("pm list packages -f " + packageName);
            if (output != null && !output.isEmpty()) {
                return packageName; // 简化实现，返回包名
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not get app name for " + packageName);
        }
        return packageName;
    }

    private long getAppMemoryUsage(String packageName) {
        try {
            String output = FileUtils.executeCommand("dumpsys meminfo " + packageName);
            if (output != null) {
                // 解析内存使用情况
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("TOTAL")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 1) {
                            return Long.parseLong(parts[1]) * 1024; // KB to bytes
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, "Could not get memory usage for " + packageName);
        }
        return 0;
    }

    private Map<String, String> getBuildProperties() {
        Map<String, String> props = new HashMap<>();
        try {
            String output = FileUtils.executeCommand("getprop");
            if (output != null) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("]:")) {
                        int start = line.indexOf("[") + 1;
                        int end = line.indexOf("]");
                        int valueStart = line.indexOf("[", end + 1) + 1;
                        int valueEnd = line.lastIndexOf("]");

                        if (start > 0 && end > start && valueStart > end && valueEnd > valueStart) {
                            String key = line.substring(start, end);
                            String value = line.substring(valueStart, valueEnd);
                            props.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error getting build properties: " + e.getMessage());
        }
        return props;
    }

    private String getKernelVersion() {
        try {
            String version = FileUtils.readFile("/proc/version");
            if (version != null) {
                return version.trim();
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error getting kernel version: " + e.getMessage());
        }
        return "Unknown";
    }

    private long getSystemUptime() {
        try {
            String uptime = FileUtils.readFile("/proc/uptime");
            if (uptime != null) {
                String[] parts = uptime.trim().split(" ");
                if (parts.length > 0) {
                    return (long) (Double.parseDouble(parts[0]) * 1000); // Convert to milliseconds
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error getting system uptime: " + e.getMessage());
        }
        return 0;
    }
}
