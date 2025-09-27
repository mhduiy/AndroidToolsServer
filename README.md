# Android Tools Server

一个基于 Android SDK 开发的系统信息监控服务器，通过 `app_process` 在 Android 设备上运行，提供 HTTP API 接口获取设备的硬件和系统信息。

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

## 部署

### 部署到设备
1. 连接 Android 设备并启用 USB 调试
2. 运行部署脚本：
   ```cmd
   .\deploy.bat
   ```
3. 按提示输入端口号（默认 18888）

### 访问 API
部署成功后可通过以下方式访问：
- 本地访问：`http://localhost:18888/`
- 设备访问：`http://设备IP:18888/`

## 许可证

本项目基于 GPL-3.0 许可证开源。详见 [LICENSE](LICENSE) 文件。

## 鸣谢

- [scrcpy](https://github.com/Genymobile/scrcpy) - 参考了其编译方式和项目架构
- [vtools_en](https://github.com/ramabondanp/vtools_en) - 参考了其获取系统信息的方法
