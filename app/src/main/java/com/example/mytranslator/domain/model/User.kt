package com.example.mytranslator.domain.model

/**
 * 用户领域模型
 *
 * 🎯 设计目的：
 * 1. 表示应用中的用户实体
 * 2. 封装用户的基本信息和状态
 * 3. 支持多种登录方式（微信、QQ等）
 * 4. 提供用户权限和偏好设置
 *
 * 🏗️ 设计原则：
 * - 不可变性：使用data class确保数据不可变
 * - 类型安全：使用枚举和密封类确保类型安全
 * - 扩展性：预留扩展字段支持未来功能
 * - 隐私保护：敏感信息的处理和保护
 *
 * 📱 使用场景：
 * - 用户登录和认证
 * - 个人信息展示
 * - 权限控制和功能访问
 * - 数据同步和备份
 *
 * 🎓 学习要点：
 * 领域模型的设计考虑：
 * 1. 业务完整性 - 包含完整的业务属性
 * 2. 数据一致性 - 确保数据的逻辑一致性
 * 3. 扩展性 - 支持未来的业务扩展
 * 4. 安全性 - 敏感数据的保护机制
 */
data class User(
    /**
     * 用户唯一标识符
     * 系统内部生成的UUID，确保全局唯一性
     */
    val id: String,
    
    /**
     * 用户昵称
     * 用户设置的显示名称，可以修改
     */
    val nickname: String,
    
    /**
     * 用户头像URL
     * 头像图片的网络地址或本地路径
     */
    val avatarUrl: String?,
    
    /**
     * 登录方式
     * 用户使用的登录方式（微信、QQ、游客等）
     */
    val loginType: LoginType,
    
    /**
     * 第三方平台用户ID
     * 在第三方平台（如微信）的用户标识
     */
    val thirdPartyId: String?,
    
    /**
     * 用户邮箱
     * 用于通知和账号恢复
     */
    val email: String?,
    
    /**
     * 手机号码
     * 用于验证和通知
     */
    val phoneNumber: String?,
    
    /**
     * 用户状态
     * 账号的当前状态（正常、冻结、注销等）
     */
    val status: UserStatus,
    
    /**
     * 注册时间
     * 用户首次注册的时间戳
     */
    val registrationTime: Long,
    
    /**
     * 最后登录时间
     * 用户最近一次登录的时间戳
     */
    val lastLoginTime: Long,
    
    /**
     * 用户偏好设置
     * 用户的个性化设置和偏好
     */
    val preferences: UserPreferences,
    
    /**
     * 会员信息
     * 用户的会员等级和权限信息
     */
    val membershipInfo: MembershipInfo,
    
    /**
     * 扩展属性
     * 用于存储额外的用户属性，支持未来扩展
     */
    val extraAttributes: Map<String, String> = emptyMap()
) {
    
    /**
     * 检查用户是否为VIP会员
     */
    fun isVipMember(): Boolean = membershipInfo.isVip
    
    /**
     * 检查用户是否已完成实名认证
     */
    fun isVerified(): Boolean = !phoneNumber.isNullOrBlank() || !email.isNullOrBlank()
    
    /**
     * 获取用户显示名称
     * 优先使用昵称，如果没有则使用默认名称
     */
    fun getDisplayName(): String = nickname.ifBlank { "用户${id.take(8)}" }
    
    /**
     * 检查用户是否为新用户
     * 注册时间在7天内的用户被认为是新用户
     */
    fun isNewUser(): Boolean {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - registrationTime < sevenDaysInMillis
    }
    
    /**
     * 获取用户等级描述
     */
    fun getLevelDescription(): String = membershipInfo.level.description
}

/**
 * 登录方式枚举
 */
enum class LoginType(val displayName: String, val code: String) {
    WECHAT("微信登录", "wechat"),
    QQ("QQ登录", "qq"),
    WEIBO("微博登录", "weibo"),
    GUEST("游客模式", "guest"),
    EMAIL("邮箱登录", "email"),
    PHONE("手机登录", "phone")
}

/**
 * 用户状态枚举
 */
enum class UserStatus(val description: String) {
    ACTIVE("正常"),
    INACTIVE("未激活"),
    SUSPENDED("已冻结"),
    DELETED("已注销")
}

/**
 * 用户偏好设置
 */
data class UserPreferences(
    /**
     * 默认源语言
     */
    val defaultSourceLanguage: String = "auto",
    
    /**
     * 默认目标语言
     */
    val defaultTargetLanguage: String = "zh",
    
    /**
     * 是否启用翻译历史记录
     */
    val enableTranslationHistory: Boolean = true,
    
    /**
     * 是否启用自动保存
     */
    val enableAutoSave: Boolean = true,
    
    /**
     * 主题设置
     */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    
    /**
     * 语言设置
     */
    val appLanguage: String = "zh",
    
    /**
     * 是否启用推送通知
     */
    val enableNotifications: Boolean = true,
    
    /**
     * 是否启用声音
     */
    val enableSound: Boolean = true,
    
    /**
     * 是否启用震动
     */
    val enableVibration: Boolean = true,
    
    /**
     * 翻译质量偏好
     */
    val translationQuality: TranslationQuality = TranslationQuality.BALANCED
)

/**
 * 主题模式枚举
 */
enum class ThemeMode(val description: String) {
    LIGHT("浅色模式"),
    DARK("深色模式"),
    SYSTEM("跟随系统")
}

/**
 * 翻译质量偏好
 */
enum class TranslationQuality(val description: String) {
    FAST("快速翻译"),
    BALANCED("平衡模式"),
    ACCURATE("精确翻译")
}

/**
 * 会员信息
 */
data class MembershipInfo(
    /**
     * 会员等级
     */
    val level: MemberLevel = MemberLevel.FREE,
    
    /**
     * 是否为VIP
     */
    val isVip: Boolean = false,
    
    /**
     * VIP到期时间
     */
    val vipExpireTime: Long? = null,
    
    /**
     * 今日翻译次数
     */
    val dailyTranslationCount: Int = 0,
    
    /**
     * 今日翻译限制
     */
    val dailyTranslationLimit: Int = level.dailyLimit,
    
    /**
     * 总翻译次数
     */
    val totalTranslationCount: Long = 0
) {
    
    /**
     * 检查今日是否还能翻译
     */
    fun canTranslateToday(): Boolean = dailyTranslationCount < dailyTranslationLimit
    
    /**
     * 获取今日剩余翻译次数
     */
    fun getRemainingTranslations(): Int = (dailyTranslationLimit - dailyTranslationCount).coerceAtLeast(0)
    
    /**
     * 检查VIP是否有效
     */
    fun isVipValid(): Boolean = isVip && (vipExpireTime?.let { it > System.currentTimeMillis() } ?: false)
}

/**
 * 会员等级枚举
 */
enum class MemberLevel(
    val description: String,
    val dailyLimit: Int,
    val priority: Int
) {
    FREE("免费用户", 100, 1),
    BRONZE("青铜会员", 500, 2),
    SILVER("白银会员", 1000, 3),
    GOLD("黄金会员", 2000, 4),
    PLATINUM("铂金会员", 5000, 5),
    DIAMOND("钻石会员", Int.MAX_VALUE, 6)
}
