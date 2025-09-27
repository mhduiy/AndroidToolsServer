@echo off
echo =====================================
echo Building Android System Info Server...
echo =====================================

REM 切换到脚本所在目录（项目根）
cd /d "%~dp0"

REM 设置控制台为UTF-8编码，防止中文乱码
chcp 65001

REM 设置环境变量确保英文输出
set LANG=en_US
set LC_ALL=en_US

echo Cleaning previous build...
call gradlew clean

echo Building DEX file for app_process...
call gradlew generateDexForAppProcess

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo =====================================
echo Build completed successfully!
echo =====================================
echo DEX file location: app\build\libs\androidtools-server
echo You can now run deploy.bat to push to device.
echo =====================================

pause
