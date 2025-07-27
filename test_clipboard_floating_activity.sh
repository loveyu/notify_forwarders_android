#!/bin/bash

echo "=== 测试ClipboardFloatingActivity剪贴板发送功能 ==="
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误: 没有检测到连接的Android设备"
    echo "请确保设备已连接并启用USB调试"
    exit 1
fi

echo "✅ 设备连接正常"
echo ""

# 安装应用
echo "📱 安装应用..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -ne 0 ]; then
    echo "❌ 应用安装失败"
    exit 1
fi
echo "✅ 应用安装成功"
echo ""

# 启动应用
echo "🚀 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity
sleep 2

# 让应用进入后台
echo "📱 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME
sleep 1

# 复制测试文本到剪贴板
TEST_TEXT="测试ClipboardFloatingActivity - $(date '+%Y-%m-%d %H:%M:%S')"
echo "📋 复制测试文本到剪贴板: $TEST_TEXT"

# 尝试设置剪贴板内容（需要API 29+）
adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
adb shell "service call clipboard 2 s16 '$TEST_TEXT'" 2>/dev/null || \
echo "  ⚠️  注意: 无法通过ADB直接设置剪贴板，请手动复制文本进行测试"

echo ""

# 等待一下确保剪贴板设置完成
sleep 1

echo "🧪 开始测试ClipboardFloatingActivity..."
echo ""

# 直接启动ClipboardFloatingActivity
echo "1. 直接启动ClipboardFloatingActivity..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.ClipboardFloatingActivity
sleep 2

echo ""
echo "2. 通过NotificationActionService启动ClipboardFloatingActivity..."
adb shell am startservice -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.service.NotificationActionService -a com.hestudio.notifyforwarders.SEND_CLIPBOARD
sleep 3

echo ""
echo "=== 查看相关日志 ==="
echo "监控ClipboardFloatingActivity和相关组件的日志..."
echo "（按Ctrl+C停止日志监控）"
echo ""

# 监控相关日志
adb logcat -s "ClipboardFloatingActivity:*" "NotificationActionService:*" "ClipboardImageUtils:*" "NotificationService:*" | while read line; do
    echo "[$(date '+%H:%M:%S')] $line"
done
