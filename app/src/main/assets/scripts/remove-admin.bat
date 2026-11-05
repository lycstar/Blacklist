@echo off
setlocal
chcp 65001 >nul
rem ============================================================
rem  应用黑名单 - 移除设备管理员脚本 (Windows)
rem  用法: 双击运行
rem ============================================================
set "PKG=cc.lycstar.admin"
set "COMPONENT=%PKG%/.MyAdmin"

where adb >nul 2>nul
if errorlevel 1 (
    echo [X] 未找到 adb,请先安装 platform-tools
    pause
    exit /b 1
)

echo ==^> 移除设备管理员: %COMPONENT%
adb shell dpm remove-active-admin %COMPONENT%

echo.
echo 提示:
echo   - 若本应用是设备所有者,部分系统会拒绝此命令;
echo     该情况下通常需要恢复出厂设置才能完全移除设备所有者。
echo   - 移除后可继续执行: adb uninstall %PKG%
pause
