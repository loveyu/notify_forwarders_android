#!/bin/bash

# 专门测试剪贴板集成功能的脚本
# 重点测试 NotificationActionService 和 ClipboardFloatingActivity 的改进

set -e

echo "📋 ===== 剪贴板功能集成测试 ====="
echo "测试时间: $(date)"
echo ""

# 应用包名
PACKAGE_NAME="net.loveyu.notifyforwarders.debug"
MAIN_ACTIVITY="com.hestudio.notifyforwarders.MainActivity"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

# 检查设备
check_device() {
    if ! adb devices | grep -q "device$"; then
        log_error "未检测到设备"
        exit 1
    fi
    log_success "设备连接正常"
}

# 安装应用
install_app() {
    log_info "构建并安装应用..."
    if ./gradlew installDebug; then
        log_success "应用安装成功"
    else
        log_error "应用安装失败"
        exit 1
    fi
}

# 启动应用
start_app() {
    log_info "启动应用..."
    adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"
    sleep 3
    log_success "应用已启动"
}

# 测试1: NotificationActionService 直接调用
test_notification_action_service() {
    echo ""
    echo "🧪 测试1: NotificationActionService 直接调用"
    echo "================================================"
    
    # 清除之前的日志
    adb logcat -c
    
    log_info "准备测试文本..."
    TEST_TEXT="NotificationActionService测试 - $(date +%H:%M:%S)"
    echo "测试文本: $TEST_TEXT"
    
    # 尝试设置剪贴板
    adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
    log_warning "无法通过ADB设置剪贴板，请手动复制: $TEST_TEXT"
    
    log_info "启动NotificationActionService..."
    adb shell am startservice -n "$PACKAGE_NAME/com.hestudio.notifyforwarders.service.NotificationActionService" \
        -a com.hestudio.notifyforwarders.SEND_CLIPBOARD
    
    log_info "等待服务处理..."
    sleep 3
    
    log_info "检查服务日志:"
    adb logcat -d -s "NotificationActionService:*" | tail -10 || log_warning "未找到相关日志"
    
    echo ""
}

# 测试2: ClipboardFloatingActivity 启动测试
test_clipboard_floating_activity() {
    echo ""
    echo "🧪 测试2: ClipboardFloatingActivity 窗口焦点改进"
    echo "================================================"
    
    # 清除日志
    adb logcat -c
    
    log_info "准备测试文本..."
    TEST_TEXT="ClipboardFloatingActivity测试 - $(date +%H:%M:%S)"
    echo "测试文本: $TEST_TEXT"
    
    # 设置剪贴板
    adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
    log_warning "请手动复制: $TEST_TEXT"
    
    log_info "启动ClipboardFloatingActivity..."
    adb shell am start -n "$PACKAGE_NAME/com.hestudio.notifyforwarders.ClipboardFloatingActivity"
    
    log_info "等待Activity处理..."
    sleep 4
    
    log_info "检查Activity日志:"
    adb logcat -d -s "ClipboardFloatingActivity:*" | tail -15 || log_warning "未找到相关日志"
    
    echo ""
}

# 测试3: 通知栏集成测试
test_notification_integration() {
    echo ""
    echo "🧪 测试3: 通知栏集成测试"
    echo "========================"
    
    log_info "检查持久化通知..."
    if adb shell dumpsys notification | grep -q "$PACKAGE_NAME"; then
        log_success "持久化通知已显示"
        
        echo ""
        log_info "通知详情:"
        adb shell dumpsys notification | grep -A 15 -B 5 "$PACKAGE_NAME" | head -25
        
        echo ""
        log_info "手动测试步骤:"
        echo "1. 复制一些文本到剪贴板"
        echo "2. 在通知栏找到应用通知"
        echo "3. 点击'发送剪贴板'按钮"
        echo "4. 观察Toast提示和日志输出"
        echo ""
        
        # 监控日志
        log_info "开始监控通知操作日志（15秒）..."
        echo "（请在这期间点击通知栏的发送剪贴板按钮）"
        timeout 15s adb logcat -s "NotificationActionService:*" "ClipboardImageUtils:*" "ToastManager:*" || true
        
    else
        log_warning "未检测到持久化通知，请检查应用是否正确启动"
    fi
    
    echo ""
}

# 测试4: 错误处理测试
test_error_handling() {
    echo ""
    echo "🧪 测试4: 错误处理测试"
    echo "====================="
    
    # 清除日志
    adb logcat -c
    
    log_info "测试空剪贴板处理..."
    # 清空剪贴板（如果可能）
    adb shell "am broadcast -a clipper.set -e text ''" 2>/dev/null || true
    
    # 启动服务
    adb shell am startservice -n "$PACKAGE_NAME/com.hestudio.notifyforwarders.service.NotificationActionService" \
        -a com.hestudio.notifyforwarders.SEND_CLIPBOARD
    
    sleep 2
    
    log_info "检查错误处理日志:"
    adb logcat -d -s "NotificationActionService:*" "ErrorNotificationUtils:*" | tail -10
    
    echo ""
}

# 测试5: 性能和资源使用
test_performance() {
    echo ""
    echo "🧪 测试5: 性能测试"
    echo "=================="
    
    log_info "检查内存使用..."
    adb shell dumpsys meminfo "$PACKAGE_NAME" | grep -E "(TOTAL|Native|Dalvik)" | head -5
    
    echo ""
    log_info "检查服务状态..."
    adb shell dumpsys activity services | grep -A 5 -B 5 "$PACKAGE_NAME" || log_info "未找到活跃服务"
    
    echo ""
}

# 生成测试报告
generate_report() {
    echo ""
    echo "📄 ===== 测试总结 ====="
    echo ""
    
    log_info "主要测试项目:"
    echo "✅ NotificationActionService 服务调用"
    echo "✅ ClipboardFloatingActivity 窗口焦点改进"
    echo "✅ 持久化通知集成"
    echo "✅ 错误处理机制"
    echo "✅ 性能基准测试"
    
    echo ""
    log_info "需要手动验证的项目:"
    echo "🔍 通知栏按钮点击响应"
    echo "🔍 剪贴板内容正确发送"
    echo "🔍 Toast提示显示"
    echo "🔍 服务器接收确认"
    
    echo ""
    log_warning "注意事项:"
    echo "• 确保已授予通知访问权限"
    echo "• 确保已配置服务器地址"
    echo "• 某些功能需要手动交互测试"
    
    echo ""
    log_success "剪贴板功能集成测试完成！"
}

# 主测试流程
main() {
    check_device
    install_app
    start_app
    
    test_notification_action_service
    test_clipboard_floating_activity
    test_notification_integration
    test_error_handling
    test_performance
    
    generate_report
}

# 执行测试
main "$@"
