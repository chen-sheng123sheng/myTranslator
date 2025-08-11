package com.example.login.api

/**
 * 微信客户端状态枚举
 * 
 * 🎯 设计目的：
 * 1. 明确区分微信客户端的各种状态
 * 2. 便于程序逻辑判断和用户引导
 * 3. 提供针对性的解决方案提示
 * 4. 支持状态变化的监听和处理
 * 
 * 🏗️ 设计模式：
 * - 状态模式：明确的状态定义和转换
 * - 策略模式：不同状态对应不同的处理策略
 * - 枚举模式：类型安全的状态表示
 * 
 * 📱 使用场景：
 * - 登录前检查微信客户端状态
 * - 根据状态选择合适的登录方式
 * - 向用户提供状态相关的提示信息
 * - 统计不同状态的用户分布
 * 
 * 🎓 学习要点：
 * 1. 枚举类的设计和使用
 * 2. 状态检查的重要性
 * 3. 用户体验的优化策略
 * 4. 错误处理和用户引导
 */
enum class WeChatStatus(
    val code: String,
    val displayName: String,
    val description: String,
    val canUseAppLogin: Boolean,
    val canUseQRLogin: Boolean,
    val userAction: String?
) {
    
    /**
     * 微信客户端正常可用
     * 
     * 状态说明：
     * - 微信客户端已安装
     * - 版本支持登录功能
     * - 可以正常调起微信进行授权
     * 
     * 推荐操作：
     * - 优先使用应用内登录
     * - 也可以选择二维码登录
     */
    AVAILABLE(
        code = "AVAILABLE",
        displayName = "微信可用",
        description = "微信客户端正常，支持应用内登录",
        canUseAppLogin = true,
        canUseQRLogin = true,
        userAction = null
    ),
    
    /**
     * 微信客户端未安装
     * 
     * 状态说明：
     * - 设备上没有安装微信客户端
     * - 无法使用应用内登录功能
     * - 可以使用二维码登录作为替代方案
     * 
     * 推荐操作：
     * - 引导用户安装微信客户端
     * - 提供二维码登录选项
     * - 提供其他登录方式（如游客登录）
     */
    NOT_INSTALLED(
        code = "NOT_INSTALLED",
        displayName = "微信未安装",
        description = "设备上未安装微信客户端",
        canUseAppLogin = false,
        canUseQRLogin = true,
        userAction = "请安装微信客户端，或使用二维码登录"
    ),
    
    /**
     * 微信客户端版本过低
     * 
     * 状态说明：
     * - 微信客户端已安装但版本过低
     * - 不支持当前的登录API
     * - 可能存在兼容性问题
     * 
     * 推荐操作：
     * - 引导用户更新微信客户端
     * - 提供二维码登录作为替代方案
     * - 显示最低版本要求信息
     */
    VERSION_TOO_LOW(
        code = "VERSION_TOO_LOW",
        displayName = "微信版本过低",
        description = "微信客户端版本过低，不支持登录功能",
        canUseAppLogin = false,
        canUseQRLogin = true,
        userAction = "请更新微信到最新版本，或使用二维码登录"
    ),
    
    /**
     * 微信客户端不支持登录
     * 
     * 状态说明：
     * - 微信客户端存在但不支持第三方登录
     * - 可能是特殊版本或定制版本
     * - API调用返回不支持的错误
     * 
     * 推荐操作：
     * - 使用二维码登录
     * - 检查微信客户端是否为官方版本
     * - 提供其他登录方式
     */
    NOT_SUPPORTED(
        code = "NOT_SUPPORTED",
        displayName = "不支持登录",
        description = "微信客户端不支持第三方应用登录",
        canUseAppLogin = false,
        canUseQRLogin = true,
        userAction = "请使用二维码登录或其他登录方式"
    ),
    
    /**
     * 微信客户端状态未知
     * 
     * 状态说明：
     * - 无法确定微信客户端的具体状态
     * - 可能是检查过程中出现异常
     * - 系统权限限制或其他技术问题
     * 
     * 推荐操作：
     * - 尝试应用内登录（可能成功）
     * - 准备二维码登录作为备选
     * - 记录错误信息用于问题排查
     */
    UNKNOWN(
        code = "UNKNOWN",
        displayName = "状态未知",
        description = "无法确定微信客户端状态",
        canUseAppLogin = true,  // 允许尝试
        canUseQRLogin = true,
        userAction = "建议尝试登录，如失败请使用二维码登录"
    );
    
    /**
     * 获取用户友好的状态描述
     * 
     * @param includeAction 是否包含用户操作建议
     * @return 状态描述文本
     */
    fun getDescription(includeAction: Boolean = false): String {
        return if (includeAction && userAction != null) {
            "$description\n$userAction"
        } else {
            description
        }
    }
    
    /**
     * 检查是否可以使用应用内登录
     * 
     * @return 是否可以使用应用内登录
     */
    fun canUseAppLogin(): Boolean = canUseAppLogin
    
    /**
     * 检查是否可以使用二维码登录
     * 
     * @return 是否可以使用二维码登录
     */
    fun canUseQRLogin(): Boolean = canUseQRLogin
    
    /**
     * 检查是否需要用户操作
     * 
     * @return 是否需要用户操作
     */
    fun needsUserAction(): Boolean = userAction != null
    
    /**
     * 获取推荐的登录方式
     * 
     * @return 推荐的登录方式列表
     */
    fun getRecommendedLoginMethods(): List<String> {
        val methods = mutableListOf<String>()
        
        when (this) {
            AVAILABLE -> {
                methods.add("微信应用内登录")
                methods.add("微信二维码登录")
            }
            NOT_INSTALLED, VERSION_TOO_LOW, NOT_SUPPORTED -> {
                methods.add("微信二维码登录")
                methods.add("游客登录")
            }
            UNKNOWN -> {
                methods.add("微信应用内登录")
                methods.add("微信二维码登录")
                methods.add("游客登录")
            }
        }
        
        return methods
    }
    
    /**
     * 获取状态对应的图标资源名
     * 
     * @return 图标资源名
     */
    fun getIconResource(): String {
        return when (this) {
            AVAILABLE -> "ic_wechat_available"
            NOT_INSTALLED -> "ic_wechat_not_installed"
            VERSION_TOO_LOW -> "ic_wechat_update"
            NOT_SUPPORTED -> "ic_wechat_not_supported"
            UNKNOWN -> "ic_wechat_unknown"
        }
    }
    
    /**
     * 获取状态对应的颜色资源名
     * 
     * @return 颜色资源名
     */
    fun getColorResource(): String {
        return when (this) {
            AVAILABLE -> "color_success"
            NOT_INSTALLED, VERSION_TOO_LOW, NOT_SUPPORTED -> "color_warning"
            UNKNOWN -> "color_info"
        }
    }
    
    companion object {
        
        /**
         * 根据错误码获取对应的状态
         * 
         * @param errorCode 微信SDK返回的错误码
         * @return 对应的微信状态
         */
        fun fromErrorCode(errorCode: Int): WeChatStatus {
            return when (errorCode) {
                -1 -> NOT_INSTALLED      // 微信未安装
                -2 -> VERSION_TOO_LOW    // 版本过低
                -3 -> NOT_SUPPORTED      // 不支持
                0 -> AVAILABLE           // 正常
                else -> UNKNOWN          // 未知错误
            }
        }
        
        /**
         * 根据异常获取对应的状态
         * 
         * @param exception 检查过程中的异常
         * @return 对应的微信状态
         */
        fun fromException(exception: Throwable): WeChatStatus {
            return when {
                exception.message?.contains("not installed", ignoreCase = true) == true -> NOT_INSTALLED
                exception.message?.contains("version", ignoreCase = true) == true -> VERSION_TOO_LOW
                exception.message?.contains("not supported", ignoreCase = true) == true -> NOT_SUPPORTED
                else -> UNKNOWN
            }
        }
    }
}
