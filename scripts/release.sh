#!/bin/bash

# 发布脚本
# 用于创建新的release标签并触发自动构建

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查参数
if [ $# -eq 0 ]; then
    echo -e "${RED}错误: 请提供版本号${NC}"
    echo "用法: $0 <version>"
    echo "示例: $0 1.0.3"
    exit 1
fi

VERSION=$1

# 验证版本号格式（简单验证）
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}错误: 版本号格式不正确${NC}"
    echo "版本号应该是 x.y.z 格式，如: 1.0.3"
    exit 1
fi

TAG_NAME="v$VERSION"

echo -e "${BLUE}🚀 准备发布版本 $TAG_NAME${NC}"

# 检查是否在git仓库中
if [ ! -d ".git" ]; then
    echo -e "${RED}错误: 当前目录不是git仓库${NC}"
    exit 1
fi

# 检查工作区是否干净
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${YELLOW}警告: 工作区有未提交的更改${NC}"
    git status --short
    echo ""
    read -p "是否继续? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消"
        exit 1
    fi
fi

# 检查标签是否已存在
if git tag -l | grep -q "^$TAG_NAME$"; then
    echo -e "${RED}错误: 标签 $TAG_NAME 已存在${NC}"
    exit 1
fi

# 更新app/build.gradle.kts中的版本号
echo -e "${BLUE}📝 更新版本号...${NC}"
if [ -f "app/build.gradle.kts" ]; then
    # 提取当前版本代码
    CURRENT_VERSION_CODE=$(grep "versionCode = " app/build.gradle.kts | sed 's/.*versionCode = \([0-9]*\).*/\1/')
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    
    # 更新版本号和版本代码
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
    sed -i.bak "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" app/build.gradle.kts
    
    # 删除备份文件
    rm -f app/build.gradle.kts.bak
    
    echo -e "${GREEN}✅ 版本号已更新: $VERSION (versionCode: $NEW_VERSION_CODE)${NC}"
    
    # 提交版本号更改
    git add app/build.gradle.kts
    git commit -m "Bump version to $VERSION"
else
    echo -e "${YELLOW}警告: 未找到 app/build.gradle.kts 文件${NC}"
fi

# 创建标签
echo -e "${BLUE}🏷️  创建标签 $TAG_NAME...${NC}"
git tag -a "$TAG_NAME" -m "Release $TAG_NAME"

# 推送到远程仓库
echo -e "${BLUE}📤 推送到远程仓库...${NC}"
git push origin main
git push origin "$TAG_NAME"

echo -e "${GREEN}🎉 发布完成!${NC}"
echo -e "${BLUE}📋 接下来的步骤:${NC}"
echo "1. GitHub Actions 将自动构建 Release APK"
echo "2. 构建完成后会自动创建 GitHub Release"
echo "3. 前往 GitHub 查看构建状态: https://github.com/YOUR_USERNAME/notify_forwarders_android/actions"
echo "4. 发布完成后可在 Releases 页面下载 APK"

echo ""
echo -e "${YELLOW}注意: 请确保已在 GitHub 仓库中配置了签名相关的 Secrets${NC}"
