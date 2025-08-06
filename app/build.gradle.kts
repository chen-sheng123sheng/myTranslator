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

            // 自定义BuildConfig字段
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

            // 生产环境配置
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}