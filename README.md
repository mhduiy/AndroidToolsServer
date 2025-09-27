# Android Tools Server

一个基于 Android SDK 开发的系统信息监控服务器，无需安装apk，可直接通过 `app_process` 在 Android 设备上运行，提供 HTTP API 接口获取设备的硬件和系统信息。

## 功能特性

- **CPU 监控**: CPU 使用率、温度、频率信息
- **GPU 监控**: GPU 使用率、频率、温度
- **内存监控**: 内存使用情况和统计信息
- **电池监控**: 电量、温度、电流、电压等信息
- **显示监控**: 实时 FPS 和应用信息
- **系统信息**: Android 版本、设备信息、内核版本等

## 编译

### 环境要求
- Android SDK (API 21+)
- Java 11+
- Gradle

### 编译步骤
1. 确保 Android SDK 已正确安装并配置
2. 运行编译脚本：
   ```cmd
   .\build.bat
   ```

### 编译产物
编译成功后，DEX 文件位于：
```
app\build\libs\androidtools-server
```

## 部署使用

### 部署到设备
1. 连接 Android 设备并启用 USB 调试
2. 运行部署脚本：
   ```cmd
   .\deploy.bat
   ```

   脚本默认转发了端口，可直接在PC端访问
3. 按提示输入端口号（默认 18888）

### 访问 API
部署成功后可通过以下方式访问：
- 本地访问：`http://localhost:18888/`
- 设备访问：`http://设备IP:18888/`

#### 示例1 获取系统基本信息
GET: http://localhost:18888/androidapi
```json
{
    "test": "Android API Access Test",
    "build_info": "{\"product\":\"nabu\",\"display\":\"AQ3A.240801.002\",\"manufacturer\":\"Xiaomi\",\"bootloader\":\"unknown\",\"fingerprint\":\"Xiaomi/nabu/nabu:15/AQ3A.240801.002/OS2.0.4.0.VOYCNXM:user/release-keys\",\"host\":\"pangu-build-component-system-362395-j4tfp-2q5bt-706dx\",\"model\":\"21051182C\",\"id\":\"AQ3A.240801.002\",\"brand\":\"Xiaomi\",\"device\":\"nabu\",\"user\":\"builder\",\"board\":\"nabu\",\"hardware\":\"qcom\"}",
    "build_info_status": "success",
    "timestamp": 1758987792487
}
```

#### 示例2 获取内存信息
GET http://localhost:18888/memory

```json
{
    "total": 5773479936,
    "buffers": 901120,
    "cached": 1039556608,
    "available": 1609687040,
    "used": 4163792896,
    "free": 835227648,
    "usage_percent": 72.11929273430145,
    "timestamp": 1758989335235
}
```

#### 示例3 获取电池信息
GET: http://localhost:18888/battery

```json
{
    "current": 0.0,
    "level": 0,
    "temperature": 0.0,
    "health": "0",
    "power": 0.0,
    "voltage": 0.0,
    "status": "0",
    "timestamp": 1758989413195
}
```

#### 示例4 获取前台应用信息
GET: http://localhost:18888/display

```json
{
    "current_app": {
        "package_name": "I=com.miui.home",
        "icon": "",
        "app_name": "I=com.miui.home",
        "memory_usage": 0
    },
    "current_fps": 60.0,
    "one_percent_low": 48.0,
    "timestamp": 1758989448817
}
```

#### 示例5 获取CPU信息
GET: 
http://localhost:18888/cpu
```json
{
    "cores": {
        "core_1": {
            "usage": -1.0,
            "frequency": 576000
        },
        "core_0": {
            "usage": -1.0,
            "frequency": 576000
        },
        "core_3": {
            "usage": -1.0,
            "frequency": 576000
        },
        "core_2": {
            "usage": -1.0,
            "frequency": 576000
        },
        "core_5": {
            "usage": -1.0,
            "frequency": 710400
        },
        "core_4": {
            "usage": -1.0,
            "frequency": 710400
        },
        "core_7": {
            "usage": -1.0,
            "frequency": 825600
        },
        "core_6": {
            "usage": -1.0,
            "frequency": 710400
        }
    },
    "temperature": 30.1,
    "overall_usage": 2.5907053618470908,
    "core_count": 8,
    "timestamp": 1758989652070
}
```
... 等等，后续整理一个api文档

### 

## 许可证

本项目基于 GPL-3.0 许可证开源。详见 [LICENSE](LICENSE) 文件。

## 鸣谢

- [scrcpy](https://github.com/Genymobile/scrcpy) - 参考了其编译方式和项目架构
- [vtools_en](https://github.com/ramabondanp/vtools_en) - 参考了其获取系统信息的方法
