#!/usr/bin/env bash
# ============================================================
#  应用黑名单 - 移除设备管理员脚本
#  适用: macOS / Linux / Windows(Git Bash)
#  用法: ./remove-admin.sh
# ============================================================
PKG="cc.lycstar.admin"
COMPONENT="$PKG/.MyAdmin"

command -v adb >/dev/null 2>&1 || { echo "❌ 未找到 adb,请先安装 platform-tools"; exit 1; }

echo "==> 移除设备管理员: $COMPONENT"
adb shell dpm remove-active-admin "$COMPONENT"

echo
echo "提示:"
echo "  - 若本应用是设备所有者,部分系统会拒绝此命令;"
echo "    该情况下通常需要恢复出厂设置才能完全移除设备所有者。"
echo "  - 移除后可继续执行: adb uninstall $PKG"
