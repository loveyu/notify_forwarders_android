#!/bin/bash

# 生成 Android Release Keystore 脚本
# 用于生成用于发布的签名密钥

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔐 Android Release Keystore 生成工具${NC}"
echo ""

# 检查 keytool 是否可用
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}错误: keytool 命令未找到${NC}"
    echo "请确保已安装 Java JDK 并且 keytool 在 PATH 中"
    exit 1
fi

# 设置默认值
DEFAULT_KEYSTORE_NAME="release.keystore"
DEFAULT_KEY_ALIAS="release"
DEFAULT_VALIDITY="10000"  # 约27年

# 获取用户输入
echo -e "${YELLOW}请输入以下信息（按回车使用默认值）:${NC}"
echo ""

read -p "Keystore 文件名 [$DEFAULT_KEYSTORE_NAME]: " KEYSTORE_NAME
KEYSTORE_NAME=${KEYSTORE_NAME:-$DEFAULT_KEYSTORE_NAME}

read -p "Key 别名 [$DEFAULT_KEY_ALIAS]: " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-$DEFAULT_KEY_ALIAS}

read -s -p "Keystore 密码: " KEYSTORE_PASSWORD
echo ""

read -s -p "Key 密码 (留空则使用与 keystore 相同的密码): " KEY_PASSWORD
echo ""
if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD=$KEYSTORE_PASSWORD
fi

read -p "证书有效期（天数）[$DEFAULT_VALIDITY]: " VALIDITY
VALIDITY=${VALIDITY:-$DEFAULT_VALIDITY}

echo ""
echo -e "${YELLOW}请输入证书信息:${NC}"

read -p "您的姓名 (CN): " CN
read -p "组织单位 (OU): " OU
read -p "组织 (O): " O
read -p "城市 (L): " L
read -p "省份 (ST): " ST
read -p "国家代码 (C, 如: CN): " C

# 构建 DN 字符串
DN="CN=$CN"
[ ! -z "$OU" ] && DN="$DN, OU=$OU"
[ ! -z "$O" ] && DN="$DN, O=$O"
[ ! -z "$L" ] && DN="$DN, L=$L"
[ ! -z "$ST" ] && DN="$DN, ST=$ST"
[ ! -z "$C" ] && DN="$DN, C=$C"

echo ""
echo -e "${BLUE}📋 生成信息确认:${NC}"
echo "Keystore 文件: $KEYSTORE_NAME"
echo "Key 别名: $KEY_ALIAS"
echo "有效期: $VALIDITY 天"
echo "证书信息: $DN"
echo ""

read -p "确认生成 keystore? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 1
fi

# 生成 keystore
echo -e "${BLUE}🔨 正在生成 keystore...${NC}"

keytool -genkeypair \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY" \
    -keystore "$KEYSTORE_NAME" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DN"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Keystore 生成成功: $KEYSTORE_NAME${NC}"
    
    # 生成 keystore.properties 文件
    echo -e "${BLUE}📝 生成 keystore.properties 文件...${NC}"
    cat > keystore.properties << EOF
# Keystore配置文件
# 注意：此文件包含敏感信息，不要提交到版本控制

# Keystore文件路径（相对于app目录）
STORE_FILE=$KEYSTORE_NAME

# Keystore密码
STORE_PASSWORD=$KEYSTORE_PASSWORD

# Key别名
KEY_ALIAS=$KEY_ALIAS

# Key密码
KEY_PASSWORD=$KEY_PASSWORD
EOF
    
    echo -e "${GREEN}✅ keystore.properties 文件已生成${NC}"
    
    # 移动 keystore 到 app 目录
    if [ -f "$KEYSTORE_NAME" ]; then
        mv "$KEYSTORE_NAME" "app/$KEYSTORE_NAME"
        echo -e "${GREEN}✅ Keystore 已移动到 app/ 目录${NC}"
    fi
    
    echo ""
    echo -e "${YELLOW}⚠️  重要提醒:${NC}"
    echo "1. 请妥善保管 keystore 文件和密码"
    echo "2. keystore.properties 文件已添加到 .gitignore，不会被提交"
    echo "3. 如需在 GitHub Actions 中使用，请按照以下步骤配置 Secrets"
    echo ""
    echo -e "${BLUE}📋 GitHub Secrets 配置:${NC}"
    echo "1. 将 keystore 文件转换为 base64:"
    echo "   base64 -i app/$KEYSTORE_NAME | pbcopy  # macOS"
    echo "   base64 -w 0 app/$KEYSTORE_NAME         # Linux"
    echo ""
    echo "2. 在 GitHub 仓库设置中添加以下 Secrets:"
    echo "   KEYSTORE_BASE64: [上面生成的 base64 字符串]"
    echo "   KEYSTORE_PASSWORD: $KEYSTORE_PASSWORD"
    echo "   KEY_ALIAS: $KEY_ALIAS"
    echo "   KEY_PASSWORD: $KEY_PASSWORD"
    
else
    echo -e "${RED}❌ Keystore 生成失败${NC}"
    exit 1
fi
