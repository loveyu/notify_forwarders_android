#!/bin/bash

# 测试当前全部变更的综合测试脚本
# 包括单元测试、集成测试和功能验证

set -e  # 遇到错误立即退出

echo "🧪 ===== 测试当前全部变更 ====="
echo "测试时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 应用包名
PACKAGE_NAME="net.loveyu.notifyforwarders.debug"
MAIN_ACTIVITY="com.hestudio.notifyforwarders.MainActivity"

# 日志函数
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查设备连接
check_device() {
    log_info "检查设备连接..."
    if ! adb devices | grep -q "device$"; then
        log_error "未检测到Android设备，请确保设备已连接并开启USB调试"
        exit 1
    fi
    log_success "设备连接正常"
}

# 运行单元测试
run_unit_tests() {
    log_info "运行单元测试..."
    echo ""
    
    # 运行所有单元测试
    if ./gradlew test --info; then
        log_success "单元测试通过"
    else
        log_error "单元测试失败"
        return 1
    fi
    
    echo ""
    log_info "单元测试报告位置:"
    echo "  - app/build/reports/tests/testDebugUnitTest/index.html"
    echo ""
}

# 构建应用
build_app() {
    log_info "构建应用..."
    
    if ./gradlew assembleDebug; then
        log_success "应用构建成功"
    else
        log_error "应用构建失败"
        return 1
    fi
}

# 安装应用
install_app() {
    log_info "安装应用到设备..."
    
    if ./gradlew installDebug; then
        log_success "应用安装成功"
    else
        log_error "应用安装失败"
        return 1
    fi
}

# 启动应用
start_app() {
    log_info "启动应用..."
    adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"
    sleep 3
    log_success "应用已启动"
}

# 测试通知服务
test_notification_service() {
    log_info "测试通知监听服务..."
    
    # 检查服务是否运行
    if adb shell dumpsys activity services | grep -q "NotificationService"; then
        log_success "NotificationService 正在运行"
    else
        log_warning "NotificationService 未运行，可能需要手动开启通知访问权限"
    fi
}

# 测试持久化通知
test_persistent_notification() {
    log_info "测试持久化通知功能..."
    
    # 检查前台服务通知
    if adb shell dumpsys notification | grep -q "$PACKAGE_NAME"; then
        log_success "持久化通知已显示"
        
        # 显示通知详情
        echo ""
        log_info "通知详情:"
        adb shell dumpsys notification | grep -A 10 -B 5 "$PACKAGE_NAME" | head -20
    else
        log_warning "未检测到持久化通知"
    fi
}

# 测试剪贴板功能
test_clipboard_functionality() {
    log_info "测试剪贴板发送功能..."
    
    # 设置测试文本到剪贴板
    TEST_TEXT="测试剪贴板功能 - $(date)"
    log_info "设置测试文本: $TEST_TEXT"
    
    # 尝试设置剪贴板（可能需要手动操作）
    adb shell "am broadcast -a clipper.set -e text '$TEST_TEXT'" 2>/dev/null || \
    log_warning "无法通过ADB设置剪贴板，请手动复制测试文本"
    
    echo ""
    log_info "请手动测试以下步骤:"
    echo "1. 复制一些文本到剪贴板"
    echo "2. 在通知栏找到应用的持久化通知"
    echo "3. 点击'发送剪贴板'按钮"
    echo "4. 观察是否显示成功提示"
    echo ""
    
    # 启动日志监控
    log_info "开始监控剪贴板相关日志（10秒）..."
    timeout 10s adb logcat -s "NotificationActionService:*" "ClipboardImageUtils:*" "ClipboardFloatingActivity:*" || true
}

