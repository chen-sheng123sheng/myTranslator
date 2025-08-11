package com.example.login.internal.wechat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.login.api.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * 二维码登录服务（内部实现）
 * 
 * 🎯 设计目的：
 * 1. 生成和管理微信登录二维码
 * 2. 处理二维码扫码状态轮询
 * 3. 管理二维码生命周期和过期机制
 * 4. 提供二维码登录的完整解决方案
 * 
 * 🏗️ 设计模式：
 * - 状态机模式：管理二维码的各种状态（生成、等待扫码、已扫码、过期等）
 * - 轮询模式：定期检查扫码状态，实时更新登录进度
 * - 生命周期模式：管理二维码的创建、使用和销毁
 * - 观察者模式：状态变化的实时通知
 * 
 * 📱 功能特性：
 * - 高质量二维码生成（使用ZXing库）
 * - 智能轮询机制（根据状态调整轮询频率）
 * - 自动过期处理（防止二维码被重复使用）
 * - 完整的错误处理和恢复机制
 * 
 * 🎓 学习要点：
 * 1. 二维码生成技术和参数优化
 * 2. 轮询机制的设计和实现
 * 3. 状态机的应用和状态转换
 * 4. 协程和Flow的高级用法
 * 
 * 为什么独立成服务？
 * 1. 职责分离：二维码逻辑与微信SDK逻辑分离
 * 2. 可复用性：二维码功能可以被其他登录方式复用
 * 3. 可测试性：独立的服务便于单元测试
 * 4. 可扩展性：便于支持其他平台的二维码登录
 */
internal class QRCodeLoginService private constructor() {
    
