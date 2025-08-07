package com.example.mytranslator.data.config

import com.example.mytranslator.BuildConfig

/**
 * API配置类
 *
 * 🎯 设计思想：
 * 1. 集中管理API配置 - 所有API相关配置都在这里
 * 2. 安全性考虑 - 提供多种配置方式，支持环境变量
 * 3. 开发便利性 - 提供默认配置用于开发和测试
 * 4. 生产环境安全 - 支持从外部配置文件或环境变量读取
 *
 * 🔧 技术特性：
 * - 支持多种翻译服务配置
 * - 环境变量优先级配置
 * - 开发和生产环境分离
 * - 配置验证和错误处理
 *
 * 📱 使用场景：
 * - ViewModelFactory中创建Repository时使用
 * - 网络请求的认证配置
 * - 不同环境的配置切换
 * - API密钥的安全管理
 *
 * 🎓 学习要点：
 * API配置的最佳实践：
 * 1. 敏感信息不硬编码 - 使用环境变量或配置文件
 * 2. 分环境配置 - 开发、测试、生产环境分离
 * 3. 配置验证 - 启动时检查配置的有效性
 * 4. 默认值提供 - 为开发环境提供便利
 */
object ApiConfig {

    /**
     * 百度翻译API配置
     *
     * 🔑 获取方式：
     * 1. 注册百度翻译开放平台：https://fanyi-api.baidu.com/
     * 2. 创建应用获取APP ID和密钥
     * 3. 配置到环境变量或此文件中
     *
     * 🛡️ 安全注意事项：
     * - 生产环境请使用环境变量
     * - 不要将真实密钥提交到版本控制
     * - 定期轮换API密钥
     */
    object BaiduTranslation {
        /**
         * 百度翻译APP ID
         *
         * 🔧 配置方式：
         * 1. BuildConfig（推荐）- 从build.gradle.kts配置
         * 2. 环境变量：BAIDU_TRANSLATE_APP_ID
         * 3. 系统属性：baidu.translate.appId
         */
        val APP_ID: String by lazy {
            // 优先从BuildConfig读取（最安全的方式）
            BuildConfig.BAIDU_APP_ID.takeIf { it.isNotBlank() }
                ?: System.getenv("BAIDU_TRANSLATE_APP_ID")
                ?: System.getProperty("baidu.translate.appId")
                ?: ""
        }

        /**
         * 百度翻译密钥
         *
         * 🔧 配置方式：
         * 1. BuildConfig（推荐）- 从build.gradle.kts配置
         * 2. 环境变量：BAIDU_TRANSLATE_SECRET_KEY
         * 3. 系统属性：baidu.translate.secretKey
         */
        val SECRET_KEY: String by lazy {
            // 优先从BuildConfig读取（最安全的方式）
            BuildConfig.BAIDU_SECRET_KEY.takeIf { it.isNotBlank() }
                ?: System.getenv("BAIDU_TRANSLATE_SECRET_KEY")
                ?: System.getProperty("baidu.translate.secretKey")
                ?: ""
        }

        /**
         * 百度翻译API基础URL
         */
        val BASE_URL: String by lazy {
            BuildConfig.API_BASE_URL
        }

        /**
         * 检查配置是否有效
         */
        fun isConfigured(): Boolean {
            return APP_ID.isNotBlank() && SECRET_KEY.isNotBlank()
        }

        /**
         * 获取配置信息（用于调试）
         */
        fun getConfigInfo(): String {
            return buildString {
                appendLine("百度翻译API配置:")
                appendLine("  APP ID: ${if (APP_ID.isNotBlank()) "${APP_ID.take(8)}..." else "未配置"}")
                appendLine("  SECRET KEY: ${if (SECRET_KEY.isNotBlank()) "已配置" else "未配置"}")
                appendLine("  BASE URL: $BASE_URL")
                appendLine("  配置状态: ${if (isConfigured()) "✅ 已配置" else "❌ 未配置"}")
                appendLine("  配置来源: BuildConfig (build.gradle.kts)")
            }
        }
    }

    /**
     * 其他翻译服务配置（预留）
     */
    object GoogleTranslation {
        // TODO: Google翻译API配置
    }

    object MicrosoftTranslation {
        // TODO: 微软翻译API配置
    }

    /**
     * 网络配置
     */
    object Network {
        /** 连接超时时间（秒） */
        const val CONNECT_TIMEOUT = 30L
        
        /** 读取超时时间（秒） */
        const val READ_TIMEOUT = 30L
        
        /** 写入超时时间（秒） */
        const val WRITE_TIMEOUT = 30L
        
        /** 是否启用日志 */
        val ENABLE_LOGGING: Boolean by lazy {
            BuildConfig.ENABLE_LOGGING
        }
    }

    /**
     * 初始化配置并验证
     *
     * 🎯 设计考虑：
     * - 应用启动时调用，确保配置正确
     * - 提供详细的配置信息用于调试
     * - 对于无效配置给出明确的指导
     */
    fun initialize(): ConfigResult {
        return try {
            val baiduConfigured = BaiduTranslation.isConfigured()
            
            if (baiduConfigured) {
                ConfigResult.Success(BaiduTranslation.getConfigInfo())
            } else {
                ConfigResult.Warning(
                    message = "API配置未完成，将使用模拟数据",
                    details = BaiduTranslation.getConfigInfo() + "\n\n" +
                            "📋 配置步骤：\n" +
                            "1. 访问 https://fanyi-api.baidu.com/ 注册账号\n" +
                            "2. 创建应用获取APP ID和密钥\n" +
                            "3. 设置环境变量或修改ApiConfig.kt中的默认值\n" +
                            "4. 重新启动应用"
                )
            }
        } catch (e: Exception) {
            ConfigResult.Error("配置初始化失败: ${e.message}")
        }
    }

    /**
     * 配置结果
     */
    sealed class ConfigResult {
        data class Success(val message: String) : ConfigResult()
        data class Warning(val message: String, val details: String) : ConfigResult()
        data class Error(val message: String) : ConfigResult()
    }
}
