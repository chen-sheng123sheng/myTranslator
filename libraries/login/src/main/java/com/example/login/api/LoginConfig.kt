package com.example.login.api

/**
 * 登录配置类
 * 
 * 🎯 设计目的：
 * 1. 集中管理登录模块的所有配置信息
 * 2. 提供类型安全的配置参数
 * 3. 支持灵活的配置组合和扩展
 * 4. 便于不同环境的配置管理
 * 
 * 🏗️ 设计模式：
 * - 建造者模式：提供灵活的配置构建方式
 * - 数据类模式：不可变的配置容器
 * - 默认值模式：提供合理的默认配置
 * 
 * 📱 使用场景：
 * - 应用启动时初始化登录模块
 * - 不同环境（开发、测试、生产）的配置切换
 * - 动态调整登录行为和参数
 * - A/B测试和功能开关
 * 
 * 🎓 学习要点：
 * 1. 建造者模式的实现和使用
 * 2. 配置管理的最佳实践
 * 3. 类型安全的参数设计
 * 4. 默认值和验证机制
 */
data class LoginConfig(
    val weChatConfig: WeChatConfig,
    val networkConfig: NetworkConfig = NetworkConfig(),
    val storageConfig: StorageConfig = StorageConfig(),
    val debugConfig: DebugConfig = DebugConfig()
) {
    
    /**
     * 配置建造者
     * 
     * 🎯 建造者模式的优势：
     * 1. 链式调用，代码更简洁易读
     * 2. 可选参数，只配置需要的部分
     * 3. 参数验证，确保配置的有效性
     * 4. 扩展性好，新增配置不影响现有代码
     * 
     * 使用示例：
     * ```kotlin
     * val config = LoginConfig.Builder()
     *     .weChatAppId("wx1234567890abcdef")
     *     .weChatAppSecret("your_app_secret")
     *     .enableDebugLog(true)
     *     .networkTimeout(30000)
     *     .build()
     * ```
     */
    class Builder {
        
        // 微信配置
        private var weChatAppId: String = ""
        private var weChatAppSecret: String = ""
        private var weChatUniversalLink: String = ""
        
        // 网络配置
        private var baseUrl: String = "https://api.example.com/"
        private var connectTimeout: Long = 15000
        private var readTimeout: Long = 30000
        private var writeTimeout: Long = 30000
        private var retryCount: Int = 3
        
        // 存储配置
        private var encryptStorage: Boolean = true
        private var storageKey: String = "login_user_info"
        
        // 调试配置
        private var enableDebugLog: Boolean = false
        private var enableMockLogin: Boolean = false
        private var logLevel: LogLevel = LogLevel.INFO
        
        /**
         * 设置微信AppID
         * 
         * @param appId 微信开放平台申请的AppID
         * @return Builder实例，支持链式调用
         */
        fun weChatAppId(appId: String) = apply {
            this.weChatAppId = appId
        }
        
        /**
         * 设置微信AppSecret
         * 
         * @param appSecret 微信开放平台申请的AppSecret
         * @return Builder实例，支持链式调用
         */
        fun weChatAppSecret(appSecret: String) = apply {
            this.weChatAppSecret = appSecret
        }
        
        /**
         * 设置微信Universal Link
         * 
         * @param universalLink iOS平台需要的Universal Link
         * @return Builder实例，支持链式调用
         */
        fun weChatUniversalLink(universalLink: String) = apply {
            this.weChatUniversalLink = universalLink
        }
        
        /**
         * 设置API基础URL
         * 
         * @param url API服务器的基础URL
         * @return Builder实例，支持链式调用
         */
        fun baseUrl(url: String) = apply {
            this.baseUrl = url
        }
        
        /**
         * 设置网络连接超时时间
         * 
         * @param timeout 超时时间（毫秒）
         * @return Builder实例，支持链式调用
         */
        fun networkTimeout(timeout: Long) = apply {
            this.connectTimeout = timeout
            this.readTimeout = timeout
            this.writeTimeout = timeout
        }
        
        /**
         * 设置网络重试次数
         * 
         * @param count 重试次数
         * @return Builder实例，支持链式调用
         */
        fun retryCount(count: Int) = apply {
            this.retryCount = count
        }
        
        /**
         * 设置是否加密存储用户信息
         * 
         * @param encrypt 是否加密存储
         * @return Builder实例，支持链式调用
         */
        fun encryptStorage(encrypt: Boolean) = apply {
            this.encryptStorage = encrypt
        }
        
        /**
         * 设置存储键名
         * 
         * @param key 存储键名
         * @return Builder实例，支持链式调用
         */
        fun storageKey(key: String) = apply {
            this.storageKey = key
        }
        
        /**
         * 设置是否启用调试日志
         * 
         * @param enable 是否启用
         * @return Builder实例，支持链式调用
         */
        fun enableDebugLog(enable: Boolean) = apply {
            this.enableDebugLog = enable
        }
        
        /**
         * 设置是否启用Mock登录（用于测试）
         * 
         * @param enable 是否启用
         * @return Builder实例，支持链式调用
         */
        fun enableMockLogin(enable: Boolean) = apply {
            this.enableMockLogin = enable
        }
        
        /**
         * 设置日志级别
         * 
         * @param level 日志级别
         * @return Builder实例，支持链式调用
         */
        fun logLevel(level: LogLevel) = apply {
            this.logLevel = level
        }
        
        /**
         * 构建配置对象
         * 
         * @return 配置对象
         * @throws IllegalArgumentException 如果必要参数缺失或无效
         */
        fun build(): LoginConfig {
            // 验证必要参数
            validateConfig()
            
            return LoginConfig(
                weChatConfig = WeChatConfig(
                    appId = weChatAppId,
                    appSecret = weChatAppSecret,
                    universalLink = weChatUniversalLink
                ),
                networkConfig = NetworkConfig(
                    baseUrl = baseUrl,
                    connectTimeout = connectTimeout,
                    readTimeout = readTimeout,
                    writeTimeout = writeTimeout,
                    retryCount = retryCount
                ),
                storageConfig = StorageConfig(
                    encryptStorage = encryptStorage,
                    storageKey = storageKey
                ),
                debugConfig = DebugConfig(
                    enableDebugLog = enableDebugLog,
                    enableMockLogin = enableMockLogin,
                    logLevel = logLevel
                )
            )
        }
        
        /**
         * 验证配置参数
         */
        private fun validateConfig() {
            if (weChatAppId.isBlank()) {
                throw IllegalArgumentException("WeChat AppID is required")
            }
            
            if (weChatAppSecret.isBlank()) {
                throw IllegalArgumentException("WeChat AppSecret is required")
            }
            
            if (connectTimeout <= 0) {
                throw IllegalArgumentException("Connect timeout must be positive")
            }
            
            if (retryCount < 0) {
                throw IllegalArgumentException("Retry count must be non-negative")
            }
            
            if (storageKey.isBlank()) {
                throw IllegalArgumentException("Storage key cannot be blank")
            }
        }
    }
}

