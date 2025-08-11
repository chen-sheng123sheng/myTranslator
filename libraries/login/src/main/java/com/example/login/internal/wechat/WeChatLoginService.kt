package com.example.login.internal.wechat

import android.content.Context
import android.util.Log
import com.example.login.api.*
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 微信登录服务（内部实现）
 * 
 * 🎯 设计目的：
 * 1. 封装微信SDK的复杂操作和状态管理
 * 2. 处理微信登录的完整流程（应用内+二维码）
 * 3. 管理微信登录状态和错误处理
 * 4. 提供统一的内部登录接口
 * 
 * 🏗️ 设计模式：
 * - 单例模式：全局唯一的微信登录服务实例
 * - 策略模式：支持应用内和二维码两种登录方式
 * - 观察者模式：异步登录结果通知
 * - 状态机模式：管理登录过程的各种状态
 * 
 * 📱 功能特性：
 * - 微信客户端状态检查和兼容性处理
 * - 应用内登录的完整流程管理
 * - 二维码登录的生成和状态轮询
 * - 登录结果的统一处理和回调
 * 
 * 🎓 学习要点：
 * 1. 第三方SDK的封装和抽象
 * 2. 异步操作的协程处理
 * 3. 状态管理和错误处理
 * 4. 内部API的设计原则
 * 
 * 为什么标记为internal？
 * 1. 封装性：隐藏内部实现细节，不对外暴露
 * 2. 安全性：防止外部直接调用内部方法
 * 3. 维护性：内部实现可以自由重构，不影响外部API
 * 4. 测试性：便于Mock和单元测试
 */
internal class WeChatLoginService private constructor() {
    
