package com.mhduiy.androidtoolsserver.monitor;


import static com.mhduiy.androidtoolsserver.util.Utils.readAllText;

import com.mhduiy.androidtoolsserver.util.Logger;

import java.io.File;

public class GPUMonitor {
    // GPU相关文件路径
    private static final File GPU_FREQ_FILE = new File("/sys/class/kgsl/kgsl-3d0/gpuclk");
    private static final File GPU_LOAD_FILE = new File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage");
    private static final File GPU_TEMP_FILE = new File("/sys/class/kgsl/kgsl-3d0/temp");
    private static final File GPU_MAX_FREQ_FILE = new File("/sys/class/kgsl/kgsl-3d0/max_gpuclk");
    private static final File GPU_MIN_FREQ_FILE = new File("/sys/class/kgsl/kgsl-3d0/min_gpuclk");

    // Mali GPU路径 (备用)
    private static final File MALI_GPU_FREQ = new File("/sys/devices/platform/mali/clock");
    private static final File MALI_GPU_UTIL = new File("/sys/devices/platform/mali/utilization");

    private static final String TAG = "GPUMonitor";

    /**
     * GPU信息类
     */
    public static class GpuInfo {
        public String name = "Unknown";
        public String vendor = "Unknown";
        public String renderer = "Unknown";
        public String version = "Unknown";
        public int currentFrequency = 0;
        public int maxFrequency = 0;
        public int minFrequency = 0;
        public double usage = 0.0;
        public int temperature = -1;
    }

    public GpuInfo getInfo() {
        GpuInfo gpuInfo = new GpuInfo();

        try {
            // 优先尝试Adreno GPU (Qualcomm)
            if (getAdrenoGpuInfo(gpuInfo)) {
                gpuInfo.vendor = "Qualcomm";
                gpuInfo.name = "Adreno GPU";
            }
            // 尝试Mali GPU (ARM)
            else if (getMaliGpuInfo(gpuInfo)) {
                gpuInfo.vendor = "ARM";
                gpuInfo.name = "Mali GPU";
            }
            // 其他GPU类型的检测
            else {
                getGenericGpuInfo(gpuInfo);
            }

        } catch (Exception e) {
            Logger.e(TAG, "Error getting GPU info: " + e.getMessage(), e);
        }

        return gpuInfo;
    }

    /**
     * 获取Adreno GPU信息
     */
    private boolean getAdrenoGpuInfo(GpuInfo gpuInfo) {
        boolean found = false;

        // 当前频率
        String freqStr = readAllText(GPU_FREQ_FILE);
        if (!freqStr.isEmpty()) {
            try {
                gpuInfo.currentFrequency = Integer.parseInt(freqStr) / 1000000; // Hz转MHz
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse GPU frequency");
            }
        }

        // GPU使用率
        String loadStr = readAllText(GPU_LOAD_FILE);
        if (!loadStr.isEmpty()) {
            try {
                gpuInfo.usage = Double.parseDouble(loadStr);
                found = true;
            } catch (NumberFormatException e) {
                // 尝试解析百分比格式 "XX%"
                if (loadStr.endsWith("%")) {
                    try {
                        gpuInfo.usage = Double.parseDouble(loadStr.substring(0, loadStr.length() - 1));
                        found = true;
                    } catch (NumberFormatException e2) {
                        Logger.w(TAG, "Failed to parse GPU usage");
                    }
                }
            }
        }

        // GPU温度
        String tempStr = readAllText(GPU_TEMP_FILE);
        if (!tempStr.isEmpty()) {
            try {
                gpuInfo.temperature = Integer.parseInt(tempStr);
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse GPU temperature");
            }
        }

        // 最大频率
        String maxFreqStr = readAllText(GPU_MAX_FREQ_FILE);
        if (!maxFreqStr.isEmpty()) {
            try {
                gpuInfo.maxFrequency = Integer.parseInt(maxFreqStr) / 1000000;
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse GPU max frequency");
            }
        }

        // 最小频率
        String minFreqStr = readAllText(GPU_MIN_FREQ_FILE);
        if (!minFreqStr.isEmpty()) {
            try {
                gpuInfo.minFrequency = Integer.parseInt(minFreqStr) / 1000000;
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse GPU min frequency");
            }
        }

        return found;
    }

    /**
     * 获取Mali GPU信息
     */
    private boolean getMaliGpuInfo(GpuInfo gpuInfo) {
        boolean found = false;

        // Mali GPU频率
        String freqStr = readAllText(MALI_GPU_FREQ);
        if (!freqStr.isEmpty()) {
            try {
                gpuInfo.currentFrequency = Integer.parseInt(freqStr) / 1000000;
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse Mali GPU frequency");
            }
        }

        // Mali GPU使用率
        String utilStr = readAllText(MALI_GPU_UTIL);
        if (!utilStr.isEmpty()) {
            try {
                gpuInfo.usage = Double.parseDouble(utilStr);
                found = true;
            } catch (NumberFormatException e) {
                Logger.w(TAG, "Failed to parse Mali GPU utilization");
            }
        }

        return found;
    }

    /**
     * 获取通用GPU信息（其他厂商）
     */
    private void getGenericGpuInfo(GpuInfo gpuInfo) {
        // 尝试从其他可能的路径获取GPU信息
        gpuInfo.vendor = "Unknown";
        gpuInfo.name = "Generic GPU";
    }

}
