package com.example.login.api

import android.content.Context
import com.example.login.internal.storage.UserStorage
import com.example.login.internal.wechat.WeChatLoginService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 登录管理器 - Login模块的主要API入口
 * 
 * 🎯 设计目的：
 * 1. 提供统一的登录接口，隐藏内部实现复杂性
 * 2. 支持多种登录方式的策略切换（微信应用内、二维码、游客）
 * 3. 管理全局登录状态和用户信息
 * 4. 处理登录生命周期和错误恢复
 * 
 * 🏗️ 设计模式：
 * - 单例模式：全局唯一的登录管理器实例
 * - 门面模式：隐藏内部复杂实现，提供简单易用的API
 * - 策略模式：支持多种登录方式的动态切换
 * - 观察者模式：异步登录结果通知
 * 
 * 📱 使用场景：
 * - 主应用需要用户登录时
 * - 检查用户登录状态时
 * - 用户主动登出时
 * - 切换登录方式时
 * 
 * 🎓 学习要点：
 * 1. API设计原则：简单、一致、易用
 * 2. 异步编程：使用协程处理异步登录操作
 * 3. 状态管理：维护全局登录状态
 * 4. 错误处理：优雅的错误处理和用户反馈
 */
class LoginManager private constructor() {
    
    companion object {
        private const val TAG = "LoginManager"
        
        @Volatile
        private var INSTANCE: LoginManager? = null
        
        /**
         * 获取LoginManager单例实例
         * 
         * 为什么使用单例模式？
         * 1. 全局状态管理：登录状态需要在整个应用中保持一致
         * 2. 资源节约：避免重复创建登录相关的服务实例
         * 3. 配置统一：确保登录配置在整个应用中一致
         */
        fun getInstance(): LoginManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoginManager().also { INSTANCE = it }
            }
        }
    }
    
    // 内部服务实例
    private var weChatLoginService: WeChatLoginService? = null
    private var userStorage: UserStorage? = null
    private var isInitialized = false

    // 延迟初始化相关
    private var appContext: Context? = null
    private var loginConfig: LoginConfig? = null

    // 协程作用域 - 用于管理异步操作
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * 初始化登录模块
     * 
     * 🎯 初始化目的：
     * 1. 配置微信SDK和相关服务
     * 2. 初始化本地存储服务
     * 3. 恢复之前的登录状态
     * 4. 验证配置的有效性
     * 
     * @param context 应用上下文，用于初始化各种服务
     * @param config 登录配置，包含微信AppID等信息
     * 
     * 为什么需要显式初始化？
     * 1. 依赖注入：需要外部提供Context和配置信息
     * 2. 错误处理：初始化过程可能失败，需要明确的结果反馈
     * 3. 延迟加载：避免在模块加载时就进行重量级操作
     */
    /**
     * 注册LoginManager（推荐在Application中调用）
     *
     * 🎯 注册流程：
     * 1. 保存应用上下文
     * 2. 保存配置信息
     * 3. 不进行实际初始化，等待首次使用
     *
     * @param context 应用上下文
     * @param config 登录配置信息
     *
     * 为什么分离注册和初始化？
     * 1. 性能优化：避免Application启动时的重量级操作
     * 2. 按需加载：只有在实际使用时才初始化
     * 3. 错误隔离：初始化失败不影响应用启动
     */
    fun register(context: Context, config: LoginConfig) {
        appContext = context.applicationContext
        loginConfig = config
    }

    /**
     * 确保LoginManager已初始化（内部方法）
     *
     * 🎯 延迟初始化流程：
     * 1. 检查是否已初始化
     * 2. 如果未初始化，进行同步初始化
     * 3. 使用双重检查锁定模式确保线程安全
     */
    private fun ensureInitialized() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val context = appContext ?: throw IllegalStateException(
                        "LoginManager not registered. Call register() in Application first."
                    )
                    val config = loginConfig ?: throw IllegalStateException(
                        "LoginConfig not provided. Call register() with config first."
                    )

                    try {
                        // 初始化用户存储服务
                        userStorage = UserStorage.getInstance(context)

                        // 初始化微信登录服务
                        weChatLoginService = WeChatLoginService.getInstance().apply {
                            initialize(context, config.weChatConfig)
                        }

                        isInitialized = true

                    } catch (e: Exception) {
                        throw LoginInitializationException("Failed to initialize LoginManager", e)
                    }
                }
            }
        }
    }

    /**
     * 微信应用内登录
     * 
     * 🎯 功能说明：
     * 1. 检查微信客户端是否安装
     * 2. 验证微信版本是否支持
     * 3. 调起微信客户端进行授权
     * 4. 处理授权结果和用户信息
     * 
     * @param callback 登录结果回调
     * 
     * 使用场景：
     * - 用户设备已安装微信客户端
     * - 微信版本支持应用内登录
     * - 用户偏好使用应用内登录方式
     */
    fun loginWithWeChatApp(callback: LoginCallback) {
        checkInitialized()
        
        managerScope.launch {
            try {
                weChatLoginService?.loginWithApp(object : InternalLoginCallback {
                    override fun onSuccess(user: User) {
                        // 保存用户信息到本地
                        managerScope.launch {
                            userStorage?.saveUser(user)
                            callback.onSuccess(LoginResult.Success(
                                user = user,
                                loginType = LoginType.WECHAT_APP
                            ))
                        }
                    }
                    
                    override fun onFailure(error: LoginError, message: String, cause: Throwable?) {
                        callback.onFailure(LoginResult.Failure(error, message, cause))
                    }
                })
                
            } catch (e: Exception) {
                callback.onFailure(LoginResult.Failure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = "微信登录失败: ${e.message}",
                    cause = e
                ))
            }
        }
    }
    
    /**
     * 微信二维码登录
     * 
     * 🎯 功能说明：
     * 1. 生成微信登录二维码
     * 2. 开始轮询扫码状态
     * 3. 处理扫码结果和用户信息
     * 4. 管理二维码生命周期
     * 
     * @param callback 登录结果回调
     * 
     * 使用场景：
     * - 用户设备未安装微信客户端
     * - 微信版本过低不支持应用内登录
     * - 用户偏好使用扫码登录方式
     * - 企业环境或公共设备使用
     */
    fun loginWithWeChatQR(callback: LoginCallback) {
        checkInitialized()
        
        managerScope.launch {
            try {
                weChatLoginService?.loginWithQRCode(object : InternalLoginCallback {
                    override fun onSuccess(user: User) {
                        // 保存用户信息到本地
                        managerScope.launch {
                            userStorage?.saveUser(user)
                            callback.onSuccess(LoginResult.Success(
                                user = user,
                                loginType = LoginType.WECHAT_QR
                            ))
                        }
                    }
                    
                    override fun onFailure(error: LoginError, message: String, cause: Throwable?) {
                        callback.onFailure(LoginResult.Failure(error, message, cause))
                    }
                    
                    override fun onProgress(progress: LoginProgress) {
                        callback.onProgress(progress)
                    }
                })
                
            } catch (e: Exception) {
                callback.onFailure(LoginResult.Failure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = "二维码登录失败: ${e.message}",
                    cause = e
                ))
            }
        }
    }
    
    /**
     * 游客登录
     * 
     * 🎯 功能说明：
     * 1. 创建临时游客用户
     * 2. 生成唯一的游客ID
     * 3. 保存游客信息到本地
     * 4. 提供基础功能访问权限
     * 
     * @param callback 登录结果回调
     * 
     * 使用场景：
     * - 用户不想使用微信登录
     * - 快速体验应用功能
     * - 网络环境不支持微信登录
     * - 隐私保护需求
     */
    fun loginAsGuest(callback: LoginCallback) {
        checkInitialized()
        
        managerScope.launch {
            try {
                // 创建游客用户
                val guestUser = createGuestUser()
                
                // 保存到本地存储
                userStorage?.saveUser(guestUser)
                
                // 回调成功结果
                callback.onSuccess(LoginResult.Success(
                    user = guestUser,
                    loginType = LoginType.GUEST
                ))
                
            } catch (e: Exception) {
                callback.onFailure(LoginResult.Failure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = "游客登录失败: ${e.message}",
                    cause = e
                ))
            }
        }
    }
    
    /**
     * 获取当前登录用户
     * 
     * @return 当前登录用户，未登录返回null
     * 
     * 使用场景：
     * - 检查用户登录状态
     * - 获取用户基本信息
     * - 个人中心页面显示
     */
    suspend fun getCurrentUser(): User? {
        checkInitialized()
        return userStorage?.getCurrentUser()
    }
    
    /**
     * 检查是否已登录
     * 
     * @return 是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        checkInitialized()
        return userStorage?.isLoggedIn() ?: false
    }
    
    /**
     * 用户登出
     * 
     * 🎯 登出处理：
     * 1. 清除本地用户信息
     * 2. 清除微信授权信息
     * 3. 重置登录状态
     * 4. 清理相关缓存
     */
    suspend fun logout() {
        checkInitialized()
        
        try {
            // 清除本地用户信息
            userStorage?.clearUser()
            
            // 清除微信相关状态
            weChatLoginService?.clearAuthInfo()
            
        } catch (e: Exception) {
            // 登出失败也不抛异常，确保用户可以重新登录
        }
    }
    
    /**
     * 检查微信客户端状态
     * 
     * @return 微信客户端状态
     */
    fun checkWeChatStatus(): WeChatStatus {
        checkInitialized()
        return weChatLoginService?.checkWeChatStatus() ?: WeChatStatus.NOT_INSTALLED
    }
    
    // ===== 私有方法 =====
    
    /**
     * 检查是否已初始化（公共方法）
     *
     * @return 是否已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }

    /**
     * 检查是否已初始化（内部方法）
     */
    private fun checkInitialized() {
        ensureInitialized() // 自动进行延迟初始化
    }
    
    /**
     * 创建游客用户
     */
    private fun createGuestUser(): User {
        val currentTime = System.currentTimeMillis()
        val guestId = "guest_${currentTime}_${(1000..9999).random()}"
        
        return User(
            id = guestId,
            nickname = "游客用户",
            avatarUrl = null,
            loginType = LoginType.GUEST,
            thirdPartyId = null,
            email = null,
            phoneNumber = null,
            registrationTime = currentTime,
            lastLoginTime = currentTime
        )
    }
}

/**
 * 登录初始化异常
 */
class LoginInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
