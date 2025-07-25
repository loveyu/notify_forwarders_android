# Android Release 构建配置指南

本文档详细说明如何生成 release.keystore 并在 GitHub Actions 中配置自动化发布构建。

## 📋 目录

1. [生成 Release Keystore](#1-生成-release-keystore)
2. [配置 GitHub Secrets](#2-配置-github-secrets)
3. [本地测试](#3-本地测试)
4. [发布流程](#4-发布流程)
5. [故障排除](#5-故障排除)

## 1. 生成 Release Keystore

### 方法一：使用自动化脚本（推荐）

```bash
# 给脚本执行权限
chmod +x scripts/generate-keystore.sh

# 运行脚本
./scripts/generate-keystore.sh
```

脚本会引导您输入必要信息并自动生成：
- `app/release.keystore` - 签名密钥文件
- `keystore.properties` - 本地配置文件

### 方法二：手动生成

```bash
# 生成 keystore（替换相应的值）
keytool -genkeypair \
    -alias release \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -keystore app/release.keystore \
    -storepass YOUR_STORE_PASSWORD \
    -keypass YOUR_KEY_PASSWORD \
    -dname "CN=Your Name, OU=Your Unit, O=Your Organization, L=Your City, ST=Your State, C=Your Country"
```

然后手动创建 `keystore.properties` 文件：

```properties
STORE_FILE=release.keystore
STORE_PASSWORD=YOUR_STORE_PASSWORD
KEY_ALIAS=release
KEY_PASSWORD=YOUR_KEY_PASSWORD
```

## 2. 配置 GitHub Secrets

### 2.1 转换 Keystore 为 Base64

```bash
# macOS
base64 -i app/release.keystore | pbcopy

# Linux
base64 -w 0 app/release.keystore

# Windows (Git Bash)
base64 -w 0 app/release.keystore
```

### 2.2 在 GitHub 仓库中添加 Secrets

前往 GitHub 仓库 → Settings → Secrets and variables → Actions，添加以下 secrets：

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `KEYSTORE_BASE64` | Keystore 文件的 base64 编码 | `MIIKXgIBAzCCCh...` |
| `KEYSTORE_PASSWORD` | Keystore 密码 | `your_store_password` |
| `KEY_ALIAS` | Key 别名 | `release` |
| `KEY_PASSWORD` | Key 密码 | `your_key_password` |

### 2.3 验证 Secrets 配置

确保所有 secrets 都已正确添加，名称完全匹配（区分大小写）。

## 3. 本地测试

### 3.1 测试 Debug 构建

```bash
./gradlew assembleDebug
```

### 3.2 测试 Release 构建

```bash
# 确保 keystore.properties 文件存在
./gradlew assembleRelease
```

### 3.3 验证签名

```bash
# 检查 APK 签名信息
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

## 4. 发布流程

### 4.1 自动发布（推荐）

使用提供的发布脚本：

```bash
# 给脚本执行权限
chmod +x scripts/release.sh

# 发布新版本（例如 1.2.0）
./scripts/release.sh 1.2.0
```

脚本会自动：
1. 更新版本号
2. 提交更改
3. 创建 Git 标签
4. 推送到远程仓库
5. 触发 GitHub Actions 构建

### 4.2 手动发布

```bash
# 1. 更新版本号（在 app/build.gradle.kts 中）
# 2. 提交更改
git add .
git commit -m "Bump version to 1.2.0"

# 3. 创建标签
git tag -a v1.2.0 -m "Release v1.2.0"

# 4. 推送
git push origin main
git push origin v1.2.0
```

### 4.3 GitHub Actions 构建流程

当推送标签时，GitHub Actions 会：

1. **检出代码**
2. **设置 Java 17 环境**
3. **缓存 Gradle 依赖**
4. **创建 keystore 文件**（从 base64 解码）
5. **生成 keystore.properties**
6. **构建 Release APK**
7. **上传 APK 作为 artifact**
8. **创建 GitHub Release**
9. **清理敏感文件**

## 5. 故障排除

### 5.1 常见问题

**问题：构建失败，提示找不到 keystore**
```
解决：检查 GitHub Secrets 是否正确配置，特别是 KEYSTORE_BASE64
```

**问题：签名验证失败**
```
解决：确保 KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD 正确
```

**问题：版本号更新失败**
```
解决：检查 app/build.gradle.kts 文件格式是否正确
```

### 5.2 调试步骤

1. **检查 GitHub Actions 日志**
   - 前往 Actions 页面查看详细错误信息

2. **本地验证**
   ```bash
   # 验证 keystore 文件
   keytool -list -keystore app/release.keystore
   
   # 验证本地构建
   ./gradlew assembleRelease --info
   ```

3. **验证 Secrets**
   ```bash
   # 测试 base64 解码
   echo "YOUR_BASE64_STRING" | base64 --decode > test.keystore
   keytool -list -keystore test.keystore
   ```

### 5.3 安全注意事项

1. **永远不要提交 keystore 文件到版本控制**
2. **定期备份 keystore 文件**
3. **使用强密码**
4. **限制对 GitHub Secrets 的访问权限**

## 📚 相关文件

- `app/build.gradle.kts` - 构建配置和签名设置
- `.github/workflows/build.yml` - GitHub Actions 工作流
- `scripts/generate-keystore.sh` - Keystore 生成脚本
- `scripts/release.sh` - 自动发布脚本
- `keystore.properties.template` - 配置文件模板

## 🔗 有用链接

- [Android 应用签名文档](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Gradle Android 插件文档](https://developer.android.com/studio/build)
