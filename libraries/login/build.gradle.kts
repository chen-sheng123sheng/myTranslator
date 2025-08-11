/**
 * Login模块构建配置
 *
 * 🎯 模块目的：
 * 1. 提供独立的登录功能模块
 * 2. 封装微信登录SDK和相关依赖
 * 3. 对外提供简洁的登录API
 * 4. 支持多种登录方式（微信应用内、二维码、游客）
 *
 * 🏗️ 模块化优势：
 * - 职责分离：登录功能与主应用解耦
 * - 可复用性：其他项目可以直接引用
 * - 依赖隔离：微信SDK等依赖只在此模块中
 * - 独立测试：可以独立进行单元测试
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.login"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 微信登录配置 - 从gradle.properties或环境变量读取
        buildConfigField("String", "WECHAT_APP_ID", "\"${findProperty("WECHAT_APP_ID") ?: "wx1234567890abcdef"}\"")
        buildConfigField("String", "WECHAT_APP_SECRET", "\"${findProperty("WECHAT_APP_SECRET") ?: "your_app_secret_here"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // Android基础库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity)

    // 协程支持
    implementation(libs.kotlinx.coroutines.android)

    // 微信SDK - 用于微信登录功能
    implementation(libs.wechat.sdk)

    // 二维码生成和扫描 - 用于二维码登录
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)

    // JSON处理 - 用于数据序列化
    implementation(libs.gson)

    // 网络请求 - 用于API调用
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // 本地存储 - 用于用户信息持久化
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}