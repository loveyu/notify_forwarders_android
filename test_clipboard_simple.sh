#!/bin/bash

echo "=== 简单测试ClipboardFloatingActivity ==="
echo ""

# 启动应用
echo "1. 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity
sleep 2

# 让应用进入后台
echo "2. 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME
sleep 1

# 复制测试文本到剪贴板
TEST_TEXT="测试ClipboardFloatingActivity - $(date '+%Y-%m-%d %H:%M:%S')"
echo "3. 复制测试文本到剪贴板: $TEST_TEXT"
echo "$TEST_TEXT" | adb shell input text "$TEST_TEXT"
adb shell input keyevent KEYCODE_CTRL_LEFT KEYCODE_A
adb shell input keyevent KEYCODE_CTRL_LEFT KEYCODE_C
sleep 1

echo ""
echo "4. 通过NotificationActionService测试剪贴板发送..."
adb shell am startservice -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.service.NotificationActionService -a com.hestudio.notifyforwarders.SEND_CLIPBOARD

echo ""
echo "5. 等待处理完成..."
sleep 3

echo ""
echo "6. 直接测试ClipboardFloatingActivity..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.ClipboardFloatingActivity

echo ""
echo "7. 等待处理完成..."
sleep 3

echo ""
echo "=== 查看相关日志 ==="
echo "最近的日志:"
adb logcat -d -s "ClipboardFloatingActivity:*" "NotificationActionService:*" "ClipboardImageUtils:*" | tail -20
