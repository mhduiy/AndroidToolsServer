package com.mhduiy.androidtoolsserver.monitor;

import static com.mhduiy.androidtoolsserver.util.Utils.*;

import com.mhduiy.androidtoolsserver.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CPUMonitor {
    // CPU相关文件路径
    private static final File CPU_INFO_FILE = new File("/proc/cpuinfo");
    private static final File CPU_STAT_FILE = new File("/proc/stat");
    private static final File THERMAL_ZONE_DIR = new File("/sys/class/thermal");
    private static final String TAG = "CPUMonitor";
    // 上次CPU统计信息，用于计算使用率
    private CpuStat lastCpuStat = null;
    private long lastStatTime = 0;

    /**
     * CPU信息类
     */
    public static class CpuInfo {
        public String model = "Unknown";
        public String architecture = "Unknown";
        public int coreCount = 0;
        public double currentUsage = 0.0;  // 总体CPU使用率
        public List<Double> coreUsages = new ArrayList<>();  // 各核心使用率
        public List<Integer> frequencies = new ArrayList<>();  // 各核心频率(MHz)
        public int temperature = -1;  // CPU温度(摄氏度)
        public int maxFrequency = 0;  // 最大频率
        public int minFrequency = 0;  // 最小频率
    }

    /**
     * CPU统计信息类，用于计算使用率
     */
    private static class CpuStat {
        public long user, nice, system, idle, iowait, irq, softirq, steal;
        public List<Long[]> coreStats = new ArrayList<>();

        public long getTotalTime() {
            return user + nice + system + idle + iowait + irq + softirq + steal;
        }

        public long getActiveTime() {
            return user + nice + system + irq + softirq + steal;
        }
    }

    public CpuInfo getInfo() {
        CpuInfo cpuInfo = new CpuInfo();
        try {
            // 读取CPU基本信息
            String cpuInfoContent = readAllText(CPU_INFO_FILE);
            if (!cpuInfoContent.isEmpty()) {
                parseCpuBasicInfo(cpuInfo, cpuInfoContent);
            }

            // 获取CPU使用率
            CpuStat currentStat = getCurrentCpuStat();
            if (currentStat != null && lastCpuStat != null) {
                calculateCpuUsage(cpuInfo, lastCpuStat, currentStat);
                lastCpuStat = currentStat;
                lastStatTime = System.currentTimeMillis();
            }

            // 获取CPU频率信息
            getCpuFrequencies(cpuInfo);

            // 获取CPU温度
            cpuInfo.temperature = getCpuTemperature();

        } catch (Exception e) {
            Logger.e(TAG, "Error getting CPU info: " + e.getMessage(), e);
        }
        return cpuInfo;
    }

    /**
     * 解析CPU基本信息
     */
    private void parseCpuBasicInfo(CpuInfo cpuInfo, String content) {
        String[] lines = content.split("\n");
        Set<String> processors = new HashSet<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("processor")) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    processors.add(parts[1].trim());
                }
            } else if (line.startsWith("model name")) {
                String[] parts = line.split(":", 2);
                if (parts.length > 1) {
                    cpuInfo.model = parts[1].trim();
                }
            } else if (line.startsWith("CPU architecture")) {
                String[] parts = line.split(":", 2);
                if (parts.length > 1) {
                    cpuInfo.architecture = parts[1].trim();
                }
            }
        }

        cpuInfo.coreCount = processors.size();
        if (cpuInfo.coreCount == 0) {
            // 备用方法：通过/sys/devices/system/cpu目录计算核心数
            cpuInfo.coreCount = getCoreCountFromSys();
        }
    }
    /**
     * 获取当前CPU统计信息
     */
    private CpuStat getCurrentCpuStat() {
        String content = readAllText(CPU_STAT_FILE);
        if (content.isEmpty()) return null;

        CpuStat stat = new CpuStat();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.startsWith("cpu ")) {
                // 总体CPU统计
                String[] parts = line.split("\\s+");
                if (parts.length >= 8) {
                    stat.user = Long.parseLong(parts[1]);
                    stat.nice = Long.parseLong(parts[2]);
                    stat.system = Long.parseLong(parts[3]);
                    stat.idle = Long.parseLong(parts[4]);
                    stat.iowait = Long.parseLong(parts[5]);
                    stat.irq = Long.parseLong(parts[6]);
                    stat.softirq = Long.parseLong(parts[7]);
                    if (parts.length > 8) {
                        stat.steal = Long.parseLong(parts[8]);
                    }
                }
            } else if (line.startsWith("cpu") && Character.isDigit(line.charAt(3))) {
                // 各核心统计
                String[] parts = line.split("\\s+");
                if (parts.length >= 8) {
                    Long[] coreData = new Long[8];
                    for (int i = 0; i < Math.min(8, parts.length - 1); i++) {
                        coreData[i] = Long.parseLong(parts[i + 1]);
                    }
                    stat.coreStats.add(coreData);
                }
            }
        }

        return stat;
    }
    /**
     * 计算CPU使用率
     */
    private void calculateCpuUsage(CpuInfo cpuInfo, CpuStat lastStat, CpuStat currentStat) {
        // 计算总体CPU使用率
        long totalDiff = currentStat.getTotalTime() - lastStat.getTotalTime();
        long activeDiff = currentStat.getActiveTime() - lastStat.getActiveTime();

        if (totalDiff > 0) {
            cpuInfo.currentUsage = (double) activeDiff / totalDiff * 100.0;
        }

        // 计算各核心使用率
        cpuInfo.coreUsages.clear();
        int coreCount = Math.min(lastStat.coreStats.size(), currentStat.coreStats.size());
        for (int i = 0; i < coreCount; i++) {
            Long[] lastCore = lastStat.coreStats.get(i);
            Long[] currentCore = currentStat.coreStats.get(i);

            long coreTotal = 0, coreActive = 0;
            for (int j = 0; j < Math.min(lastCore.length, currentCore.length); j++) {
                long diff = currentCore[j] - lastCore[j];
                coreTotal += diff;
                if (j != 3) { // 排除idle时间
                    coreActive += diff;
                }
            }

            double coreUsage = coreTotal > 0 ? (double) coreActive / coreTotal * 100.0 : 0.0;
            cpuInfo.coreUsages.add(coreUsage);
        }
    }
    private void getCpuFrequencies(CpuInfo cpuInfo) {
        cpuInfo.frequencies.clear();

        for (int i = 0; i < cpuInfo.coreCount; i++) {
            File freqFile = new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
            String freqStr = readAllText(freqFile);
            if (!freqStr.isEmpty()) {
                try {
                    int freqKHz = Integer.parseInt(freqStr);
                    cpuInfo.frequencies.add(freqKHz / 1000); // 转换为MHz
                } catch (NumberFormatException e) {
                    cpuInfo.frequencies.add(0);
                }
            } else {
                cpuInfo.frequencies.add(0);
            }
        }

        // 获取最大和最小频率
        File maxFreqFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        File minFreqFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");

        String maxFreqStr = readAllText(maxFreqFile);
        String minFreqStr = readAllText(minFreqFile);

        try {
            if (!maxFreqStr.isEmpty()) {
                cpuInfo.maxFrequency = Integer.parseInt(maxFreqStr) / 1000;
            }
            if (!minFreqStr.isEmpty()) {
                cpuInfo.minFrequency = Integer.parseInt(minFreqStr) / 1000;
            }
        } catch (NumberFormatException e) {
            Logger.w(TAG, "Failed to parse CPU frequency limits");
        }
    }

    /**
     * 获取CPU温度
     */
    private int getCpuTemperature() {
        // 尝试从thermal zone获取温度
        File thermalDir = new File("/sys/class/thermal");
        if (thermalDir.exists()) {
            File[] zones = thermalDir.listFiles();
            if (zones != null) {
                for (File zone : zones) {
                    if (zone.getName().startsWith("thermal_zone")) {
                        File typeFile = new File(zone, "type");
                        File tempFile = new File(zone, "temp");

                        String type = readAllText(typeFile);
                        if (type.toLowerCase().contains("cpu") || type.toLowerCase().contains("tsens")) {
                            String tempStr = readAllText(tempFile);
                            if (!tempStr.isEmpty()) {
                                try {
                                    return Integer.parseInt(tempStr) / 1000; // 转换为摄氏度
                                } catch (NumberFormatException e) {
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }

        return -1; // 无法获取温度
    }
    /**
     * 通过/sys目录获取CPU核心数
     */
    private int getCoreCountFromSys() {
        File cpuDir = new File("/sys/devices/system/cpu");
        if (!cpuDir.exists()) return 1;

        File[] files = cpuDir.listFiles();
        if (files == null) return 1;

        int count = 0;
        Pattern pattern = Pattern.compile("cpu\\d+");
        for (File file : files) {
            if (pattern.matcher(file.getName()).matches()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }
}
