package com.example.login.api

/**
 * 登录结果密封类
 * 
 * 🎯 设计目的：
 * 1. 类型安全的登录结果处理
 * 2. 统一的成功和失败数据格式
 * 3. 详细的错误分类和信息
 * 4. 便于模式匹配和结果处理
 * 
 * 🏗️ 设计模式：
 * - 密封类模式：限制继承，确保类型安全
 * - 数据类模式：不可变的数据容器
 * - 结果模式：统一的操作结果表示
 * 
 * 📱 使用场景：
 * - 登录操作结果的统一表示
 * - 错误处理和用户反馈
 * - 日志记录和问题排查
 * - 单元测试的结果验证
 * 
 * 🎓 学习要点：
 * 1. 密封类的使用：限制继承，提供类型安全
 * 2. 数据类的设计：不可变数据容器
 * 3. 错误处理：详细的错误分类
 * 4. API设计：清晰的结果表示
 */
sealed class LoginResult {
    
    /**
     * 登录成功结果
     * 
     * 🎯 包含信息：
     * 1. 登录用户的详细信息
     * 2. 使用的登录方式类型
     * 3. 登录成功的时间戳
     * 4. 可选的额外信息
     * 
     * @param user 登录成功的用户信息
     * @param loginType 使用的登录方式
     * @param timestamp 登录成功的时间戳
     * @param extras 额外信息（可选）
     * 
     * 使用示例：
     * ```kotlin
     * when (result) {
     *     is LoginResult.Success -> {
     *         val user = result.user
     *         showWelcomeMessage("欢迎 ${user.nickname}")
     *         navigateToMainScreen()
     *     }
     * }
     * ```
     */
    data class Success(
        val user: User,
        val loginType: LoginType,
        val timestamp: Long = System.currentTimeMillis(),
        val extras: Map<String, Any> = emptyMap()
    ) : LoginResult()
    
