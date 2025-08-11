package com.example.mytranslator.data.config

import com.example.mytranslator.BuildConfig

/**
 * 微信登录配置类
 *
 * 🎯 设计目的：
 * 1. 集中管理微信开放平台的配置信息
 * 2. 提供配置验证和状态检查功能
 * 3. 支持开发和生产环境的配置切换
 * 4. 确保配置信息的安全性和正确性
 *
 * 🏗️ 配置策略：
 * - 环境隔离：开发和生产环境使用不同配置
 * - 安全存储：敏感信息通过BuildConfig管理
 * - 验证机制：提供配置有效性检查
 * - 调试支持：开发环境提供详细的调试信息
 *
 * 📱 使用场景：
 * - 微信SDK初始化
 * - 登录授权配置
 * - 分享功能配置
 * - 支付功能配置（未来扩展）
 *
 * 🎓 学习要点：
 * 第三方SDK配置的最佳实践：
 * 1. 配置集中化 - 统一管理所有配置
 * 2. 环境区分 - 开发和生产环境隔离
 * 3. 安全性 - 敏感信息的保护
 * 4. 可维护性 - 便于配置的修改和扩展
 */
object WeChatConfig {

    /**
     * 微信开放平台应用ID
     * 
     * 🔐 安全说明：
     * - 开发环境：使用测试AppID
     * - 生产环境：使用正式AppID
     * - 通过BuildConfig配置，避免硬编码
     */
    val APP_ID: String = BuildConfig.WECHAT_APP_ID.ifEmpty { 
        "wx1234567890abcdef" // 默认测试AppID，请替换为实际的AppID
    }

    /**
     * 微信开放平台应用密钥
     * 
     * ⚠️ 重要提醒：
     * AppSecret应该保存在服务器端，客户端不应该包含
     * 这里仅用于开发测试，生产环境请通过服务器API获取
     */
    val APP_SECRET: String = BuildConfig.WECHAT_APP_SECRET.ifEmpty {
        "1234567890abcdef1234567890abcdef" // 默认测试Secret，生产环境请移除
    }

    /**
     * 微信登录授权作用域
     * 
     * 📋 作用域说明：
     * - snsapi_userinfo：获取用户个人信息
     * - snsapi_base：仅获取用户openid
     */
    const val SCOPE = "snsapi_userinfo"

    /**
     * 微信登录状态标识
     * 用于防止CSRF攻击的随机字符串
     */
    const val STATE = "mytranslator_wechat_login"

    /**
     * 微信API基础URL
     */
    const val API_BASE_URL = "https://api.weixin.qq.com"

    /**
     * 获取Access Token的URL
     */
    const val ACCESS_TOKEN_URL = "$API_BASE_URL/sns/oauth2/access_token"

    /**
     * 刷新Access Token的URL
     */
    const val REFRESH_TOKEN_URL = "$API_BASE_URL/sns/oauth2/refresh_token"

    /**
     * 获取用户信息的URL
     */
    const val USER_INFO_URL = "$API_BASE_URL/sns/userinfo"

    /**
     * 检查Access Token有效性的URL
     */
    const val CHECK_TOKEN_URL = "$API_BASE_URL/sns/auth"

    /**
     * 微信登录超时时间（毫秒）
     */
    const val LOGIN_TIMEOUT = 30000L

    /**
     * Token刷新提前时间（秒）
     * 在Token过期前多长时间开始刷新
     */
    const val TOKEN_REFRESH_ADVANCE_TIME = 300L

    /**
     * 检查微信配置是否完整
     *
     * @return 配置是否有效
     */
    fun isConfigured(): Boolean {
        return APP_ID.isNotBlank() && 
               APP_ID != "wx1234567890abcdef" && 
               APP_SECRET.isNotBlank() && 
               APP_SECRET != "1234567890abcdef1234567890abcdef"
    }

    /**
     * 获取配置信息摘要
     * 用于调试和日志记录
     *
     * @return 配置信息字符串
     */
    fun getConfigInfo(): String {
        return buildString {
            appendLine("微信登录配置信息:")
            appendLine("  APP_ID: ${if (APP_ID.isNotBlank()) "${APP_ID.take(8)}..." else "未配置"}")
            appendLine("  APP_SECRET: ${if (APP_SECRET.isNotBlank()) "已配置" else "未配置"}")
            appendLine("  SCOPE: $SCOPE")
            appendLine("  STATE: $STATE")
            appendLine("  配置状态: ${if (isConfigured()) "✅ 已配置" else "❌ 未配置"}")
            
            if (!isConfigured()) {
                appendLine()
                appendLine("📋 配置步骤:")
                appendLine("1. 访问 https://open.weixin.qq.com/ 注册开发者账号")
                appendLine("2. 创建移动应用获取AppID和AppSecret")
                appendLine("3. 在app/build.gradle.kts中配置:")
                appendLine("   buildConfigField(\"String\", \"WECHAT_APP_ID\", \"\\\"your_app_id\\\"\")")
                appendLine("   buildConfigField(\"String\", \"WECHAT_APP_SECRET\", \"\\\"your_app_secret\\\"\")")
                appendLine("4. 重新编译应用")
            }
        }
    }

    /**
     * 验证AppID格式
     *
     * @param appId 要验证的AppID
     * @return 是否为有效格式
     */
    fun isValidAppId(appId: String): Boolean {
        return appId.matches(Regex("^wx[a-f0-9]{16}$"))
    }

    /**
     * 验证AppSecret格式
     *
     * @param appSecret 要验证的AppSecret
     * @return 是否为有效格式
     */
    fun isValidAppSecret(appSecret: String): Boolean {
        return appSecret.matches(Regex("^[a-f0-9]{32}$"))
    }

    /**
     * 获取微信登录回调的包名
     * 微信SDK要求回调Activity的包名必须与注册时一致
     */
    fun getCallbackPackageName(): String {
        return "com.example.mytranslator.wxapi"
    }

    /**
     * 获取微信登录回调Activity的完整类名
     */
    fun getCallbackActivityName(): String {
        return "${getCallbackPackageName()}.WXEntryActivity"
    }

    /**
     * 配置结果密封类
     */
    sealed class ConfigResult {
        /**
         * 配置成功
         */
        data class Success(val message: String) : ConfigResult()
        
        /**
         * 配置警告
         */
        data class Warning(val message: String, val details: String) : ConfigResult()
        
        /**
         * 配置错误
         */
        data class Error(val message: String) : ConfigResult()
    }

    /**
     * 初始化并验证微信配置
     *
     * @return 配置结果
     */
    fun initialize(): ConfigResult {
        return try {
            when {
                !isConfigured() -> {
                    ConfigResult.Warning(
                        message = "微信登录配置未完成，将禁用微信登录功能",
                        details = getConfigInfo()
                    )
                }
                
                !isValidAppId(APP_ID) -> {
                    ConfigResult.Error("微信AppID格式不正确，请检查配置")
                }
                
                !isValidAppSecret(APP_SECRET) -> {
                    ConfigResult.Warning(
                        message = "微信AppSecret格式可能不正确",
                        details = "请确认AppSecret是32位十六进制字符串"
                    )
                }
                
                else -> {
                    ConfigResult.Success("微信登录配置验证成功")
                }
            }
        } catch (e: Exception) {
            ConfigResult.Error("微信配置初始化失败: ${e.message}")
        }
    }
}