    companion object {
        private const val TAG = "QRCodeLoginService"
        
        // 二维码相关常量
        private const val QR_CODE_SIZE = 512                    // 二维码尺寸（像素）
        private const val QR_CODE_MARGIN = 2                    // 二维码边距
        private const val QR_CODE_EXPIRE_TIME = 5 * 60 * 1000L  // 二维码有效期（5分钟）
        
        // 轮询相关常量
        private const val POLLING_INTERVAL = 2000L              // 轮询间隔（2秒）
        private const val POLLING_TIMEOUT = 10 * 60 * 1000L     // 轮询超时（10分钟）
        
        @Volatile
        private var INSTANCE: QRCodeLoginService? = null
        
        /**
         * 获取QRCodeLoginService单例实例
         */
        fun getInstance(): QRCodeLoginService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QRCodeLoginService().also { INSTANCE = it }
            }
        }
    }
    
    // ===== 内部状态管理 =====
    
    // 配置信息
    private var config: WeChatConfig? = null
    
    // 初始化状态
    private var isInitialized = false
    
    // 当前二维码信息
    private var currentQRCode: QRCodeInfo? = null
    
    // 当前登录回调
    private var currentCallback: InternalLoginCallback? = null
    
    // 轮询任务
    private var pollingJob: Job? = null
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 二维码状态流
    private val _qrCodeStateFlow = MutableStateFlow<QRCodeState>(QRCodeState.Idle)
    val qrCodeStateFlow: StateFlow<QRCodeState> = _qrCodeStateFlow.asStateFlow()
    
    // ===== 初始化方法 =====
    
    /**
     * 初始化二维码登录服务
     * 
     * @param context 应用上下文
     * @param weChatConfig 微信配置
     */
    fun initialize(context: Context, weChatConfig: WeChatConfig) {
        if (isInitialized) {
            Log.d(TAG, "QRCodeLoginService already initialized")
            return
        }
        
        try {
            Log.d(TAG, "🚀 Initializing QRCodeLoginService")
            
            this.config = weChatConfig
            isInitialized = true
            
            Log.i(TAG, "✅ QRCodeLoginService initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize QRCodeLoginService", e)
            throw QRCodeInitializationException("Failed to initialize QR code login service", e)
        }
    }
    
    // ===== 二维码登录方法 =====
    
    /**
     * 开始二维码登录流程
     * 
     * 🎯 登录流程：
     * 1. 生成登录二维码和票据
     * 2. 开始轮询扫码状态
     * 3. 处理状态变化和用户反馈
     * 4. 完成登录或处理错误
     * 
     * @param callback 登录结果回调
     */
    suspend fun startQRCodeLogin(callback: InternalLoginCallback) {
        checkInitialized()
        
        try {
            Log.d(TAG, "🚀 Starting QR code login")
            
            // 1. 停止之前的登录流程
            stopQRCodeLogin()
            
            // 2. 设置当前回调
            currentCallback = callback
            
            // 3. 生成二维码
            callback.onProgress(LoginProgress(
                type = ProgressType.QR_CODE_GENERATING,
                message = "正在生成二维码..."
            ))
            
            val qrCodeInfo = generateQRCode()
            currentQRCode = qrCodeInfo
            
            // 4. 通知二维码生成完成
            callback.onProgress(LoginProgress(
                type = ProgressType.QR_CODE_GENERATED,
                message = "二维码已生成，请使用微信扫描",
                data = qrCodeInfo.bitmap
            ))
            
            // 5. 开始轮询状态
            startPolling(qrCodeInfo.ticket, callback)
            
            Log.i(TAG, "📱 QR code login started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start QR code login", e)
            callback.onFailure(
                error = LoginError.QR_CODE_GENERATION_FAILED,
                message = "二维码登录启动失败: ${e.message}",
                cause = e
            )
        }
    }
    
    /**
     * 停止二维码登录流程
     */
    fun stopQRCodeLogin() {
        Log.d(TAG, "🛑 Stopping QR code login")
        
        // 取消轮询任务
        pollingJob?.cancel()
        pollingJob = null
        
        // 清理状态
        currentCallback = null
        currentQRCode = null
        
        // 重置状态
        _qrCodeStateFlow.value = QRCodeState.Idle
    }
    
    /**
     * 刷新二维码
     * 
     * @param callback 登录结果回调
     */
    suspend fun refreshQRCode(callback: InternalLoginCallback) {
        Log.d(TAG, "🔄 Refreshing QR code")
        
        // 重新开始登录流程
        startQRCodeLogin(callback)
    }
    
    // ===== 二维码生成方法 =====
    
    /**
     * 生成登录二维码
     * 
     * 🎯 生成流程：
     * 1. 创建唯一的登录票据
     * 2. 构建二维码内容（包含票据和时间戳）
     * 3. 使用ZXing库生成二维码图片
     * 4. 设置二维码参数（尺寸、容错等级等）
     * 
     * @return 二维码信息对象
     */
    private suspend fun generateQRCode(): QRCodeInfo = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎨 Generating QR code")
            
            // 1. 生成唯一票据
            val ticket = generateTicket()
            val timestamp = System.currentTimeMillis()
            
            // 2. 构建二维码内容
            val qrContent = buildQRCodeContent(ticket, timestamp)
            
            // 3. 设置二维码生成参数
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",           // 字符编码
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,  // 容错等级
                EncodeHintType.MARGIN to QR_CODE_MARGIN            // 边距
            )
            
            // 4. 生成二维码
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints)
            
            // 5. 转换为Bitmap
            val bitmap = createBitmapFromBitMatrix(bitMatrix)
            
            // 6. 创建二维码信息对象
            val qrCodeInfo = QRCodeInfo(
                ticket = ticket,
                content = qrContent,
                bitmap = bitmap,
                createTime = timestamp,
                expireTime = timestamp + QR_CODE_EXPIRE_TIME
            )
            
            Log.i(TAG, "✅ QR code generated successfully: $ticket")
            return@withContext qrCodeInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to generate QR code", e)
            throw QRCodeGenerationException("Failed to generate QR code", e)
        }
    }
    
    /**
     * 生成登录票据
     * 
     * 票据格式：qr_login_{timestamp}_{random}
     * 
     * @return 唯一的登录票据
     */
    private fun generateTicket(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        return "qr_login_${timestamp}_$random"
    }
    
    /**
     * 构建二维码内容
     * 
     * 二维码内容格式：
     * {
     *   "type": "wechat_login",
     *   "ticket": "登录票据",
     *   "timestamp": "时间戳",
     *   "app_id": "应用ID"
     * }
     * 
     * @param ticket 登录票据
     * @param timestamp 时间戳
     * @return 二维码内容字符串
     */
    private fun buildQRCodeContent(ticket: String, timestamp: Long): String {
        val config = this.config ?: throw IllegalStateException("Config not initialized")
        
        // 在实际项目中，这里应该是一个URL，指向后端的二维码登录接口
        // 例如：https://api.example.com/qr-login?ticket=xxx&timestamp=xxx
        return "wechat://login?ticket=$ticket&timestamp=$timestamp&app_id=${config.appId}"
    }
    
    /**
     * 从BitMatrix创建Bitmap
     * 
     * @param bitMatrix ZXing生成的位矩阵
     * @return 二维码Bitmap
     */
    private fun createBitmapFromBitMatrix(bitMatrix: com.google.zxing.common.BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) {
                    0xFF000000.toInt()  // 黑色
                } else {
                    0xFFFFFFFF.toInt()  // 白色
                }
            }
        }
        
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
    
    // ===== 轮询方法 =====
    
    /**
     * 开始轮询二维码状态
     * 
     * 🎯 轮询机制：
     * 1. 定期检查二维码扫码状态
     * 2. 根据状态变化通知用户
     * 3. 处理登录成功或失败
     * 4. 自动处理二维码过期
     * 
     * @param ticket 二维码票据
     * @param callback 登录回调
     */
    private fun startPolling(ticket: String, callback: InternalLoginCallback) {
        Log.d(TAG, "🔄 Starting QR code status polling: $ticket")
        
        pollingJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                while (isActive) {
                    // 检查轮询超时
                    if (System.currentTimeMillis() - startTime > POLLING_TIMEOUT) {
                        callback.onFailure(
                            error = LoginError.NETWORK_TIMEOUT,
                            message = "二维码登录超时"
                        )
                        break
                    }
                    
                    // 检查二维码是否过期
                    val qrCode = currentQRCode
                    if (qrCode != null && System.currentTimeMillis() > qrCode.expireTime) {
                        callback.onProgress(LoginProgress(
                            type = ProgressType.QR_CODE_EXPIRED,
                            message = "二维码已过期，请刷新"
                        ))
                        
                        callback.onFailure(
                            error = LoginError.QR_CODE_EXPIRED,
                            message = "二维码已过期，请刷新后重试"
                        )
                        break
                    }
                    
                    // 检查扫码状态
                    val status = checkQRCodeStatus(ticket)
                    handleStatusChange(status, callback)
                    
                    // 如果登录完成，停止轮询
                    if (status is QRCodeStatus.LoginSuccess || status is QRCodeStatus.LoginFailed) {
                        break
                    }
                    
                    // 等待下次轮询
                    delay(POLLING_INTERVAL)
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "🛑 QR code polling cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "❌ QR code polling failed", e)
                callback.onFailure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = "状态检查失败: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    /**
     * 检查二维码状态
     * 
     * 在实际项目中，这里应该调用后端API检查扫码状态
     * 
     * @param ticket 二维码票据
     * @return 二维码状态
     */
    private suspend fun checkQRCodeStatus(ticket: String): QRCodeStatus = withContext(Dispatchers.IO) {
        try {
            // 模拟网络请求延迟
            delay(100)
            
            // 在实际项目中，这里应该是真实的API调用
            // 为了演示，我们模拟不同的状态
            return@withContext simulateQRCodeStatus(ticket)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check QR code status", e)
            return@withContext QRCodeStatus.Error("状态检查失败: ${e.message}")
        }
    }
    
    /**
     * 模拟二维码状态（用于演示）
     * 
     * 在实际项目中，这个方法应该被真实的API调用替换
     */
    private fun simulateQRCodeStatus(ticket: String): QRCodeStatus {
        val qrCode = currentQRCode ?: return QRCodeStatus.Error("二维码信息丢失")
        val elapsedTime = System.currentTimeMillis() - qrCode.createTime
        
        return when {
            elapsedTime < 10000 -> QRCodeStatus.WaitingScan("等待扫码...")
            elapsedTime < 20000 -> QRCodeStatus.Scanned("已扫码，请在微信中确认登录")
            elapsedTime < 30000 -> {
                // 模拟登录成功
                val user = createMockUser(LoginType.WECHAT_QR)
                QRCodeStatus.LoginSuccess(user)
            }
            else -> QRCodeStatus.Error("模拟登录超时")
        }
    }
    
    /**
     * 处理状态变化
     * 
     * @param status 新的状态
     * @param callback 登录回调
     */
    private fun handleStatusChange(status: QRCodeStatus, callback: InternalLoginCallback) {
        when (status) {
            is QRCodeStatus.WaitingScan -> {
                _qrCodeStateFlow.value = QRCodeState.WaitingScan
                callback.onProgress(LoginProgress(
                    type = ProgressType.QR_CODE_WAITING_SCAN,
                    message = status.message
                ))
            }
            
            is QRCodeStatus.Scanned -> {
                _qrCodeStateFlow.value = QRCodeState.Scanned
                callback.onProgress(LoginProgress(
                    type = ProgressType.QR_CODE_SCANNED,
                    message = status.message
                ))
            }
            
            is QRCodeStatus.LoginSuccess -> {
                _qrCodeStateFlow.value = QRCodeState.LoginSuccess
                callback.onSuccess(status.user)
            }
            
            is QRCodeStatus.LoginFailed -> {
                _qrCodeStateFlow.value = QRCodeState.LoginFailed
                callback.onFailure(
                    error = LoginError.AUTH_FAILED,
                    message = status.message
                )
            }
            
            is QRCodeStatus.Error -> {
                _qrCodeStateFlow.value = QRCodeState.Error
                callback.onFailure(
                    error = LoginError.UNKNOWN_ERROR,
                    message = status.message
                )
            }
        }
    }
    
    // ===== 私有辅助方法 =====
    
    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("QRCodeLoginService not initialized")
        }
    }
    
    /**
     * 创建模拟用户（用于演示）
     */
    private fun createMockUser(loginType: LoginType): User {
        val currentTime = System.currentTimeMillis()

        return User(
            id = "qr_${currentTime}",
            nickname = "二维码用户",
            avatarUrl = null,
            loginType = loginType,
            thirdPartyId = "qr_openid_${currentTime}",
            email = null,
            phoneNumber = null,
            registrationTime = currentTime,
            lastLoginTime = currentTime
        )
    }
}

