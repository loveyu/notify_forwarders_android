#!/bin/bash

# ClipboardFloatingActivity 窗口焦点改进测试脚本

echo "=== ClipboardFloatingActivity 窗口焦点改进测试 ==="
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误: 没有检测到连接的Android设备"
    echo "请确保设备已连接并启用USB调试"
    exit 1
fi

echo "✅ 设备连接正常"

# 检查应用是否已安装
PACKAGE_NAME="com.hestudio.notifyforwarders"
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "❌ 错误: 应用未安装"
    echo "请先运行: adb install app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi

echo "✅ 应用已安装"

# 启动应用
echo ""
echo "=== 启动应用 ==="
adb shell am start -n "$PACKAGE_NAME/.MainActivity"
sleep 2

echo ""
echo "=== 测试说明 ==="
echo "1. 请在设备上配置服务器地址"
echo "2. 开启持久化通知功能"
echo "3. 复制一些文本到剪贴板"
echo "4. 点击通知栏的'发送剪贴板'按钮"
echo "5. 观察下面的日志输出"
echo ""
echo "预期看到的日志顺序："
echo "  - ClipboardFloatingActivity onCreate"
echo "  - 等待窗口获得焦点后处理剪贴板"
echo "  - 窗口获得焦点，现在可以安全访问剪贴板"
echo "  - 剪贴板处理相关日志"
echo ""

# 清除之前的日志
adb logcat -c

echo "=== 开始监控日志 ==="
echo "（按 Ctrl+C 停止监控）"
echo ""

# 监控相关日志，重点关注窗口焦点和剪贴板处理
adb logcat -s "ClipboardFloatingActivity:*" | while read line; do
    timestamp=$(date '+%H:%M:%S')
    
    # 高亮重要的日志行
    if echo "$line" | grep -q "onCreate"; then
        echo "[$timestamp] 🟢 $line"
    elif echo "$line" | grep -q "等待窗口获得焦点"; then
        echo "[$timestamp] 🟡 $line"
    elif echo "$line" | grep -q "窗口获得焦点"; then
        echo "[$timestamp] 🔵 $line"
    elif echo "$line" | grep -q "窗口再次获得焦点"; then
        echo "[$timestamp] 🟠 $line"
    elif echo "$line" | grep -q "剪贴板"; then
        echo "[$timestamp] 📋 $line"
    else
        echo "[$timestamp] $line"
    fi
done
