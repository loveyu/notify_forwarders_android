# 自动构建说明

本项目配置了GitHub Actions自动构建，可以根据不同的分支和标签自动生成APK文件。

## 构建触发条件

### Debug版本构建
- **触发条件**: 推送到 `dev` 分支或创建Pull Request到 `main` 分支
- **构建类型**: Debug APK
- **文件名格式**: `NotifyForwarders-dev-YYYYMMDD-HHMMSS-debug.apk`
- **特点**: 
  - 包含debug信息
  - 应用ID后缀为 `.debug`
  - 版本名后缀为 `-debug`
  - 未启用代码混淆

### Release版本构建
- **触发条件**: 创建以 `v` 开头的标签（如 `v1.0.3`）
- **构建类型**: Release APK
- **文件名格式**: `NotifyForwarders-v1.0.3-release.apk`
- **特点**:
  - 启用代码混淆和资源压缩
  - 使用发布签名
  - 自动创建GitHub Release

## 配置Release构建签名

要启用Release版本的自动构建，需要在GitHub仓库中配置以下Secrets：

### 1. 生成Keystore文件
```bash
keytool -genkey -v -keystore release.keystore -alias your_key_alias -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 转换Keystore为Base64
```bash
base64 -i release.keystore -o keystore_base64.txt
```

### 3. 在GitHub仓库中添加Secrets
进入仓库设置 → Secrets and variables → Actions，添加以下secrets：

- `KEYSTORE_BASE64`: keystore文件的base64编码内容
- `KEYSTORE_PASSWORD`: keystore密码
- `KEY_ALIAS`: key别名
- `KEY_PASSWORD`: key密码

## 本地构建

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 17
- Android SDK API 35

### Debug版本
```bash
./gradlew assembleDebug
```

### Release版本
1. 复制 `keystore.properties.template` 为 `keystore.properties`
2. 填入实际的keystore信息
3. 运行构建命令：
```bash
./gradlew assembleRelease
```

## 构建产物

- **Debug APK**: `app/build/outputs/apk/debug/`
- **Release APK**: `app/build/outputs/apk/release/`
- **GitHub Artifacts**: 每次构建的APK都会作为Artifacts上传，保留30天
- **GitHub Releases**: Tag构建会自动创建Release并附加APK文件

## 版本管理

- Debug版本使用时间戳作为版本标识
- Release版本使用Git标签作为版本号
- 建议使用语义化版本号，如 `v1.0.3`

## 注意事项

1. `keystore.properties` 文件已被添加到 `.gitignore`，不会被提交到版本控制
2. Release构建需要正确配置签名，否则构建会失败
3. 只有推送到 `dev` 分支或创建标签才会触发构建
4. Pull Request构建不会创建Release，只会生成Artifacts
