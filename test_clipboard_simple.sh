#!/bin/bash

echo "=== 简单测试剪贴板发送功能 ==="

# 启动应用
echo "1. 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity

sleep 2

# 让应用进入后台
echo "2. 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME

sleep 1

# 直接调用剪贴板发送服务
echo "3. 直接调用剪贴板发送服务..."
adb shell am startservice -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.service.NotificationActionService -a com.hestudio.notifyforwarders.SEND_CLIPBOARD

echo "4. 等待处理完成..."
sleep 3

echo "=== 测试完成 ==="
echo "请查看日志以确认是否成功："
echo "adb logcat -s NotificationActionService ClipboardImageUtils"