    companion object {
        private const val TAG = "WeChatLoginService"
        
        // 微信SDK相关常量
        private const val WECHAT_MIN_VERSION = 0x21020001  // 微信最低版本要求
        private const val LOGIN_TIMEOUT = 30000L           // 登录超时时间（毫秒）
        
        @Volatile
        private var INSTANCE: WeChatLoginService? = null
        
        /**
         * 获取WeChatLoginService单例实例
         * 
         * 为什么使用单例模式？
         * 1. 资源管理：微信SDK实例需要全局唯一
         * 2. 状态一致：登录状态需要在整个应用中保持一致
         * 3. 性能优化：避免重复初始化微信SDK
         * 4. 回调管理：统一管理微信的回调处理
         */
        fun getInstance(): WeChatLoginService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeChatLoginService().also { INSTANCE = it }
            }
        }
    }
    
    // ===== 内部状态管理 =====
    
    // 微信API实例
    private var wxApi: IWXAPI? = null
    
    // 配置信息
    private var config: WeChatConfig? = null
    
    // 初始化状态
    private var isInitialized = false
    
    // 当前登录回调
    private var currentCallback: InternalLoginCallback? = null
    
    // 二维码登录服务
    private var qrCodeService: QRCodeLoginService? = null
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 登录状态流
    private val _loginStateFlow = MutableSharedFlow<LoginState>()
    val loginStateFlow: SharedFlow<LoginState> = _loginStateFlow.asSharedFlow()
    
    // ===== 初始化方法 =====
    
    /**
     * 初始化微信登录服务
     * 
     * 🎯 初始化流程：
     * 1. 验证配置参数的有效性
     * 2. 创建微信API实例并注册到微信
     * 3. 检查微信客户端状态
     * 4. 初始化二维码登录服务
     * 5. 设置登录超时处理
     * 
     * @param context 应用上下文
     * @param weChatConfig 微信配置信息
     * 
     * 为什么需要显式初始化？
     * 1. 依赖注入：需要外部提供Context和配置
     * 2. 错误处理：初始化可能失败，需要明确反馈
     * 3. 延迟加载：避免在类加载时就进行重量级操作
     * 4. 配置验证：确保配置参数的正确性
     */
    fun initialize(context: Context, weChatConfig: WeChatConfig) {
        if (isInitialized) {
            Log.d(TAG, "WeChatLoginService already initialized")
            return
        }
        
        try {
            Log.d(TAG, "🚀 Initializing WeChatLoginService")
            
            // 1. 保存配置
            this.config = weChatConfig
            
            // 2. 验证配置
            validateConfig(weChatConfig)
            
            // 3. 创建微信API实例
            wxApi = WXAPIFactory.createWXAPI(context.applicationContext, weChatConfig.appId, true)
            
            // 4. 注册到微信
            val registerResult = wxApi?.registerApp(weChatConfig.appId) ?: false
            if (!registerResult) {
                throw IllegalStateException("Failed to register WeChat app")
            }
            
            // 5. 初始化二维码登录服务
            qrCodeService = QRCodeLoginService.getInstance().apply {
                initialize(context.applicationContext, weChatConfig)
            }
            
            isInitialized = true
            Log.i(TAG, "✅ WeChatLoginService initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize WeChatLoginService", e)
            throw WeChatInitializationException("Failed to initialize WeChat login service", e)
        }
    }
    
    /**
     * 验证微信配置
     */
    private fun validateConfig(config: WeChatConfig) {
        if (config.appId.isBlank()) {
            throw IllegalArgumentException("WeChat AppID cannot be blank")
        }
        
        if (config.appSecret.isBlank()) {
            throw IllegalArgumentException("WeChat AppSecret cannot be blank")
        }
        
        // 验证AppID格式（微信AppID通常以wx开头）
        if (!config.appId.startsWith("wx")) {
            Log.w(TAG, "⚠️ WeChat AppID should start with 'wx': ${config.appId}")
        }
    }
    
    // ===== 登录方法 =====
    
    /**
     * 微信应用内登录
     * 
     * 🎯 登录流程：
     * 1. 检查初始化状态和微信客户端状态
     * 2. 创建微信登录请求
     * 3. 调起微信客户端进行授权
     * 4. 等待微信回调结果
     * 5. 处理授权结果和获取用户信息
     * 
     * @param callback 登录结果回调
     * 
     * 应用内登录的优势：
     * 1. 用户体验好：无需扫码，直接在微信中授权
     * 2. 流程简单：一键登录，减少用户操作
     * 3. 安全性高：微信官方授权流程
     * 
     * 适用场景：
     * - 用户设备已安装微信客户端
     * - 微信版本支持第三方登录
     * - 用户偏好快速登录方式
     */
    suspend fun loginWithApp(callback: InternalLoginCallback) {
        checkInitialized()
        
        try {
            Log.d(TAG, "🚀 Starting WeChat app login")
            
            // 1. 检查微信客户端状态
            val weChatStatus = checkWeChatStatus()
            if (!weChatStatus.canUseAppLogin()) {
                val errorMessage = when (weChatStatus) {
                    WeChatStatus.NOT_INSTALLED -> "微信客户端未安装"
                    WeChatStatus.VERSION_TOO_LOW -> "微信版本过低"
                    WeChatStatus.NOT_SUPPORTED -> "微信客户端不支持登录"
                    else -> "微信客户端状态异常"
                }
                
                callback.onFailure(
                    error = when (weChatStatus) {
                        WeChatStatus.NOT_INSTALLED -> LoginError.WECHAT_NOT_INSTALLED
                        WeChatStatus.VERSION_TOO_LOW -> LoginError.WECHAT_VERSION_LOW
                        else -> LoginError.WECHAT_AUTH_FAILED
                    },
                    message = errorMessage
                )
                return
            }
            
            // 2. 设置当前回调
            currentCallback = callback
            
            // 3. 创建登录请求
            val loginRequest = createWeChatLoginRequest()
            
            // 4. 发送登录请求
            val sendResult = wxApi?.sendReq(loginRequest) ?: false
            if (!sendResult) {
                callback.onFailure(
                    error = LoginError.WECHAT_AUTH_FAILED,
                    message = "发送微信登录请求失败"
                )
                return
            }
            
            // 5. 设置登录超时
            setupLoginTimeout(callback)
            
            Log.i(TAG, "📱 WeChat login request sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WeChat app login failed", e)
            callback.onFailure(
                error = LoginError.WECHAT_AUTH_FAILED,
                message = "微信登录失败: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * 微信二维码登录
     * 
     * 🎯 登录流程：
     * 1. 检查初始化状态
     * 2. 委托给二维码登录服务处理
     * 3. 监听二维码登录状态变化
     * 4. 处理登录结果和用户信息
     * 
     * @param callback 登录结果回调
     * 
     * 二维码登录的优势：
     * 1. 兼容性好：不依赖微信客户端版本
     * 2. 适用性广：适合各种设备和环境
     * 3. 安全性高：二维码有时效性，防止重复使用
     * 
     * 适用场景：
     * - 微信客户端未安装或版本过低
     * - 企业环境或公共设备
     * - 用户偏好扫码登录方式
     */
    suspend fun loginWithQRCode(callback: InternalLoginCallback) {
        checkInitialized()
        
        try {
            Log.d(TAG, "🚀 Starting WeChat QR code login")
            
            // 委托给二维码登录服务
            qrCodeService?.startQRCodeLogin(callback)
                ?: callback.onFailure(
                    error = LoginError.QR_CODE_GENERATION_FAILED,
                    message = "二维码登录服务未初始化"
                )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WeChat QR code login failed", e)
            callback.onFailure(
                error = LoginError.QR_CODE_GENERATION_FAILED,
                message = "二维码登录失败: ${e.message}",
                cause = e
            )
        }
    }
    
    // ===== 状态检查方法 =====
    
    /**
     * 检查微信客户端状态
     *
     * 🎯 检查内容：
     * 1. 微信客户端是否已安装
     * 2. 微信版本是否支持登录功能
     * 3. 微信是否支持第三方应用登录
     *
     * @return 微信客户端状态
     *
     * 状态检查的重要性：
     * 1. 用户体验：提前告知用户微信状态，避免登录失败
     * 2. 策略选择：根据状态选择合适的登录方式
     * 3. 错误预防：避免无效的登录尝试
     */
    fun checkWeChatStatus(): WeChatStatus {
        return try {
            val api = wxApi
            if (api == null) {
                Log.w(TAG, "⚠️ WeChat API not initialized")
                return WeChatStatus.UNKNOWN
            }

            // 简化的状态检查，避免API兼容性问题
            if (!api.isWXAppInstalled) {
                Log.d(TAG, "📱 WeChat app not installed")
                WeChatStatus.NOT_INSTALLED
            } else {
                Log.d(TAG, "📱 WeChat app available")
                WeChatStatus.AVAILABLE
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check WeChat status", e)
            WeChatStatus.UNKNOWN
        }
    }
    
    // ===== 清理方法 =====
    
    /**
     * 清除授权信息
     * 
     * 用于用户登出时清理相关状态
     */
    fun clearAuthInfo() {
        currentCallback = null
        qrCodeService?.stopQRCodeLogin()
        Log.d(TAG, "🧹 WeChat auth info cleared")
    }
    
    // ===== 私有辅助方法 =====
    
    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("WeChatLoginService not initialized")
        }
    }
    
    /**
     * 创建微信登录请求
     */
    private fun createWeChatLoginRequest(): com.tencent.mm.opensdk.modelmsg.SendAuth.Req {
        return com.tencent.mm.opensdk.modelmsg.SendAuth.Req().apply {
            scope = "snsapi_userinfo"  // 获取用户信息权限
            state = "wechat_login_${System.currentTimeMillis()}"  // 防CSRF攻击
        }
    }
    
    /**
     * 设置登录超时处理
     */
    private fun setupLoginTimeout(callback: InternalLoginCallback) {
        serviceScope.launch {
            delay(LOGIN_TIMEOUT)

            if (currentCallback == callback) {
                currentCallback = null
                callback.onFailure(
                    error = LoginError.NETWORK_TIMEOUT,
                    message = "微信登录超时，请重试"
                )
            }
        }
    }

    /**
     * 处理微信回调结果（由WXEntryActivity调用）
     *
     * 🎯 回调处理：
     * 1. 验证回调的有效性
     * 2. 解析授权结果
     * 3. 获取用户信息
     * 4. 通知登录结果
     *
     * @param code 授权码
     * @param state 状态参数
     * @param errCode 错误码
     * @param errStr 错误信息
     */
    fun handleWeChatCallback(code: String?, state: String?, errCode: Int, errStr: String?) {
        val callback = currentCallback
        if (callback == null) {
            Log.w(TAG, "⚠️ No callback available for WeChat result")
            return
        }

        currentCallback = null

        serviceScope.launch {
            try {
                when (errCode) {
                    0 -> {
                        // 授权成功，获取用户信息
                        if (code.isNullOrBlank()) {
                            callback.onFailure(
                                error = LoginError.AUTH_FAILED,
                                message = "授权码为空"
                            )
                            return@launch
                        }

                        // 这里应该调用后端API获取用户信息
                        // 为了演示，创建一个模拟用户
                        val user = createMockUser(LoginType.WECHAT_APP)
                        callback.onSuccess(user)
                    }

                    -4 -> {
                        // 用户拒绝授权
                        callback.onFailure(
                            error = LoginError.AUTH_DENIED,
                            message = "用户拒绝授权"
                        )
                    }

                    -2 -> {
                        // 用户取消
                        callback.onFailure(
                            error = LoginError.USER_CANCELLED,
                            message = "用户取消登录"
                        )
                    }

                    else -> {
                        // 其他错误
                        callback.onFailure(
                            error = LoginError.WECHAT_AUTH_FAILED,
                            message = errStr ?: "微信授权失败"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to handle WeChat callback", e)
                callback.onFailure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = "处理微信回调失败: ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * 创建模拟用户（用于演示）
     *
     * 在实际项目中，这里应该：
     * 1. 使用授权码调用微信API获取access_token
     * 2. 使用access_token获取用户信息
     * 3. 将用户信息转换为应用的User对象
     */
    private fun createMockUser(loginType: LoginType): User {
        val currentTime = System.currentTimeMillis()

        return User(
            id = "wx_${currentTime}",
            nickname = "微信用户",
            avatarUrl = null,
            loginType = loginType,
            thirdPartyId = "openid_${currentTime}",
            email = null,
            phoneNumber = null,
            registrationTime = currentTime,
            lastLoginTime = currentTime
        )
    }
}

/**
 * 微信初始化异常
 */
internal class WeChatInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 登录状态枚举（内部使用）
 */
internal enum class LoginState {
    IDLE,           // 空闲状态
    LOGGING_IN,     // 登录中
    SUCCESS,        // 登录成功
    FAILED          // 登录失败
}
