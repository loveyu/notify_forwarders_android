#!/bin/bash

# 通知栏剪贴板发送功能演示脚本
# 用于演示持久化通知中的剪贴板发送功能

echo "=== 通知栏剪贴板发送功能演示 ==="
echo ""

# 检查ADB连接
if ! command -v adb &> /dev/null; then
    echo "错误: 未找到ADB命令，请确保Android SDK已安装并配置PATH"
    exit 1
fi

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "错误: 未检测到Android设备，请确保设备已连接并启用USB调试"
    exit 1
fi

echo "✓ 检测到Android设备连接"

# 应用包名
PACKAGE_NAME="com.hestudio.notifyforwarders"

# 检查应用是否已安装
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "错误: 应用未安装，请先运行以下命令安装应用："
    echo "  ./gradlew assembleDebug -x lintDebug"
    echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi

echo "✓ 应用已安装"

# 启动应用
echo ""
echo "1. 启动应用..."
adb shell am start -n "$PACKAGE_NAME/.MainActivity"
sleep 2

echo "2. 等待应用完全启动..."
sleep 3

echo ""
echo "=== 功能演示步骤 ==="
echo ""
echo "请按照以下步骤测试通知栏剪贴板发送功能："
echo ""
echo "步骤1: 配置应用"
echo "  - 在应用中授予所需权限（通知权限、通知监听权限）"
echo "  - 进入设置页面配置服务器地址"
echo "  - 开启'显示常驻通知'开关"
echo ""

echo "步骤2: 准备剪贴板内容"
echo "  - 在设备上复制一些文本（例如在浏览器或记事本中）"
echo "  - 确保剪贴板中有内容"
echo ""

echo "步骤3: 测试通知栏剪贴板发送"
echo "  - 在通知栏中找到'Notify forwarders'的持久化通知"
echo "  - 点击通知中的'发送剪贴板'按钮"
echo "  - 观察是否成功发送到服务器"
echo "  - 查看Toast提示信息"
echo ""

echo "步骤4: 测试不同内容类型"
echo "  - 复制不同类型的文本内容"
echo "  - 测试空剪贴板的情况"
echo "  - 观察应用的处理和反馈"
echo ""

# 模拟剪贴板操作（如果可能）
echo "=== 自动测试 ==="
echo ""
echo "尝试模拟剪贴板操作..."

# 向剪贴板写入测试内容
TEST_TEXT="这是一个测试文本 - $(date)"
echo "写入测试文本到剪贴板: $TEST_TEXT"

# 使用ADB设置剪贴板内容（需要API 29+）
if adb shell getprop ro.build.version.sdk | awk '{if($1>=29) exit 0; else exit 1}'; then
    adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
    adb shell "service call clipboard 2 s16 '$TEST_TEXT'" 2>/dev/null || \
    echo "  注意: 无法通过ADB直接设置剪贴板，请手动复制文本进行测试"
else
    echo "  注意: Android版本过低，无法通过ADB设置剪贴板，请手动复制文本进行测试"
fi

echo ""
echo "=== 日志监控 ==="
echo ""
echo "开始监控应用日志，查看剪贴板通知相关信息..."
echo "（按Ctrl+C停止日志监控）"
echo ""

# 监控相关日志
adb logcat -s "NotificationActionService:*" "NotificationService:*" "ClipboardImageUtils:*" "ServerPreferences:*" | while read line; do
    echo "[$(date '+%H:%M:%S')] $line"
done
