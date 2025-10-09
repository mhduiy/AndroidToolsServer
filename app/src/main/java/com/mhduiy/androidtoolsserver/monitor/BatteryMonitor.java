package com.mhduiy.androidtoolsserver.monitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import com.mhduiy.androidtoolsserver.util.ContextManager;
import com.mhduiy.androidtoolsserver.util.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BatteryMonitor {
    private static final String TAG = "BatteryMonitor";

    public static class BatteryInfo {
        public int level; // 电量百分比 (0-100)
        public int scale; // 电池容量 (通常为100)
        public long capacity; // 电池容量 (单位: 微安时)
        public int voltage; // 电池电压 (单位: 毫伏)
        public int current; // 当前电流 (单位: 毫安)
        public int temperature; // 电池温度 (单位: 摄氏度*10)
        public String technology; // 电池技术类型 (如 Li-ion)
        public int chargeCounter; // 充电计数器
        public int health; // 电池健康状态
        public int status; // 电池状态 (如 充电中, 放电中, 未知等)
        public int plugged; // 电池充电方式 (如 USB, AC, 无等)
        public boolean present; // 电池是否存在
    }

    public BatteryInfo getInfo() {
        BatteryInfo batteryInfo = new BatteryInfo();
        Context context = ContextManager.getContext();
        if (context != null) {
            try {
                batteryInfo = getBatteryInfoFromDumpsys();
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (batteryManager != null) {
                    batteryInfo.current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    batteryInfo.capacity = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    batteryInfo.chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                }
            } catch (Exception e) {
                Logger.w(TAG, "Android API failed: " + e.getMessage());
            }
        }
        return batteryInfo;
    }

    private BatteryInfo getBatteryInfoFromDumpsys() {
        BatteryInfo batteryInfo = new BatteryInfo();

        try {
            Process process = Runtime.getRuntime().exec("dumpsys battery");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("level:")) {
                    batteryInfo.level = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("scale:")) {
                    batteryInfo.scale = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("voltage:")) {
                    batteryInfo.voltage = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("temperature:")) {
                    batteryInfo.temperature = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("technology:")) {
                    batteryInfo.technology = line.split(":")[1].trim();
                } else if (line.startsWith("health:")) {
                    batteryInfo.health = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("status:")) {
                    batteryInfo.status = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("plugged:")) {
                    batteryInfo.plugged = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("present:")) {
                    batteryInfo.present = Boolean.parseBoolean(line.split(":")[1].trim());
                } else if (line.startsWith("Charge counter:")) {
                    try {
                        batteryInfo.chargeCounter = Integer.parseInt(line.split(":")[1].trim());
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }
            reader.close();
            process.waitFor();

            // 尝试从power_supply获取电流信息
            batteryInfo.current = getCurrentFromPowerSupply();

            // 尝试获取电池容量
            if (batteryInfo.capacity == 0) {
                batteryInfo.capacity = getCapacityFromPowerSupply();
            }

            Log.i(TAG, "Battery info from dumpsys - Level: " + batteryInfo.level + "%, Voltage: " + batteryInfo.voltage + "mV");

        } catch (Exception e) {
            Log.e(TAG, "Failed to get battery info from dumpsys: " + e.getMessage());
        }

        return batteryInfo;
    }

    private int getCurrentFromPowerSupply() {
        String[] paths = {
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/bms/current_now",
            "/sys/class/power_supply/usb/current_now"
        };

        for (String path : paths) {
            try {
                Process process = Runtime.getRuntime().exec("cat " + path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                reader.close();
                process.waitFor();

                if (line != null && !line.trim().isEmpty()) {
                    int current = Integer.parseInt(line.trim());
                    if (current != 0) {
                        return current;
                    }
                }
            } catch (Exception e) {
                // 继续尝试下一个路径
            }
        }

        return 0;
    }

    private long getCapacityFromPowerSupply() {
        String[] paths = {
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/bms/charge_full",
            "/sys/class/power_supply/battery/charge_full_design"
        };

        for (String path : paths) {
            try {
                Process process = Runtime.getRuntime().exec("cat " + path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                reader.close();
                process.waitFor();

                if (line != null && !line.trim().isEmpty()) {
                    long capacity = Long.parseLong(line.trim());
                    if (capacity != 0) {
                        return capacity;
                    }
                }
            } catch (Exception e) {
                // 继续尝试下一个路径
            }
        }

        return 0;
    }
}
