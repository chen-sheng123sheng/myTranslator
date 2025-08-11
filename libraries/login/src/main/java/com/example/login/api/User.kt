package com.example.login.api

import com.google.gson.annotations.SerializedName

/**
 * 用户信息数据类
 * 
 * 🎯 设计目的：
 * 1. 统一的用户信息数据结构
 * 2. 支持多种登录方式的用户信息
 * 3. 便于序列化和本地存储
 * 4. 提供用户信息的扩展性
 * 
 * 🏗️ 设计模式：
 * - 数据类模式：不可变的数据容器
 * - 建造者模式：灵活的对象构建
 * - 适配器模式：适配不同平台的用户信息格式
 * 
 * 📱 使用场景：
 * - 用户登录成功后的信息存储
 * - 个人中心页面的信息显示
 * - 用户权限和状态判断
 * - 用户信息的网络传输
 * 
 * 🎓 学习要点：
 * 1. 数据类的设计原则
 * 2. JSON序列化的处理
 * 3. 可选字段的设计
 * 4. 数据验证和默认值
 */
data class User(
    /**
     * 用户唯一标识符
     * 
     * 对于不同登录方式：
     * - 微信登录：使用微信的openid或unionid
     * - 游客登录：使用本地生成的UUID
     * - 其他登录：使用对应平台的用户ID
     */
    @SerializedName("id")
    val id: String,
    
    /**
     * 用户昵称
     * 
     * 显示规则：
     * - 微信登录：使用微信昵称
     * - 游客登录：使用默认昵称"游客用户"
     * - 可以为空，使用默认显示逻辑
     */
    @SerializedName("nickname")
    val nickname: String?,
    
    /**
     * 用户头像URL
     * 
     * 头像来源：
     * - 微信登录：微信头像URL
     * - 游客登录：null，使用默认头像
     * - 支持本地头像路径和网络URL
     */
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    
    /**
     * 登录方式类型
     * 
     * 用于区分用户的登录来源：
     * - WECHAT_APP：微信应用内登录
     * - WECHAT_QR：微信二维码登录
     * - GUEST：游客登录
     */
    @SerializedName("login_type")
    val loginType: LoginType,
    
    /**
     * 第三方平台用户ID
     * 
     * 存储原始的第三方用户标识：
     * - 微信：openid或unionid
     * - 其他平台：对应的用户ID
     * - 游客登录：null
     */
    @SerializedName("third_party_id")
    val thirdPartyId: String?,
    
    /**
     * 用户邮箱
     * 
     * 可选信息，部分平台可能提供：
     * - 微信：通常不提供邮箱信息
     * - 其他平台：可能提供邮箱
     * - 用户手动填写：后续完善资料时添加
     */
    @SerializedName("email")
    val email: String?,
    
    /**
     * 用户手机号
     * 
     * 可选信息，需要特殊权限：
     * - 微信：需要特殊权限才能获取
     * - 其他平台：根据平台政策
     * - 用户手动填写：后续完善资料时添加
     */
    @SerializedName("phone_number")
    val phoneNumber: String?,
    
    /**
     * 用户注册时间
     * 
     * 时间戳格式（毫秒）：
     * - 首次登录时记录
     * - 用于统计和分析
     */
    @SerializedName("registration_time")
    val registrationTime: Long,
    
    /**
     * 最后登录时间
     * 
     * 时间戳格式（毫秒）：
     * - 每次登录时更新
     * - 用于活跃度统计
     */
    @SerializedName("last_login_time")
    val lastLoginTime: Long,
    
    /**
     * 用户性别
     * 
     * 可选信息：
     * - 微信：可能提供性别信息
     * - 其他平台：根据平台政策
     * - 隐私保护：用户可选择不提供
     */
    @SerializedName("gender")
    val gender: Gender? = null,
    
    /**
     * 用户所在地区
     * 
     * 可选信息：
     * - 微信：可能提供地区信息
     * - 格式：国家-省份-城市
     * - 隐私保护：用户可选择不提供
     */
    @SerializedName("location")
    val location: String? = null,
    
    /**
     * 用户状态
     * 
     * 账户状态管理：
     * - ACTIVE：正常状态
     * - SUSPENDED：暂停状态
     * - BANNED：封禁状态
     */
    @SerializedName("status")
    val status: UserStatus = UserStatus.ACTIVE,
    
    /**
     * 扩展属性
     * 
     * 用于存储额外的用户信息：
     * - 平台特有的属性
     * - 应用自定义的属性
     * - 临时性的标记信息
     */
    @SerializedName("extras")
    val extras: Map<String, Any> = emptyMap()
) {
    
    /**
     * 获取显示用的用户名
     * 
     * 显示优先级：
     * 1. 用户昵称（如果不为空）
     * 2. 根据登录类型的默认名称
     * 3. 用户ID的简化显示
     * 
     * @return 用于显示的用户名
     */
    fun getDisplayName(): String {
        return when {
            !nickname.isNullOrBlank() -> nickname
            loginType == LoginType.GUEST -> "游客用户"
            loginType == LoginType.WECHAT_APP || loginType == LoginType.WECHAT_QR -> "微信用户"
            else -> "用户${id.takeLast(4)}"
        }
    }
    
    /**
     * 获取显示用的头像URL
     * 
     * 头像优先级：
     * 1. 用户设置的头像URL
     * 2. 默认头像（根据登录类型）
     * 
     * @return 用于显示的头像URL
     */
    fun getDisplayAvatarUrl(): String? {
        return avatarUrl ?: getDefaultAvatarUrl()
    }
    
    /**
     * 检查是否为游客用户
     * 
     * @return 是否为游客用户
     */
    fun isGuest(): Boolean {
        return loginType == LoginType.GUEST
    }
    
    /**
     * 检查是否为微信用户
     * 
     * @return 是否为微信用户
     */
    fun isWeChatUser(): Boolean {
        return loginType == LoginType.WECHAT_APP || loginType == LoginType.WECHAT_QR
    }
    
    /**
     * 检查用户状态是否正常
     * 
     * @return 用户状态是否正常
     */
    fun isActive(): Boolean {
        return status == UserStatus.ACTIVE
    }
    
    /**
     * 获取用户信息的摘要
     * 
     * 用于日志记录和调试：
     * - 不包含敏感信息
     * - 便于问题排查
     * 
     * @return 用户信息摘要
     */
    fun getSummary(): String {
        return "User(id=${id.takeLast(4)}, name=${getDisplayName()}, type=$loginType, status=$status)"
    }
    
    /**
     * 复制用户信息并更新最后登录时间
     * 
     * @param newLoginTime 新的登录时间，默认为当前时间
     * @return 更新后的用户信息
     */
    fun updateLastLoginTime(newLoginTime: Long = System.currentTimeMillis()): User {
        return copy(lastLoginTime = newLoginTime)
    }
    
    /**
     * 复制用户信息并更新状态
     * 
     * @param newStatus 新的用户状态
     * @return 更新后的用户信息
     */
    fun updateStatus(newStatus: UserStatus): User {
        return copy(status = newStatus)
    }
    
    // ===== 私有方法 =====
    
    /**
     * 获取默认头像URL
     */
    private fun getDefaultAvatarUrl(): String? {
        return when (loginType) {
            LoginType.GUEST -> null // 使用应用默认头像
            LoginType.WECHAT_APP, LoginType.WECHAT_QR -> null // 使用微信默认头像
            else -> null
        }
    }
}

/**
 * 登录类型枚举
 */
enum class LoginType(val displayName: String) {
    @SerializedName("wechat_app")
    WECHAT_APP("微信应用内登录"),
    
    @SerializedName("wechat_qr")
    WECHAT_QR("微信二维码登录"),
    
    @SerializedName("guest")
    GUEST("游客登录")
}

/**
 * 用户性别枚举
 */
enum class Gender(val displayName: String) {
    @SerializedName("male")
    MALE("男"),
    
    @SerializedName("female")
    FEMALE("女"),
    
    @SerializedName("unknown")
    UNKNOWN("未知")
}

/**
 * 用户状态枚举
 */
enum class UserStatus(val displayName: String) {
    @SerializedName("active")
    ACTIVE("正常"),
    
    @SerializedName("suspended")
    SUSPENDED("暂停"),
    
    @SerializedName("banned")
    BANNED("封禁")
}
