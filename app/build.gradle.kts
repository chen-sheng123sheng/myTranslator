plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mytranslator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mytranslator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 添加这行来确保生成BuildConfig
        buildConfigField("Boolean", "DEBUG", "true")
    }

    buildTypes {
        debug {
            // 🎯 学习要点：debug构建类型配置

            /**
             * 为什么需要显式配置debug？
             * 1. 默认存在：Android会自动创建debug构建类型
             * 2. 自定义配置：我们需要添加自定义的BuildConfig字段
             * 3. 开发优化：针对开发环境的特殊配置
             */

            // 应用名称后缀，便于区分开发版和正式版
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"

            // 启用调试功能
            isDebuggable = true

            // 确保DEBUG字段正确设置
            buildConfigField("Boolean", "DEBUG", "true")

            // 🔑 百度翻译API配置
            buildConfigField("String", "BAIDU_APP_ID", "\"20250726002416270\"")
            buildConfigField("String", "BAIDU_SECRET_KEY", "\"y56YShfSW4UVgFmmbliB\"")
            buildConfigField("String", "API_BASE_URL", "\"https://fanyi-api.baidu.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 🎯 学习要点：release构建类型配置

            /**
             * 生产环境的安全考虑：
             * 1. 关闭调试功能
             * 2. 启用代码混淆
             * 3. 移除调试日志
             */

            // 关闭调试
            isDebuggable = false

            // 确保DEBUG字段正确设置
            buildConfigField("Boolean", "DEBUG", "false")

            // 🔑 百度翻译API配置（生产环境）
            buildConfigField("String", "BAIDU_APP_ID", "\"20250726002416270\"")
            buildConfigField("String", "BAIDU_SECRET_KEY", "\"y56YShfSW4UVgFmmbliB\"")
            buildConfigField("String", "API_BASE_URL", "\"https://fanyi-api.baidu.com/\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // 启用ViewBinding
    // 显式启用BuildConfig生成
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // 基础Android库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // textTranslation分支新增依赖
    // 网络请求 - Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // ViewModel和LiveData - MVVM架构
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    // 协程 - 异步处理
    implementation(libs.kotlinx.coroutines.android)

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}