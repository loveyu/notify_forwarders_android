#!/bin/bash

echo "=== 手动测试剪贴板发送功能 ==="

# 启动应用
echo "1. 启动应用..."
adb shell am start -n net.loveyu.notifyforwarders.debug/com.hestudio.notifyforwarders.MainActivity

sleep 3

# 让应用进入后台
echo "2. 让应用进入后台..."
adb shell input keyevent KEYCODE_HOME

sleep 1

echo "3. 现在请手动执行以下步骤："
echo "   a) 复制一些文本到剪贴板（比如在任何应用中选择文本并复制）"
echo "   b) 下拉通知栏"
echo "   c) 找到通知1000（应该显示应用名称和两个按钮）"
echo "   d) 点击'Send Clipboard'按钮"
echo "   e) 观察是否有Toast提示或错误信息"
echo ""
echo "4. 查看实时日志："
echo "   运行: adb logcat -s NotificationActionService ClipboardImageUtils AppStateManager"
echo ""
echo "=== 预期行为 ==="
echo "- 点击按钮后，应用应该会短暂进入前台"
echo "- 应该能够成功读取剪贴板内容"
echo "- 如果配置了服务器，应该尝试发送内容"
echo "- 应该显示成功或失败的Toast消息"
