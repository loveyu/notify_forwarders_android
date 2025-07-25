#!/bin/bash

# 验证 Release 构建配置脚本
# 检查所有必要的文件和配置是否正确

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 验证 Release 构建配置${NC}"
echo ""

# 检查项目结构
echo -e "${BLUE}📁 检查项目结构...${NC}"

# 必需文件列表
REQUIRED_FILES=(
    "app/build.gradle.kts"
    ".github/workflows/build.yml"
    "scripts/release.sh"
    "scripts/generate-keystore.sh"
    "keystore.properties.template"
    ".gitignore"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "  ✅ $file"
    else
        echo -e "  ❌ $file ${RED}(缺失)${NC}"
    fi
done

echo ""

# 检查 .gitignore 配置
echo -e "${BLUE}🔒 检查 .gitignore 配置...${NC}"

GITIGNORE_PATTERNS=(
    "keystore.properties"
    "*.keystore"
    "*.jks"
)

for pattern in "${GITIGNORE_PATTERNS[@]}"; do
    if grep -q "$pattern" .gitignore; then
        echo -e "  ✅ $pattern"
    else
        echo -e "  ❌ $pattern ${RED}(缺失)${NC}"
    fi
done

echo ""

# 检查 keystore 相关文件
echo -e "${BLUE}🔐 检查 keystore 配置...${NC}"

if [ -f "keystore.properties" ]; then
    echo -e "  ✅ keystore.properties 存在"
    
    # 检查必需的配置项
    REQUIRED_PROPS=(
        "STORE_FILE"
        "STORE_PASSWORD"
        "KEY_ALIAS"
        "KEY_PASSWORD"
    )
    
    for prop in "${REQUIRED_PROPS[@]}"; do
        if grep -q "^$prop=" keystore.properties; then
            echo -e "    ✅ $prop"
        else
            echo -e "    ❌ $prop ${RED}(缺失)${NC}"
        fi
    done
    
    # 检查 keystore 文件
    STORE_FILE=$(grep "^STORE_FILE=" keystore.properties | cut -d'=' -f2)
    if [ -f "app/$STORE_FILE" ]; then
        echo -e "  ✅ Keystore 文件存在: app/$STORE_FILE"
    else
        echo -e "  ❌ Keystore 文件不存在: app/$STORE_FILE ${RED}(需要生成)${NC}"
    fi
else
    echo -e "  ❌ keystore.properties ${RED}(不存在)${NC}"
    echo -e "    ${YELLOW}提示: 运行 ./scripts/generate-keystore.sh 生成${NC}"
fi

echo ""

# 检查 build.gradle.kts 签名配置
echo -e "${BLUE}⚙️  检查构建配置...${NC}"

if grep -q "signingConfigs" app/build.gradle.kts; then
    echo -e "  ✅ 签名配置存在"
else
    echo -e "  ❌ 签名配置缺失 ${RED}(需要配置)${NC}"
fi

if grep -q "keystoreProperties" app/build.gradle.kts; then
    echo -e "  ✅ Keystore 属性读取逻辑存在"
else
    echo -e "  ❌ Keystore 属性读取逻辑缺失 ${RED}(需要配置)${NC}"
fi

echo ""

# 检查 GitHub Actions 配置
echo -e "${BLUE}🚀 检查 GitHub Actions 配置...${NC}"

REQUIRED_SECRETS=(
    "KEYSTORE_BASE64"
    "KEYSTORE_PASSWORD"
    "KEY_ALIAS"
    "KEY_PASSWORD"
)

echo -e "  ${YELLOW}需要在 GitHub 仓库中配置的 Secrets:${NC}"
for secret in "${REQUIRED_SECRETS[@]}"; do
    if grep -q "$secret" .github/workflows/build.yml; then
        echo -e "    ✅ $secret (在工作流中使用)"
    else
        echo -e "    ❌ $secret ${RED}(未在工作流中使用)${NC}"
    fi
done

echo ""

# 检查脚本权限
echo -e "${BLUE}🔧 检查脚本权限...${NC}"

SCRIPTS=(
    "scripts/generate-keystore.sh"
    "scripts/release.sh"
    "scripts/verify-setup.sh"
)

for script in "${SCRIPTS[@]}"; do
    if [ -x "$script" ]; then
        echo -e "  ✅ $script (可执行)"
    else
        echo -e "  ❌ $script ${RED}(不可执行)${NC}"
        echo -e "    ${YELLOW}运行: chmod +x $script${NC}"
    fi
done

echo ""

# 总结
echo -e "${BLUE}📋 配置总结${NC}"
echo ""

if [ -f "keystore.properties" ] && [ -f "app/$(grep "^STORE_FILE=" keystore.properties | cut -d'=' -f2)" ]; then
    echo -e "${GREEN}✅ 本地配置完整，可以进行本地 release 构建${NC}"
    echo -e "   运行: ./gradlew assembleRelease"
else
    echo -e "${YELLOW}⚠️  本地配置不完整${NC}"
    echo -e "   运行: ./scripts/generate-keystore.sh"
fi

echo ""
echo -e "${YELLOW}📝 GitHub Actions 配置清单:${NC}"
echo "1. 前往 GitHub 仓库 → Settings → Secrets and variables → Actions"
echo "2. 添加以下 Secrets:"
for secret in "${REQUIRED_SECRETS[@]}"; do
    echo "   - $secret"
done
echo ""
echo -e "${BLUE}🚀 发布流程:${NC}"
echo "1. 确保本地配置完整"
echo "2. 配置 GitHub Secrets"
echo "3. 运行: ./scripts/release.sh <version>"
echo "4. 查看 GitHub Actions 构建状态"