    /**
     * 登录失败结果
     * 
     * 🎯 包含信息：
     * 1. 详细的错误类型分类
     * 2. 用户友好的错误信息
     * 3. 原始异常信息（用于调试）
     * 4. 失败发生的时间戳
     * 
     * @param error 错误类型，用于程序逻辑判断
     * @param message 用户友好的错误信息
     * @param cause 原始异常，用于调试和日志记录
     * @param timestamp 失败发生的时间戳
     * 
     * 使用示例：
     * ```kotlin
     * when (result) {
     *     is LoginResult.Failure -> {
     *         when (result.error) {
     *             LoginError.WECHAT_NOT_INSTALLED -> {
     *                 showQRCodeLoginOption()
     *             }
     *             LoginError.NETWORK_ERROR -> {
     *                 showRetryOption()
     *             }
     *             else -> {
     *                 showErrorMessage(result.message)
     *             }
     *         }
     *     }
     * }
     * ```
     */
    data class Failure(
        val error: LoginError,
        val message: String,
        val cause: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : LoginResult()
}

/**
 * 登录错误类型枚举
 * 
 * 🎯 设计目的：
 * 1. 详细分类各种登录失败情况
 * 2. 便于程序逻辑判断和处理
 * 3. 提供针对性的用户引导
 * 4. 支持错误统计和分析
 * 
 * 错误分类原则：
 * 1. 按照错误原因分类（网络、用户操作、系统环境等）
 * 2. 按照处理方式分类（可重试、需要用户操作、不可恢复等）
 * 3. 按照错误严重程度分类（警告、错误、致命错误等）
 */
enum class LoginError(
    val code: String,
    val description: String,
    val isRetryable: Boolean = false,
    val needUserAction: Boolean = false
) {
    
    /**
     * 网络相关错误
     */
    NETWORK_ERROR(
        code = "NETWORK_ERROR",
        description = "网络连接失败，请检查网络设置",
        isRetryable = true
    ),
    
    NETWORK_TIMEOUT(
        code = "NETWORK_TIMEOUT", 
        description = "网络请求超时，请重试",
        isRetryable = true
    ),
    
    SERVER_ERROR(
        code = "SERVER_ERROR",
        description = "服务器错误，请稍后重试",
        isRetryable = true
    ),
    
    /**
     * 用户操作相关错误
     */
    USER_CANCELLED(
        code = "USER_CANCELLED",
        description = "用户取消了登录操作",
        needUserAction = true
    ),
    
    AUTH_DENIED(
        code = "AUTH_DENIED",
        description = "用户拒绝了授权请求",
        needUserAction = true
    ),
    
    /**
     * 微信客户端相关错误
     */
    WECHAT_NOT_INSTALLED(
        code = "WECHAT_NOT_INSTALLED",
        description = "未安装微信客户端，请安装后重试或使用二维码登录",
        needUserAction = true
    ),
    
    WECHAT_VERSION_LOW(
        code = "WECHAT_VERSION_LOW",
        description = "微信版本过低，请更新微信或使用二维码登录",
        needUserAction = true
    ),
    
    WECHAT_AUTH_FAILED(
        code = "WECHAT_AUTH_FAILED",
        description = "微信授权失败，请重试",
        isRetryable = true
    ),
    
    /**
     * 二维码相关错误
     */
    QR_CODE_EXPIRED(
        code = "QR_CODE_EXPIRED",
        description = "二维码已过期，请刷新后重试",
        isRetryable = true,
        needUserAction = true
    ),
    
    QR_CODE_GENERATION_FAILED(
        code = "QR_CODE_GENERATION_FAILED",
        description = "二维码生成失败，请重试",
        isRetryable = true
    ),
    
    /**
     * 认证相关错误
     */
    AUTH_FAILED(
        code = "AUTH_FAILED",
        description = "身份认证失败，请重试",
        isRetryable = true
    ),
    
    TOKEN_INVALID(
        code = "TOKEN_INVALID",
        description = "登录凭证无效，请重新登录",
        needUserAction = true
    ),
    
    /**
     * 系统相关错误
     */
    PERMISSION_DENIED(
        code = "PERMISSION_DENIED",
        description = "缺少必要权限，请检查应用权限设置",
        needUserAction = true
    ),
    
    STORAGE_ERROR(
        code = "STORAGE_ERROR",
        description = "本地存储错误，请检查存储空间",
        needUserAction = true
    ),
    
    /**
     * 未知错误
     */
    UNKNOWN_ERROR(
        code = "UNKNOWN_ERROR",
        description = "未知错误，请重试或联系客服",
        isRetryable = true
    );
    
    /**
     * 获取用户友好的错误提示
     * 
     * @param context 上下文信息（可选）
     * @return 用户友好的错误提示
     */
    fun getUserFriendlyMessage(context: String? = null): String {
        return if (context != null) {
            "$description ($context)"
        } else {
            description
        }
    }
    
    /**
     * 是否可以自动重试
     * 
     * @return 是否可以自动重试
     */
    fun canAutoRetry(): Boolean {
        return isRetryable && !needUserAction
    }
}

/**
 * 登录进度信息
 * 
 * 🎯 设计目的：
 * 1. 提供登录过程的实时进度反馈
 * 2. 改善用户体验，减少等待焦虑
 * 3. 支持复杂登录流程的状态管理
 * 4. 便于调试和问题排查
 * 
 * @param type 进度类型
 * @param message 进度描述信息
 * @param data 进度相关数据（可选）
 * @param timestamp 进度更新时间戳
 */
data class LoginProgress(
    val type: ProgressType,
    val message: String,
    val data: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 登录进度类型枚举
 */
enum class ProgressType(val description: String) {
    
    // 初始化阶段
    INITIALIZING("正在初始化..."),
    
    // 微信应用内登录进度
    WECHAT_APP_LAUNCHING("正在启动微信..."),
    WECHAT_APP_AUTHORIZING("等待微信授权..."),
    
    // 二维码登录进度
    QR_CODE_GENERATING("正在生成二维码..."),
    QR_CODE_GENERATED("二维码已生成"),
    QR_CODE_WAITING_SCAN("等待扫码..."),
    QR_CODE_SCANNED("已扫码，请在微信中确认"),
    QR_CODE_CONFIRMING("等待确认授权..."),
    QR_CODE_EXPIRED("二维码已过期"),
    
    // 用户信息处理
    USER_INFO_FETCHING("正在获取用户信息..."),
    USER_INFO_SAVING("正在保存用户信息..."),
    
    // 完成阶段
    LOGIN_COMPLETING("登录即将完成..."),
    LOGIN_COMPLETED("登录完成")
}
