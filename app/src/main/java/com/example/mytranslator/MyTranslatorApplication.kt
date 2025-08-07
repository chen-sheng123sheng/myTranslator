package com.example.mytranslator

import android.app.Application
import android.util.Log
import com.example.mytranslator.data.config.ApiConfig
import com.example.mytranslator.common.utils.ApiTestHelper
import com.example.mytranslator.common.utils.DatabaseTestHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用程序Application类
 *
 * 🎯 设计思想：
 * 1. 全局初始化 - 应用启动时的一次性初始化工作
 * 2. 配置管理 - 集中管理应用级别的配置
 * 3. 依赖注入准备 - 为依赖注入框架做准备
 * 4. 全局状态管理 - 管理应用级别的状态
 *
 * 🔧 技术特性：
 * - API配置初始化和验证
 * - 全局异常处理设置
 * - 日志系统初始化
 * - 性能监控初始化
 *
 * 📱 使用场景：
 * - 应用启动时的初始化工作
 * - 全局配置的设置和验证
 * - 第三方库的初始化
 * - 全局状态的管理
 *
 * 🎓 学习要点：
 * Application类的职责：
 * 1. 一次性初始化 - 应用生命周期内只执行一次
 * 2. 全局配置 - 影响整个应用的配置
 * 3. 资源管理 - 全局资源的创建和管理
 * 4. 状态维护 - 跨Activity/Fragment的状态
 */
class MyTranslatorApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "MyTranslatorApplication"

        /**
         * 全局应用实例
         *
         * 🎯 设计说明：
         * 提供全局访问应用上下文的方式，主要用于：
         * 1. 国际化资源访问
         * 2. 全局配置获取
         * 3. 系统服务访问
         *
         * ⚠️ 注意：
         * 虽然提供了全局访问，但应该谨慎使用，
         * 优先通过依赖注入或参数传递的方式获取Context
         */
        lateinit var instance: MyTranslatorApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化全局实例
        instance = this

        Log.i(TAG, "🚀 MyTranslator应用启动")
        Log.i(TAG, getVersionInfo())

        // 初始化API配置
        initializeApiConfig()

        // 初始化其他组件
        initializeOtherComponents()

        // 在调试模式下运行测试
        if (BuildConfig.DEBUG) {
            runApiTests()
            runDatabaseTests()
        }
    }

    /**
     * 初始化API配置
     *
     * 🎯 设计考虑：
     * - 应用启动时验证API配置
     * - 提供详细的配置状态信息
     * - 对于配置问题给出明确指导
     */
    private fun initializeApiConfig() {
        try {
            val configResult = ApiConfig.initialize()

            when (configResult) {
                is ApiConfig.ConfigResult.Success -> {
                    Log.i(TAG, "✅ API配置初始化成功")
                    Log.d(TAG, configResult.message)
                }

                is ApiConfig.ConfigResult.Warning -> {
                    Log.w(TAG, "⚠️ API配置警告: ${configResult.message}")
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, configResult.details)
                    }
                }

                is ApiConfig.ConfigResult.Error -> {
                    Log.e(TAG, "❌ API配置初始化失败: ${configResult.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ API配置初始化异常", e)
        }
    }

    /**
     * 运行API测试（仅在调试模式下）
     *
     * 🎯 测试目的：
     * - 验证API配置是否正确
     * - 测试网络连接和认证
     * - 提供详细的诊断信息
     * - 帮助开发者快速定位问题
     */
    private fun runApiTests() {
        Log.d(TAG, "🧪 开始运行API测试...")

        // 先显示快速诊断信息
        Log.d(TAG, ApiTestHelper.getQuickDiagnosis())

        // 异步运行完整测试
        applicationScope.launch {
            try {
                val testReport = ApiTestHelper.runFullTest()
                Log.i(TAG, testReport.getSummary())

                if (testReport.isAllTestsPassed()) {
                    Log.i(TAG, "🎉 所有API测试通过，翻译功能可正常使用")
                } else {
                    Log.w(TAG, "⚠️ 部分API测试失败，可能影响翻译功能")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API测试过程中发生异常", e)
            }
        }
    }

    /**
     * 运行数据库测试（仅在调试模式下）
     *
     * 🎯 测试目的：
     * - 验证Room数据库配置是否正确
     * - 测试数据库连接和基本操作
     * - 提供详细的诊断信息
     * - 帮助开发者快速定位问题
     */
    private fun runDatabaseTests() {
        Log.d(TAG, "🏠 开始运行数据库测试...")

        // 先显示快速诊断信息
        Log.d(TAG, DatabaseTestHelper.getQuickDiagnosis(this))

        // 异步运行完整测试
        applicationScope.launch {
            try {
                val testReport = DatabaseTestHelper.runFullTest(this@MyTranslatorApplication)
                Log.i(TAG, testReport.getSummary())

                if (testReport.isAllTestsPassed()) {
                    Log.i(TAG, "🎉 所有数据库测试通过，历史记录功能可正常使用")
                } else {
                    Log.w(TAG, "⚠️ 部分数据库测试失败，可能影响历史记录功能")
                }
            } catch (e: Exception) {
                Log.e(TAG, "数据库测试过程中发生异常", e)
            }
        }
    }

    /**
     * 初始化其他组件
     *
     * 🎯 设计考虑：
     * - 第三方库的初始化
     * - 全局异常处理
     * - 性能监控
     * - 日志系统
     */
    private fun initializeOtherComponents() {
        // 初始化日志系统
        initializeLogging()
        
        // 初始化性能监控
        initializePerformanceMonitoring()
        
        // 初始化全局异常处理
        initializeGlobalExceptionHandler()
    }

    /**
     * 初始化日志系统
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "🔍 调试模式：启用详细日志")
        }
    }

    /**
     * 初始化性能监控
     */
    private fun initializePerformanceMonitoring() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "📊 调试模式：启用性能监控")
        }
    }

    /**
     * 初始化全局异常处理
     */
    private fun initializeGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "💥 未捕获异常 in ${thread.name}: ${exception.message}", exception)

            // 在生产环境中，这里可以上报异常到崩溃分析服务
            // 例如：Firebase Crashlytics, Bugly等
        }
    }

    /**
     * 获取应用版本信息
     */
    private fun getVersionInfo(): String {
        return "MyTranslator v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    /**
     * 检查是否为调试版本
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }

    /**
     * 获取API配置信息
     */
    fun getApiConfigInfo(): String {
        return ApiConfig.BaiduTranslation.getConfigInfo()
    }
}

