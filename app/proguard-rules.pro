# ============================================================
# ProGuard / R8 规则 - NotifyForwarders
# ============================================================

# ---------- 调试辅助 ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Android 组件（Manifest 声明，系统通过反射实例化） ----------
-keep class com.hestudio.notifyforwarders.NotifyForwardersApplication { *; }
-keep class com.hestudio.notifyforwarders.MainActivity { *; }
-keep class com.hestudio.notifyforwarders.SettingsActivity { *; }
-keep class com.hestudio.notifyforwarders.ExampleConfigActivity { *; }
-keep class com.hestudio.notifyforwarders.MediaPermissionActivity { *; }
-keep class com.hestudio.notifyforwarders.ClipboardFloatingActivity { *; }
-keep class com.hestudio.notifyforwarders.service.NotificationService { *; }
-keep class com.hestudio.notifyforwarders.service.NotificationActionService { *; }
-keep class com.hestudio.notifyforwarders.service.JobSchedulerService { *; }
-keep class com.hestudio.notifyforwarders.receiver.BootCompletedReceiver { *; }

# ---------- SnakeYAML（内部使用反射） ----------
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**

# ---------- 枚举类（.name 属性用于序列化） ----------
-keep enum com.hestudio.notifyforwarders.util.DedupStrategy { *; }

# ---------- 数据类（字段名参与 JSON 构建 / YAML 序列化） ----------
-keep class com.hestudio.notifyforwarders.service.NotificationData { *; }
-keep class com.hestudio.notifyforwarders.util.AppConfig { *; }
-keep class com.hestudio.notifyforwarders.util.IgnoreFilterRule { *; }
-keep class com.hestudio.notifyforwarders.util.IgnoreFilterConfig { *; }
-keep class com.hestudio.notifyforwarders.util.DedupFilterConfig { *; }
-keep class com.hestudio.notifyforwarders.util.DedupAppConfig { *; }
-keep class com.hestudio.notifyforwarders.util.ApiConfig { *; }
-keep class com.hestudio.notifyforwarders.util.ApiEndpointConfig { *; }
-keep class com.hestudio.notifyforwarders.util.TimeoutConfig { *; }
-keep class com.hestudio.notifyforwarders.util.TimeoutGroupConfig { *; }
-keep class com.hestudio.notifyforwarders.util.IconUrlConfig { *; }
-keep class com.hestudio.notifyforwarders.util.IconUrlCacheConfig { *; }
-keep class com.hestudio.notifyforwarders.util.MirrorConfig { *; }
-keep class com.hestudio.notifyforwarders.util.MirrorEndpointConfig { *; }
-keep class com.hestudio.notifyforwarders.util.MirrorDestination { *; }

# ---------- Kotlin 协程 ----------
-dontwarn kotlinx.coroutines.**
