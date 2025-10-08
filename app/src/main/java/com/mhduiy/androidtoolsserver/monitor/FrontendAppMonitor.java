package com.mhduiy.androidtoolsserver.monitor;

import com.mhduiy.androidtoolsserver.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class FrontendAppMonitor {
    private static final String TAG = "FrontendAppMonitor";
    public static class FrontendAppInfo {
        public String packageName = "Unknown";
        public String appName = "Unknown";
        public String activityName = "Unknown";
        public String version = "Unknown";
        public String versionCode = "0";
        public long memoryUsageMB = 0;
        public double cpuUsage = 0.0;
        public int fps = 0;
        public int pid = 0;
        public int uid = 0;
        public boolean isSystemApp = false;
        public String iconBase64 = "";
        public long installTime = 0;
        public long lastUpdateTime = 0;
        public long timestamp = System.currentTimeMillis();
    }

    public FrontendAppInfo getInfo() {
        FrontendAppInfo appInfo =  new FrontendAppInfo();
        try {
            // 获取前台应用包名
            String foregroundPackage = getForegroundAppPackageName();
            if (foregroundPackage != null && !foregroundPackage.isEmpty()) {
                appInfo.packageName = foregroundPackage;

                // 获取应用详细信息
                getAppDetailsViaReflection(appInfo);

                // 获取应用内存使用情况
                getAppMemoryUsageViaReflection(appInfo);
            }

        } catch (Exception e) {
            Logger.e(TAG, "Error getting current app info: " + e.getMessage(), e);
        }

        return appInfo;
    }


    /**
     * 通过反射获取应用详细信息
     */
    private void getAppDetailsViaReflection(FrontendAppInfo appInfo) {
        try {
            Object packageManager = getSystemService("package");
            if (packageManager != null) {
                Class<?> packageManagerClass = Class.forName("android.content.pm.IPackageManager");

                // 获取PackageInfo
                java.lang.reflect.Method getPackageInfoMethod = packageManagerClass.getMethod(
                        "getPackageInfo", String.class, int.class, int.class);
                Object packageInfo = getPackageInfoMethod.invoke(packageManager, appInfo.packageName, 0, 0); // user 0

                if (packageInfo != null) {
                    Class<?> packageInfoClass = packageInfo.getClass();

                    // 获取版本信息
                    java.lang.reflect.Field versionNameField = packageInfoClass.getField("versionName");
                    appInfo.version = (String) versionNameField.get(packageInfo);

                    java.lang.reflect.Field versionCodeField = packageInfoClass.getField("versionCode");
                    appInfo.versionCode = String.valueOf(versionCodeField.get(packageInfo));

                    // 获取ApplicationInfo
                    java.lang.reflect.Field applicationInfoField = packageInfoClass.getField("applicationInfo");
                    Object applicationInfo = applicationInfoField.get(packageInfo);

                    if (applicationInfo != null) {
                        Class<?> applicationInfoClass = applicationInfo.getClass();

                        // 获取UID
                        java.lang.reflect.Field uidField = applicationInfoClass.getField("uid");
                        appInfo.uid = (Integer) uidField.get(applicationInfo);

                        // 判断是否为系统应用
                        java.lang.reflect.Field flagsField = applicationInfoClass.getField("flags");
                        int flags = (Integer) flagsField.get(applicationInfo);
                        appInfo.isSystemApp = (flags & 1) != 0; // FLAG_SYSTEM = 1

                        // 获取应用标签（应用名称）
                        getAppNameViaReflection(appInfo, applicationInfo);
                    }

                    // 获取安装时间
                    java.lang.reflect.Field firstInstallTimeField = packageInfoClass.getField("firstInstallTime");
                    appInfo.installTime = (Long) firstInstallTimeField.get(packageInfo);

                    java.lang.reflect.Field lastUpdateTimeField = packageInfoClass.getField("lastUpdateTime");
                    appInfo.lastUpdateTime = (Long) lastUpdateTimeField.get(packageInfo);
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get app details via reflection: " + e.getMessage());
        }
    }

    /**
     * 通过反射获取应用名称
     */
    private void getAppNameViaReflection(FrontendAppInfo appInfo, Object applicationInfo) {
        try {
            Object packageManager = getSystemService("package");
            if (packageManager != null && applicationInfo != null) {
                Class<?> packageManagerClass = Class.forName("android.content.pm.IPackageManager");

                // 获取应用标签
                java.lang.reflect.Method getApplicationLabelMethod = packageManagerClass.getMethod(
                        "getApplicationLabel", Class.forName("android.content.pm.ApplicationInfo"));
                Object label = getApplicationLabelMethod.invoke(packageManager, applicationInfo);

                if (label != null) {
                    appInfo.appName = label.toString();
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, "Failed to get app name via reflection: " + e.getMessage());
            // 如果获取失败，使用包名作为应用名
            appInfo.appName = appInfo.packageName;
        }
    }

    /**
     * 通过反射获取应用内存使用情况
     */
    private void getAppMemoryUsageViaReflection(FrontendAppInfo appInfo) {
        try {
            Object activityManager = getSystemService("activity");
            if (activityManager != null) {
                Class<?> activityManagerClass = Class.forName("android.app.IActivityManager");

                // 获取运行进程列表
                java.lang.reflect.Method getRunningAppProcessesMethod = activityManagerClass.getMethod("getRunningAppProcesses");
                @SuppressWarnings("unchecked")
                java.util.List<Object> runningProcesses = (java.util.List<Object>) getRunningAppProcessesMethod.invoke(activityManager);

                if (runningProcesses != null) {
                    for (Object processInfo : runningProcesses) {
                        Class<?> processInfoClass = processInfo.getClass();

                        // 获取进程的包列表
                        java.lang.reflect.Field pkgListField = processInfoClass.getField("pkgList");
                        String[] pkgList = (String[]) pkgListField.get(processInfo);

                        // 检查是否包含目标包名
                        boolean containsPackage = false;
                        if (pkgList != null) {
                            for (String pkg : pkgList) {
                                if (appInfo.packageName.equals(pkg)) {
                                    containsPackage = true;
                                    break;
                                }
                            }
                        }

                        if (containsPackage) {
                            // 获取PID
                            java.lang.reflect.Field pidField = processInfoClass.getField("pid");
                            appInfo.pid = (Integer) pidField.get(processInfo);

                            // 获取内存信息
                            int[] pids = {appInfo.pid};
                            java.lang.reflect.Method getProcessMemoryInfoMethod = activityManagerClass.getMethod(
                                    "getProcessMemoryInfo", int[].class);
                            Object[] memoryInfos = (Object[]) getProcessMemoryInfoMethod.invoke(activityManager, new Object[]{pids});

                            if (memoryInfos != null && memoryInfos.length > 0) {
                                Object memoryInfo = memoryInfos[0];
                                Class<?> memoryInfoClass = memoryInfo.getClass();

                                // 获取PSS内存使用量
                                java.lang.reflect.Method getTotalPssMethod = memoryInfoClass.getMethod("getTotalPss");
                                int totalPss = (Integer) getTotalPssMethod.invoke(memoryInfo);
                                appInfo.memoryUsageMB = totalPss / 1024; // 转换为MB
                            }

                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get app memory usage via reflection: " + e.getMessage());
        }
    }


    /**
     * 通过反射获取前台应用包名
     */
    private String getForegroundAppPackageName() {
        try {
            // 方法1: 通过ActivityManager获取运行任务
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Object activityManager = getSystemService("activity");

            if (activityManager != null) {
                // 获取运行任务列表
                java.lang.reflect.Method getRunningTasksMethod = activityManagerClass.getMethod("getRunningTasks", int.class);
                @SuppressWarnings("unchecked")
                java.util.List<Object> runningTasks = (java.util.List<Object>) getRunningTasksMethod.invoke(activityManager, 1);

                if (runningTasks != null && !runningTasks.isEmpty()) {
                    Object taskInfo = runningTasks.get(0);
                    Class<?> taskInfoClass = taskInfo.getClass();

                    // 获取topActivity字段
                    java.lang.reflect.Field topActivityField = taskInfoClass.getField("topActivity");
                    Object componentName = topActivityField.get(taskInfo);

                    if (componentName != null) {
                        // 获取包名
                        java.lang.reflect.Method getPackageNameMethod = componentName.getClass().getMethod("getPackageName");
                        String packageName = (String) getPackageNameMethod.invoke(componentName);
                        Logger.d(TAG, "Found foreground app via ActivityManager: " + packageName);
                        return packageName;
                    }
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get foreground app via ActivityManager: " + e.getMessage());
        }

        try {
            // 方法2: 通过UsageStatsManager（需要系统权限）
            Object usageStatsManager = getSystemService("usagestats");
            if (usageStatsManager != null) {
                Class<?> usageStatsManagerClass = Class.forName("android.app.usage.UsageStatsManager");
                java.lang.reflect.Method queryUsageStatsMethod = usageStatsManagerClass.getMethod(
                        "queryUsageStats", int.class, long.class, long.class);

                long time = System.currentTimeMillis();
                @SuppressWarnings("unchecked")
                java.util.List<Object> appList = (java.util.List<Object>) queryUsageStatsMethod.invoke(
                        usageStatsManager, 0, time - 1000 * 1000, time); // INTERVAL_DAILY = 0

                if (appList != null && !appList.isEmpty()) {
                    // 按最后使用时间排序
                    Object mostRecentApp = null;
                    long lastTimeUsed = 0;

                    for (Object usageStats : appList) {
                        Class<?> usageStatsClass = usageStats.getClass();
                        java.lang.reflect.Method getLastTimeUsedMethod = usageStatsClass.getMethod("getLastTimeUsed");
                        long timeUsed = (Long) getLastTimeUsedMethod.invoke(usageStats);

                        if (timeUsed > lastTimeUsed) {
                            lastTimeUsed = timeUsed;
                            mostRecentApp = usageStats;
                        }
                    }

                    if (mostRecentApp != null) {
                        java.lang.reflect.Method getPackageNameMethod = mostRecentApp.getClass().getMethod("getPackageName");
                        String packageName = (String) getPackageNameMethod.invoke(mostRecentApp);
                        Logger.d(TAG, "Found foreground app via UsageStatsManager: " + packageName);
                        return packageName;
                    }
                }
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get foreground app via UsageStatsManager: " + e.getMessage());
        }

        return "";
    }

    /**
     * 通过反射获取系统服务
     */
    private Object getSystemService(String serviceName) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
            Object service = getServiceMethod.invoke(null, serviceName);

            if (service != null) {
                // 根据服务类型获取对应的Stub
                String stubClassName = "";
                switch (serviceName) {
                    case "activity":
                        stubClassName = "android.app.IActivityManager$Stub";
                        break;
                    case "package":
                        stubClassName = "android.content.pm.IPackageManager$Stub";
                        break;
                    case "usagestats":
                        stubClassName = "android.app.usage.IUsageStatsManager$Stub";
                        break;
                    default:
                        return service;
                }

                Class<?> stubClass = Class.forName(stubClassName);
                java.lang.reflect.Method asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder.class);
                return asInterfaceMethod.invoke(null, service);
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get system service " + serviceName + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取所有运行中的应用进程信息（通过Android API反射调用）
     */
    public List<SystemMonitor.ProcessInfo> getRunningProcesses() {
        List<SystemMonitor.ProcessInfo> processes = new ArrayList<>();

        try {
            Object activityManager = getSystemService("activity");
            if (activityManager != null) {
                Class<?> activityManagerClass = Class.forName("android.app.IActivityManager");

                // 获取运行进程列表
                java.lang.reflect.Method getRunningAppProcessesMethod = activityManagerClass.getMethod("getRunningAppProcesses");
                @SuppressWarnings("unchecked")
                java.util.List<Object> runningProcesses = (java.util.List<Object>) getRunningAppProcessesMethod.invoke(activityManager);

                if (runningProcesses != null) {
                    for (Object processInfo : runningProcesses) {
                        SystemMonitor.ProcessInfo info = new SystemMonitor.ProcessInfo();
                        Class<?> processInfoClass = processInfo.getClass();

                        // 获取进程信息
                        java.lang.reflect.Field processNameField = processInfoClass.getField("processName");
                        info.processName = (String) processNameField.get(processInfo);

                        java.lang.reflect.Field pidField = processInfoClass.getField("pid");
                        info.pid = (Integer) pidField.get(processInfo);

                        java.lang.reflect.Field uidField = processInfoClass.getField("uid");
                        info.uid = (Integer) uidField.get(processInfo);

                        java.lang.reflect.Field importanceField = processInfoClass.getField("importance");
                        info.importance = (Integer) importanceField.get(processInfo);

                        // 判断是否为前台进程
                        info.foreground = info.importance <= 100; // IMPORTANCE_FOREGROUND = 100

                        // 获取包名（取第一个）
                        java.lang.reflect.Field pkgListField = processInfoClass.getField("pkgList");
                        String[] pkgList = (String[]) pkgListField.get(processInfo);
                        if (pkgList != null && pkgList.length > 0) {
                            info.packageName = pkgList[0];
                        }

                        // 获取内存使用情况
                        getProcessMemoryUsageViaReflection(info, activityManager, activityManagerClass);

                        processes.add(info);
                    }
                }
            }

        } catch (Exception e) {
            Logger.e(TAG, "Error getting running processes via reflection: " + e.getMessage());
        }

        return processes;
    }

    /**
     * 通过反射获取进程内存使用情况
     */
    private void getProcessMemoryUsageViaReflection(SystemMonitor.ProcessInfo processInfo, Object activityManager, Class<?> activityManagerClass) {
        try {
            int[] pids = {processInfo.pid};
            java.lang.reflect.Method getProcessMemoryInfoMethod = activityManagerClass.getMethod(
                    "getProcessMemoryInfo", int[].class);
            Object[] memoryInfos = (Object[]) getProcessMemoryInfoMethod.invoke(activityManager, new Object[]{pids});

            if (memoryInfos != null && memoryInfos.length > 0) {
                Object memoryInfo = memoryInfos[0];
                Class<?> memoryInfoClass = memoryInfo.getClass();

                // 获取PSS内存使用量
                java.lang.reflect.Method getTotalPssMethod = memoryInfoClass.getMethod("getTotalPss");
                int totalPss = (Integer) getTotalPssMethod.invoke(memoryInfo);
                processInfo.memoryUsage = totalPss; // PSS in KB
            }

        } catch (Exception e) {
            Logger.w(TAG, "Failed to get process memory usage for PID " + processInfo.pid + ": " + e.getMessage());
        }
    }

}