/**
 * 二维码信息数据类
 */
internal data class QRCodeInfo(
    val ticket: String,        // 登录票据
    val content: String,       // 二维码内容
    val bitmap: Bitmap,        // 二维码图片
    val createTime: Long,      // 创建时间
    val expireTime: Long       // 过期时间
)

/**
 * 二维码状态密封类
 */
internal sealed class QRCodeState {
    object Idle : QRCodeState()           // 空闲状态
    object Generating : QRCodeState()     // 生成中
    object WaitingScan : QRCodeState()    // 等待扫码
    object Scanned : QRCodeState()        // 已扫码
    object LoginSuccess : QRCodeState()   // 登录成功
    object LoginFailed : QRCodeState()    // 登录失败
    object Error : QRCodeState()          // 错误状态
}

/**
 * 二维码状态检查结果
 */
internal sealed class QRCodeStatus {
    data class WaitingScan(val message: String) : QRCodeStatus()
    data class Scanned(val message: String) : QRCodeStatus()
    data class LoginSuccess(val user: User) : QRCodeStatus()
    data class LoginFailed(val message: String) : QRCodeStatus()
    data class Error(val message: String) : QRCodeStatus()
}

/**
 * 二维码初始化异常
 */
internal class QRCodeInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 二维码生成异常
 */
internal class QRCodeGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)