/**
 * 微信配置
 * 
 * @param appId 微信开放平台申请的AppID
 * @param appSecret 微信开放平台申请的AppSecret
 * @param universalLink iOS平台需要的Universal Link
 */
data class WeChatConfig(
    val appId: String,
    val appSecret: String,
    val universalLink: String = ""
)

/**
 * 网络配置
 * 
 * @param baseUrl API服务器基础URL
 * @param connectTimeout 连接超时时间（毫秒）
 * @param readTimeout 读取超时时间（毫秒）
 * @param writeTimeout 写入超时时间（毫秒）
 * @param retryCount 重试次数
 */
data class NetworkConfig(
    val baseUrl: String = "https://api.example.com/",
    val connectTimeout: Long = 15000,
    val readTimeout: Long = 30000,
    val writeTimeout: Long = 30000,
    val retryCount: Int = 3
)

/**
 * 存储配置
 * 
 * @param encryptStorage 是否加密存储用户信息
 * @param storageKey 存储键名
 */
data class StorageConfig(
    val encryptStorage: Boolean = true,
    val storageKey: String = "login_user_info"
)

/**
 * 调试配置
 * 
 * @param enableDebugLog 是否启用调试日志
 * @param enableMockLogin 是否启用Mock登录（用于测试）
 * @param logLevel 日志级别
 */
data class DebugConfig(
    val enableDebugLog: Boolean = false,
    val enableMockLogin: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO
)

/**
 * 日志级别枚举
 */
enum class LogLevel(val level: Int, val tag: String) {
    VERBOSE(0, "VERBOSE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARN(3, "WARN"),
    ERROR(4, "ERROR")
}
