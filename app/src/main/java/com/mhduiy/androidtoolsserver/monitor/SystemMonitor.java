package com.mhduiy.androidtoolsserver.monitor;

import com.mhduiy.androidtoolsserver.util.Logger;

import java.util.*;

public class SystemMonitor {
    private final CPUMonitor cpuMonitor = new CPUMonitor();
    private final GPUMonitor gpuMonitor = new GPUMonitor();
    private final MemoryMonitor memoryMonitor = new MemoryMonitor();
    private final FrontendAppMonitor frontendAppMonitor = new FrontendAppMonitor();
    private final BatteryMonitor batteryMonitor = new BatteryMonitor();
    private static final String TAG = "SystemMonitor";

    public SystemMonitor() {
        try {
            Logger.i(TAG, "SystemMonitor initializing with file system access...");
            Logger.i(TAG, "SystemMonitor initialized successfully");
        } catch (Exception e) {
            Logger.e(TAG, "Error initializing SystemMonitor: " + e.getMessage(), e);
            throw new RuntimeException("SystemMonitor initialization failed", e);
        }
    }

    /**
     * 进程信息类（暂时空实现）
     */
    public static class ProcessInfo {
        public String packageName = "Unknown";
        public String processName = "Unknown";
        public int pid = 0;
        public int uid = 0;
        public int importance = 0;
        public long memoryUsage = 0;
        public boolean foreground = false;
    }

    public GPUMonitor.GpuInfo getGpuInfo() {
        return gpuMonitor.getInfo();
    }

    public CPUMonitor.CpuInfo getCpuInfo() {
        return cpuMonitor.getInfo();
    }

    public MemoryMonitor.MemInfo getMemoryInfo() {
        return memoryMonitor.getInfo();
    }

    public FrontendAppMonitor.FrontendAppInfo getFrontendAppInfo() {
        return frontendAppMonitor.getInfo();
    }

    public BatteryMonitor.BatteryInfo getBatteryInfo() {
        return batteryMonitor.getInfo();
    }

    /**
     * 获取系统信息摘要
     */
    public Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            CPUMonitor.CpuInfo cpuInfo = cpuMonitor.getInfo();
            GPUMonitor.GpuInfo gpuInfo = gpuMonitor.getInfo();
            MemoryMonitor.MemInfo memInfo = getMemoryInfo();

            // CPU信息
            Map<String, Object> cpu = new HashMap<>();
            cpu.put("model", cpuInfo.model);
            cpu.put("architecture", cpuInfo.architecture);
            cpu.put("coreCount", cpuInfo.coreCount);
            cpu.put("usage", Math.round(cpuInfo.currentUsage * 100.0) / 100.0);
            cpu.put("coreUsages", cpuInfo.coreUsages);
            cpu.put("frequencies", cpuInfo.frequencies);
            cpu.put("temperature", cpuInfo.temperature);
            cpu.put("maxFrequency", cpuInfo.maxFrequency);
            cpu.put("minFrequency", cpuInfo.minFrequency);
            summary.put("cpu", cpu);

            // GPU信息
            Map<String, Object> gpu = new HashMap<>();
            gpu.put("name", gpuInfo.name);
            gpu.put("vendor", gpuInfo.vendor);
            gpu.put("renderer", gpuInfo.renderer);
            gpu.put("version", gpuInfo.version);
            gpu.put("currentFrequency", gpuInfo.currentFrequency);
            gpu.put("maxFrequency", gpuInfo.maxFrequency);
            gpu.put("minFrequency", gpuInfo.minFrequency);
            gpu.put("usage", Math.round(gpuInfo.usage * 100.0) / 100.0);
            gpu.put("temperature", gpuInfo.temperature);
            summary.put("gpu", gpu);

            // 内存信息
            Map<String, Object> memory = new HashMap<>();
            memory.put("total", memInfo.MemTotal);
            memory.put("available", memInfo.MemAvailable);
            memory.put("swapFree", memInfo.SwapFree);
            memory.put("swapCached", memInfo.SwapCached);
            memory.put("freeRatio", Math.round(memInfo.getMemFreeRatio() * 10000.0) / 100.0);
            memory.put("absFreeRatio", Math.round(memInfo.getMemAbsFreeRatio() * 10000.0) / 100.0);
            summary.put("memory", memory);

            summary.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            Logger.e(TAG, "Error getting system summary: " + e.getMessage(), e);
            summary.put("error", e.getMessage());
        }

        return summary;
    }
}
