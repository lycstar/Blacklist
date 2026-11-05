#!/usr/bin/env bash
# ============================================================
#  应用黑名单 - 设备所有者一键授权脚本
#  适用: macOS / Linux / Windows(Git Bash)
#  用法: ./setup-device-owner.sh [可选:APK路径]
#  示例: ./setup-device-owner.sh app-debug.apk
# ============================================================
PKG="cc.lycstar.admin"
COMPONENT="$PKG/.MyAdmin"

say() { echo; echo "==> $*"; }
fail() { echo; echo "❌ $*"; exit 1; }

say "检查 adb"
command -v adb >/dev/null 2>&1 || fail "未找到 adb。
    请下载 Android platform-tools 并加入 PATH 后重试:
    https://developer.android.com/tools/releases/platform-tools
    (也可把本脚本复制到 platform-tools 目录内直接运行)"

say "检查设备连接"
CONNECTED=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l)
[ "$CONNECTED" -ge 1 ] || fail "没有已授权连接的设备。
    1. 手机进入 设置 → 开发者选项 → 打开 USB 调试
    2. 数据线连接电脑,手机弹窗点击「允许 USB 调试」
    3. 重新运行本脚本"

if [ -n "$1" ]; then
    say "安装 APK: $1"
    adb install -r "$1" || fail "APK 安装失败"
fi

say "检查应用是否已安装(包名 $PKG)"
adb shell pm path "$PKG" >/dev/null 2>&1 \
    || fail "设备上尚未安装本应用,请先安装 APK 再运行本脚本。

    提示: 也可以把 APK 路径作为参数传给本脚本自动安装:
    ./setup-device-owner.sh 应用黑名单.apk"

say "检查现有设备所有者"
CURRENT=$(adb shell dpm list-owners 2>/dev/null | tr -d '\r' | head -n 1)
if [ -n "$CURRENT" ]; then
    if [ "$CURRENT" = "$COMPONENT" ]; then
        echo "✅ 本应用已经是设备所有者,无需重复设置。"
        exit 0
    fi
    fail "设备已有其他设备所有者: $CURRENT
    一台设备只能有一个设备所有者,需先移除它或恢复出厂设置。"
fi

say "机型兼容性检查"
MIUI=$(adb shell getprop ro.miui.ui.version.name 2>/dev/null | tr -d '\r')
if [ -n "$MIUI" ]; then
    echo "检测到 MIUI ($MIUI),请先逐项确认(任何一项没做都可能导致失败):"
    echo "  1. 开发者选项中已开启「USB 调试(安全设置)」"
    echo "  2. 已退出小米账号及 设置→账户 中的全部账户"
    echo "  3. 已删除锁屏密码"
    echo "  4. 已关闭「应用双开」「手机分身」等多用户功能(设备所有者与其冲突)"
    echo "  5. 开发者选项中已关闭「MIUI 优化」(设置成功后可重新打开)"
    echo
fi

say "设置设备所有者"
OUT=$(adb shell dpm set-device-owner "$COMPONENT" 2>&1)
echo "$OUT" | tr -d '\r'
sleep 1

NOW=$(adb shell dpm list-owners 2>/dev/null | tr -d '\r' | head -n 1)
if [ "$NOW" = "$COMPONENT" ]; then
    echo
    echo "✅ 设置成功!回到手机上的应用,即可开始隐藏应用。"
    exit 0
fi

echo
case "$OUT" in
    *accounts*)
        echo "❌ 失败原因: 设备上存在账户。
    请到 设置 → 账户 移除全部账户后重试(MIUI 还需删除锁屏密码);
    仍失败则恢复出厂设置,首次开机后(不登录任何账户)立即执行本脚本。"
        ;;
    *users*)
        echo "❌ 失败原因: 设备存在多个用户或访客。
    请移除多余用户(含「手机分身」「应用双开」产生的用户)后重试。"
        ;;
    *)
        echo "❌ 设置失败。
    MIUI 设备请确认: 已开启「USB 调试(安全设置)」、已删除锁屏密码、
    已关闭「MIUI 优化」。其他原因请参考项目 README。"
        ;;
esac
exit 1
