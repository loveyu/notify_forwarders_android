#!/bin/bash

echo "=== 测试通知1000中剪贴板发送功能修复 ==="

# 启动应用
echo "1. 启动应用..."
adb shell am start -n com.hestudio.notifyforwarders/.MainActivity

sleep 2

# 复制一些测试文本到剪贴板
echo "2. 复制测试文本到剪贴板..."
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "测试剪贴板发送功能 - $(date)"

sleep 1

# 让应用进入后台
echo "3. 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME

sleep 1

# 查看当前通知
echo "4. 查看当前通知..."
adb shell dumpsys notification | grep -A 5 -B 5 "notifyforwarders"

echo ""
echo "=== 现在请手动测试 ==="
echo "1. 在通知栏中找到通知1000"
echo "2. 点击'发送剪贴板'按钮"
echo "3. 观察是否成功发送（应该显示成功Toast或错误信息）"
echo ""
echo "=== 查看相关日志 ==="
echo "运行以下命令查看日志："
echo "adb logcat -s NotificationActionService ClipboardImageUtils AppStateManager"
