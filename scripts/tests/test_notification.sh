#!/bin/bash

echo "=== 测试通知合并功能 ==="

echo "1. 检查当前通知状态..."
adb shell dumpsys notification | grep -A 5 -B 2 "net.loveyu.notifyforwarders.debug.*id=" | head -10

echo -e "\n2. 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity

echo -e "\n3. 等待5秒..."
sleep 5

echo -e "\n4. 再次检查通知状态..."
adb shell dumpsys notification | grep -A 5 -B 2 "net.loveyu.notifyforwarders.debug.*id=" | head -10

echo -e "\n5. 检查服务日志..."
adb logcat -s NotificationService:D -v time -t 10 | grep "统一前台服务通知"

echo -e "\n=== 测试完成 ==="
