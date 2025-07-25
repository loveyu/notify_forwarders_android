# 🚀 Release 构建快速开始

## 第一次设置（只需执行一次）

### 1. 生成 Release Keystore

```bash
./scripts/generate-keystore.sh
```

按提示输入信息，脚本会自动生成：
- `app/release.keystore` - 签名密钥文件
- `keystore.properties` - 本地配置文件

### 2. 配置 GitHub Secrets

#### 2.1 获取 Keystore 的 Base64 编码

```bash
# macOS/Linux
base64 -w 0 app/release.keystore
```

复制输出的 base64 字符串。

#### 2.2 在 GitHub 添加 Secrets

前往：**GitHub 仓库 → Settings → Secrets and variables → Actions**

添加以下 4 个 secrets：

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | 上面生成的 base64 字符串 |
| `KEYSTORE_PASSWORD` | 你设置的 keystore 密码 |
| `KEY_ALIAS` | 你设置的 key 别名（通常是 `release`） |
| `KEY_PASSWORD` | 你设置的 key 密码 |

## 日常发布流程

### 方法一：自动发布（推荐）

```bash
./scripts/release.sh 1.2.0
```

脚本会自动：
1. 更新版本号
2. 提交更改
3. 创建 Git 标签
4. 推送到 GitHub
5. 触发自动构建

### 方法二：手动发布

```bash
# 1. 手动更新 app/build.gradle.kts 中的版本号
# 2. 提交并推送
git add .
git commit -m "Bump version to 1.2.0"
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin main
git push origin v1.2.0
```

## 验证配置

随时运行以下命令检查配置状态：

```bash
./scripts/verify-setup.sh
```

## 本地测试

```bash
# 测试 debug 构建
./gradlew assembleDebug

# 测试 release 构建（需要先生成 keystore）
./gradlew assembleRelease
```

## 查看构建结果

1. **GitHub Actions**: 前往 Actions 页面查看构建状态
2. **Release 页面**: 构建成功后会自动创建 GitHub Release
3. **下载 APK**: 从 Release 页面或 Actions artifacts 下载

## 故障排除

如果遇到问题：

1. 运行 `./scripts/verify-setup.sh` 检查配置
2. 查看 GitHub Actions 日志
3. 确认所有 Secrets 都已正确配置

---

**重要提醒**：
- 🔒 妥善保管 keystore 文件和密码
- 📝 keystore.properties 不会被提交到 Git
- 🔄 每次发布都会自动增加 versionCode
