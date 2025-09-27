@echo off
echo Deploying Android System Info Server...
REM 设置控制台为UTF-8编码，防止中文乱码
chcp 65001

REM 检查ADB是否可用
adb version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: ADB not found! Please install Android SDK Platform Tools.
    pause
    exit /b 1
)

REM 检查设备连接
echo Checking device connection...
adb devices | findstr "device" | findstr /v "devices" >nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: No Android device connected or device not authorized.
    echo Please connect your device and enable USB debugging.
    pause
    exit /b 1
)

REM 检查DEX文件是否存在（无后缀的 androidtools-server）
if not exist "app\build\libs\androidtools-server" (
    echo Error: DEX file not found! Please run build.bat first.
    pause
    exit /b 1
)

REM 推送DEX文件到设备（源文件无后缀，目标文件加 .dex 后缀）
echo Pushing DEX file to device...
adb push "app\build\libs\androidtools-server" "/data/local/tmp/androidtools-server.dex"
if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to push DEX file to device.
    pause
    exit /b 1
)

REM 设置文件权限
echo Setting file permissions...
adb shell chmod 755 "/data/local/tmp/androidtools-server.dex"

REM 启动服务器
set /p PORT="Enter port number (default 18888): "
if "%PORT%"=="" set PORT=18888

REM 设置端口转发
echo Setting up port forwarding...
adb forward tcp:%PORT% tcp:%PORT%
if %ERRORLEVEL% NEQ 0 (
    echo Warning: Port forwarding setup failed, but continuing...
) else (
    echo Port forwarding successful: localhost:%PORT% -> device:%PORT%
)

echo Starting SystemInfoServer on port %PORT%...
echo You can access the API at:
echo   - Device IP: http://device_ip:%PORT%/
echo   - Localhost: http://localhost:%PORT%/
echo Press Ctrl+C to stop the server.
echo.

REM 启动服务器（前台运行以便查看日志）
adb shell "CLASSPATH=/data/local/tmp/androidtools-server.dex app_process / com.mhduiy.androidtoolsserver.SystemInfoServer %PORT%"

REM 服务器停止后清理端口转发
echo.
echo Cleaning up port forwarding...
adb forward --remove tcp:%PORT%
