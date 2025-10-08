package com.mhduiy.androidtoolsserver.monitor;

import static com.mhduiy.androidtoolsserver.util.Utils.readAllText;

import java.io.File;

public class MemoryMonitor {

    public static class MemInfo {
        public long SwapFree;
        public long MemTotal;
        public long MemAvailable;
        public long SwapCached;

        // 新增字段用于兼容
        public long totalMemory;
        public long availableMemory;
        public long usedMemory;
        public double memoryUsageRatio;
        public long threshold;
        public boolean lowMemory;
        public long totalStorage;
        public long availableStorage;
        public long usedStorage;

        public double getMemFreeRatio() {
            if (MemTotal > 0 && MemAvailable > 0) {
                return (0.0 + MemAvailable + SwapCached) / MemTotal;
            }
            return 0;
        }

        public double getMemAbsFreeRatio() {
            if (MemTotal > 0 && MemAvailable > 0) {
                return (0.0 + MemAvailable) / MemTotal;
            }
            return 0;
        }

        public double getSwapCacheRatio() {
            if (MemTotal > 0 && SwapCached > 0) {
                return (0.0 + SwapCached) / MemTotal;
            }
            return 0;
        }
    }
    public MemoryMonitor.MemInfo getInfo() {
        String[] memInfoRows = readAllText(new File("/proc/meminfo")).split("\n");
        MemInfo memInfo = new MemInfo();
        for (String row : memInfoRows) {
            if (row.startsWith("SwapFree")) {
                memInfo.SwapFree = memInfoRowValue(row);
            } else if (row.startsWith("MemAvailable")) {
                memInfo.MemAvailable = memInfoRowValue(row);
            } else if (row.startsWith("SwapCached")) {
                memInfo.SwapCached = memInfoRowValue(row);
            } else if (row.startsWith("MemTotal")) {
                memInfo.MemTotal = memInfoRowValue(row);
            }
        }

        // 填充新字段用于兼容
        memInfo.totalMemory = memInfo.MemTotal * 1024; // 转换为字节
        memInfo.availableMemory = memInfo.MemAvailable * 1024;
        memInfo.usedMemory = memInfo.totalMemory - memInfo.availableMemory;
        if (memInfo.totalMemory > 0) {
            memInfo.memoryUsageRatio = (double) memInfo.usedMemory / memInfo.totalMemory;
        }

        return memInfo;
    }

    private static long memInfoRowValue(String row) {
        return Integer.parseInt(row.substring(row.indexOf(":") + 1, row.lastIndexOf(" ")).trim());
    }
}
