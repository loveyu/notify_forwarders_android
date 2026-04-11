#!/bin/bash

echo "=== 测试通知栏按钮点击 ==="
echo ""

# 启动应用
echo "1. 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity
sleep 2

# 让应用进入后台
echo "2. 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME
sleep 1

# 手动设置剪贴板内容
TEST_TEXT="测试通知栏按钮 - $(date '+%Y-%m-%d %H:%M:%S')"
echo "3. 设置剪贴板内容: $TEST_TEXT"

# 尝试通过广播设置剪贴板
adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
adb shell "service call clipboard 2 s16 '$TEST_TEXT'" 2>/dev/null || \
echo "  ⚠️  无法通过ADB设置剪贴板，请手动复制文本"

sleep 1

echo ""
echo "4. 查看当前通知..."
adb shell dumpsys notification | grep -A 10 -B 5 "notifyforwarders"

echo ""
echo "=== 现在请手动测试 ==="
echo "1. 下拉通知栏"
echo "2. 找到 'Notify forwarders' 通知"
echo "3. 点击 '发送剪贴板' 按钮"
echo "4. 观察是否启动了ClipboardFloatingActivity"
echo ""
echo "=== 监控日志 ==="
echo "运行以下命令查看实时日志："
echo "adb logcat -s ClipboardFloatingActivity NotificationActionService ClipboardImageUtils"
echo ""
echo "或者查看最近的日志："
adb logcat -d -s "ClipboardFloatingActivity:*" "NotificationActionService:*" "ClipboardImageUtils:*" | tail -10
