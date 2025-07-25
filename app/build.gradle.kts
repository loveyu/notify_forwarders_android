plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

android {
    namespace = "com.hestudio.notifyforwarders"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.loveyu.notifyforwarders"
        minSdk = 33
        targetSdk = 35
        versionCode = 4
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 签名配置
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                storeFile = file("${keystoreProperties["STORE_FILE"]}")
                storePassword = keystoreProperties["STORE_PASSWORD"].toString()
                keyAlias = keystoreProperties["KEY_ALIAS"].toString()
                keyPassword = keystoreProperties["KEY_PASSWORD"].toString()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"

            // 为 debug 版本生成带时间戳的版本号
            val dateFormat = SimpleDateFormat("yyMMddHHmm")
            val timestamp = dateFormat.format(Date())
            versionNameSuffix = "-debug-$timestamp"

            isDebuggable = true
            isMinifyEnabled = false

            // 为 debug 版本也添加签名配置
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 只有在签名配置存在时才使用
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}