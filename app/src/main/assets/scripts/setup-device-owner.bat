@echo off
setlocal
chcp 65001 >nul
rem ============================================================
rem  应用黑名单 - 设备所有者一键授权脚本 (Windows)
rem  用法: 双击运行,或命令行执行 setup-device-owner.bat [APK路径]
rem ============================================================
set "PKG=cc.lycstar.admin"
set "COMPONENT=%PKG%/.MyAdmin"

echo ==^> 检查 adb
where adb >nul 2>nul
if errorlevel 1 (
    echo [X] 未找到 adb。
    echo     请下载 Android platform-tools 并加入 PATH 后重试:
    echo     https://developer.android.com/tools/releases/platform-tools
    echo     (也可把本脚本放到 platform-tools 目录内直接运行)
    pause
    exit /b 1
)

echo ==^> 检查设备连接
set "DEVFOUND="
for /f "skip=1 tokens=2" %%i in ('adb devices') do if "%%i"=="device" set "DEVFOUND=1"
if not defined DEVFOUND (
    echo [X] 没有已授权连接的设备。
    echo     1. 手机进入 设置 - 开发者选项 - 打开 USB 调试
    echo     2. 数据线连接电脑,手机弹窗点击「允许 USB 调试」
    echo     3. 重新运行本脚本
    pause
    exit /b 1
)

if not "%~1"=="" (
    echo ==^> 安装 APK: %~1
    adb install -r "%~1"
)

echo ==^> 检查应用是否已安装(包名 %PKG%)
adb shell pm path %PKG% >nul 2>nul
if errorlevel 1 (
    echo [X] 设备上尚未安装本应用,请先安装 APK 再运行本脚本。
    pause
    exit /b 1
)

echo ==^> 检查现有设备所有者
set "CURRENT="
for /f "delims=" %%o in ('adb shell dpm list-owners 2^>nul') do set "CURRENT=%%o"
if not "%CURRENT%"=="" (
    echo %CURRENT% | find /i "%COMPONENT%" >nul
    if not errorlevel 1 (
        echo [OK] 本应用已经是设备所有者,无需重复设置。
        pause
        exit /b 0
    )
    echo [X] 设备已有其他设备所有者: %CURRENT%
    echo     一台设备只能有一个设备所有者,需先移除它或恢复出厂设置。
    pause
    exit /b 1
)

echo ==^> 机型兼容性检查
set "MIUI="
for /f "delims=" %%m in ('adb shell getprop ro.miui.ui.version.name 2^>nul') do set "MIUI=%%m"
if not "%MIUI%"=="" (
    echo 检测到 MIUI (%MIUI%),请先逐项确认(任何一项没做都可能导致失败^):
    echo   1. 开发者选项中已开启「USB 调试(安全设置)」
    echo   2. 已退出小米账号及 设置-账户 中的全部账户
    echo   3. 已删除锁屏密码
    echo   4. 已关闭「应用双开」「手机分身」等多用户功能(设备所有者与其冲突^)
    echo   5. 开发者选项中已关闭「MIUI 优化」(设置成功后可重新打开^)
    echo.
)

echo ==^> 设置设备所有者
adb shell dpm set-device-owner %COMPONENT%
timeout /t 1 >nul

set "NOW="
for /f "delims=" %%o in ('adb shell dpm list-owners 2^>nul') do set "NOW=%%o"
echo %NOW% | find /i "%COMPONENT%" >nul
if errorlevel 1 (
    echo.
    echo [X] 设置失败。常见原因:
    echo     - 设备上存在账户: 到 设置-账户 移除全部账户后重试;
    echo       MIUI 还需删除锁屏密码;
    echo       仍失败则恢复出厂设置,首次开机后立即执行本脚本。
    echo     - 存在多个用户/访客/手机分身/应用双开: 移除后重试。
    echo     - MIUI 设备: 已开启「USB 调试(安全设置)」、已关闭「MIUI 优化」。
    echo     - 其他原因请参考项目 README。
    pause
    exit /b 1
)

echo.
echo [OK] 设置成功!回到手机上的应用,即可开始隐藏应用。
pause