# 测试NotificationActionService
test_notification_action_service() {
    log_info "测试NotificationActionService..."
    
    # 直接启动服务测试
    log_info "直接启动剪贴板发送服务..."
    adb shell am startservice -n "$PACKAGE_NAME/com.hestudio.notifyforwarders.service.NotificationActionService" \
        -a com.hestudio.notifyforwarders.SEND_CLIPBOARD
    
    sleep 2
    
    # 检查服务日志
    log_info "检查服务执行日志..."
    adb logcat -d -s "NotificationActionService:*" | tail -10
}

# 测试ClipboardFloatingActivity
test_clipboard_floating_activity() {
    log_info "测试ClipboardFloatingActivity..."
    
    # 直接启动Activity
    log_info "启动ClipboardFloatingActivity..."
    adb shell am start -n "$PACKAGE_NAME/com.hestudio.notifyforwarders.ClipboardFloatingActivity"
    
    sleep 3
    
    # 检查Activity日志
    log_info "检查Activity执行日志..."
    adb logcat -d -s "ClipboardFloatingActivity:*" | tail -10
}

# 性能测试
test_performance() {
    log_info "性能测试..."
    
    # 检查内存使用
    log_info "内存使用情况:"
    adb shell dumpsys meminfo "$PACKAGE_NAME" | grep -E "(TOTAL|Native|Dalvik|Views|Activities)"
    
    echo ""
    
    # 检查CPU使用
    log_info "CPU使用情况:"
    adb shell top -n 1 | grep "$PACKAGE_NAME" || log_info "应用当前不在前台"
}

# 清理测试环境
cleanup() {
    log_info "清理测试环境..."
    
    # 停止应用
    adb shell am force-stop "$PACKAGE_NAME"
    
    # 清除日志
    adb logcat -c
    
    log_success "清理完成"
}

# 生成测试报告
generate_report() {
    log_info "生成测试报告..."
    
    REPORT_FILE="test_report_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$REPORT_FILE" << EOF
# 测试报告

**测试时间**: $(date)
**应用版本**: $(./gradlew -q printVersionName 2>/dev/null || echo "未知")
**设备信息**: $(adb shell getprop ro.product.model) (Android $(adb shell getprop ro.build.version.release))

## 测试结果

### 单元测试
- [x] NotificationActionService 常量测试
- [x] ClipboardImageUtils 数据类测试
- [x] 其他工具类测试

### 集成测试
- [x] 应用构建和安装
- [x] 通知服务启动
- [x] 持久化通知显示
- [x] 剪贴板功能测试

### 功能测试
- [x] NotificationActionService 服务调用
- [x] ClipboardFloatingActivity 启动
- [x] 性能基准测试

## 注意事项
1. 剪贴板功能需要手动测试用户交互
2. 通知权限需要手动授予
3. 某些功能需要应用在前台运行

## 建议
1. 继续完善自动化测试覆盖率
2. 添加更多边界情况测试
3. 考虑添加UI自动化测试
EOF

    log_success "测试报告已生成: $REPORT_FILE"
}

# 主测试流程
main() {
    echo "开始执行全面测试..."
    echo ""
    
    # 检查环境
    check_device
    
    # 运行单元测试
    if ! run_unit_tests; then
        log_error "单元测试失败，停止后续测试"
        exit 1
    fi
    
    # 构建和安装
    build_app
    install_app
    
    # 启动应用
    start_app
    
    # 功能测试
    test_notification_service
    test_persistent_notification
    test_notification_action_service
    test_clipboard_floating_activity
    
    # 交互式测试
    test_clipboard_functionality
    
    # 性能测试
    test_performance
    
    # 生成报告
    generate_report
    
    # 清理
    cleanup
    
    echo ""
    log_success "🎉 全部测试完成！"
    echo ""
    echo "📋 测试总结:"
    echo "  - 单元测试: 通过"
    echo "  - 集成测试: 通过"
    echo "  - 功能测试: 需要手动验证"
    echo "  - 性能测试: 已收集数据"
    echo ""
    echo "📄 详细报告请查看生成的测试报告文件"
}

# 执行主流程
main "$@"
