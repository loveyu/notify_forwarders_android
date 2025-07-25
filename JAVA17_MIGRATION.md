# Java 17 迁移指南

本项目已从Java 11升级到Java 17。本文档说明了迁移过程中的变化和注意事项。

## 🔄 已更改的配置

### 1. GitHub Actions工作流 (`.github/workflows/build.yml`)
- JDK版本从11更新到17
- 使用Temurin发行版的JDK 17

### 2. Android Gradle配置 (`app/build.gradle.kts`)
- `sourceCompatibility` 和 `targetCompatibility` 更新为 `JavaVersion.VERSION_17`
- Kotlin `jvmTarget` 更新为 `"17"`

### 3. Gradle属性 (`gradle.properties`)
- 添加了Java 17兼容性的JVM参数
- 包含必要的模块导出和开放配置

### 4. 文档更新
- `README.md` 和 `BUILD_INSTRUCTIONS.md` 中的环境要求已更新

## 🚀 Java 17的优势

### 性能改进
- 更好的垃圾收集器性能
- 改进的JIT编译器
- 更低的内存占用

### 新特性
- **密封类 (Sealed Classes)**: 更好的类型安全
- **模式匹配**: 简化instanceof检查
- **文本块**: 多行字符串支持
- **Records**: 简化数据类定义

### 示例代码

#### 文本块 (Java 14+)
```java
String json = """
    {
        "name": "NotifyForwarders",
        "version": "1.0.3"
    }
    """;
```

#### Records (Java 14+)
```java
public record NotificationData(String title, String content, long timestamp) {}
```

#### 模式匹配 (Java 16+)
```java
if (obj instanceof String str) {
    // 直接使用str变量
    System.out.println(str.toUpperCase());
}
```

## 🔧 本地开发环境设置

### 1. 安装JDK 17
#### Windows
```bash
# 使用Chocolatey
choco install openjdk17

# 或下载安装包
# https://adoptium.net/temurin/releases/
```

#### macOS
```bash
# 使用Homebrew
brew install openjdk@17

# 设置JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v17)' >> ~/.zshrc
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk

# 设置默认Java版本
sudo update-alternatives --config java
```

### 2. 验证安装
```bash
java -version
javac -version
```

应该显示类似以下输出：
```
openjdk version "17.0.x" 2023-xx-xx
OpenJDK Runtime Environment Temurin-17.0.x+x (build 17.0.x+x)
OpenJDK 64-Bit Server VM Temurin-17.0.x+x (build 17.0.x+x, mixed mode, sharing)
```

### 3. Android Studio配置
1. 打开 Android Studio
2. 进入 `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
3. 设置 `Gradle JDK` 为 JDK 17

## 🐛 常见问题

### 1. 构建失败：模块访问错误
如果遇到类似以下错误：
```
Unable to make field private final java.lang.String java.io.File.path accessible
```

解决方案：确保 `gradle.properties` 中包含了必要的JVM参数（已在项目中配置）。

### 2. IDE不识别Java 17语法
确保IDE配置了正确的项目SDK：
- IntelliJ IDEA: `File` → `Project Structure` → `Project` → `Project SDK`
- Android Studio: `File` → `Project Structure` → `SDK Location` → `JDK Location`

### 3. Gradle Daemon问题
如果遇到Gradle相关问题，尝试：
```bash
./gradlew --stop
./gradlew clean build
```

## 📚 兼容性说明

### Android Gradle Plugin
- 当前使用的AGP版本 (8.11.1) 完全支持Java 17
- 最低要求：AGP 7.0+

### Kotlin
- 当前Kotlin版本 (2.0.21) 完全支持Java 17
- 最低要求：Kotlin 1.5+

### 第三方依赖
- 所有当前使用的依赖都与Java 17兼容
- Jetpack Compose完全支持Java 17

## 🔄 回滚到Java 11（如果需要）

如果需要回滚到Java 11，请执行以下步骤：

1. 恢复 `app/build.gradle.kts` 中的Java版本配置
2. 恢复 `.github/workflows/build.yml` 中的JDK版本
3. 简化 `gradle.properties` 中的JVM参数
4. 更新文档中的环境要求

## 📞 支持

如果在迁移过程中遇到问题，请：
1. 检查本文档的常见问题部分
2. 确保本地环境正确配置了JDK 17
3. 在项目仓库中创建Issue并提供详细的错误信息
