#!/bin/bash

# 测试构建脚本
# 用于本地测试构建配置是否正确

set -e

echo "🚀 开始测试构建配置..."

# 检查必要文件
echo "📋 检查必要文件..."
if [ ! -f "gradlew" ]; then
    echo "❌ gradlew 文件不存在"
    exit 1
fi

if [ ! -f "app/build.gradle.kts" ]; then
    echo "❌ app/build.gradle.kts 文件不存在"
    exit 1
fi

echo "✅ 必要文件检查通过"

# 赋予执行权限
chmod +x gradlew

# 清理之前的构建
echo "🧹 清理之前的构建..."
./gradlew clean

# 测试Debug构建
echo "🔨 测试Debug构建..."
./gradlew assembleDebug

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK 构建成功"
    ls -la app/build/outputs/apk/debug/
else
    echo "❌ Debug APK 构建失败"
    exit 1
fi

# 测试Release构建（如果有keystore配置）
if [ -f "keystore.properties" ]; then
    echo "🔨 测试Release构建..."
    ./gradlew assembleRelease
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        echo "✅ Release APK 构建成功"
        ls -la app/build/outputs/apk/release/
    else
        echo "❌ Release APK 构建失败"
        exit 1
    fi
else
    echo "⚠️  未找到 keystore.properties，跳过Release构建测试"
    echo "   如需测试Release构建，请参考 BUILD_INSTRUCTIONS.md"
fi

echo "🎉 构建测试完成！"
