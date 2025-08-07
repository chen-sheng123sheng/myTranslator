package com.example.mytranslator.domain.repository

import com.example.mytranslator.domain.model.Language

/**
 * 语言仓库接口
 *
 * 🎯 设计思想：
 * 1. 单一职责原则 - 专门负责语言相关的数据操作
 * 2. 数据源抽象 - 语言数据可能来自本地、网络或混合源
 * 3. 用户偏好管理 - 记住用户的语言选择和使用习惯
 * 4. 国际化支持 - 为多语言界面提供数据基础
 *
 * 🔧 技术特性：
 * - 支持静态语言列表和动态语言更新
 * - 用户偏好的持久化存储
 * - 语言使用统计和推荐
 * - 离线语言包管理（预留）
 *
 * 📱 使用场景：
 * - 语言选择对话框的数据源
 * - 用户偏好设置的读取和保存
 * - 翻译界面的默认语言设置
 * - 语言使用统计和推荐
 *
 * 🎓 学习要点：
 * 为什么需要单独的LanguageRepository？
 * 1. 关注点分离 - 语言管理与翻译操作是不同的业务领域
 * 2. 数据来源不同 - 语言列表相对稳定，翻译结果动态变化
 * 3. 缓存策略不同 - 语言数据可以长期缓存，翻译结果需要及时更新
 * 4. 用户偏好管理 - 语言选择是用户的个人偏好，需要特殊处理
 */
interface LanguageRepository {

    /**
     * 获取所有支持的语言列表
     *
     * 🎯 设计考虑：
     * - 支持本地预定义和远程动态更新
     * - 按用户偏好和使用频率排序
     * - 支持语言包的可用性检查
     *
     * @param includeAutoDetect 是否包含"自动检测"选项
     * @param sortByUsage 是否按使用频率排序
     * @return 支持的语言列表
     */
    suspend fun getSupportedLanguages(
        includeAutoDetect: Boolean = true,
        sortByUsage: Boolean = false
    ): Result<List<Language>>

    /**
     * 获取常用语言列表
     *
     * 🎯 设计考虑：
     * - 基于用户使用历史推荐
     * - 为快速选择提供便利
     * - 支持个性化推荐
     *
     * @param limit 返回的语言数量限制
     * @return 常用语言列表
     */
    suspend fun getFrequentlyUsedLanguages(limit: Int = 5): Result<List<Language>>

    /**
     * 搜索语言
     *
     * 🎯 设计考虑：
     * - 支持多种搜索方式：代码、英文名、本地名
     * - 模糊匹配和智能推荐
     * - 为语言选择对话框提供搜索功能
     *
     * @param query 搜索关键词
     * @param limit 返回结果数量限制
     * @return 匹配的语言列表
     */
    suspend fun searchLanguages(
        query: String,
        limit: Int = 10
    ): Result<List<Language>>

    /**
     * 根据语言代码获取语言信息
     *
     * 🎯 设计考虑：
     * - 从API响应或存储中恢复Language对象
     * - 支持大小写不敏感查找
     * - 处理未知语言代码的情况
     *
     * @param code 语言代码（如"en", "zh"）
     * @return 对应的Language对象
     */
    suspend fun getLanguageByCode(code: String): Result<Language?>

    /**
     * 获取用户的默认源语言
     *
     * 🎯 设计考虑：
     * - 基于用户偏好设置
     * - 如果未设置，返回系统推荐
     * - 支持"自动检测"作为默认选项
     *
     * @return 默认源语言
     */
    suspend fun getDefaultSourceLanguage(): Result<Language>

    /**
     * 获取用户的默认目标语言
     *
     * 🎯 设计考虑：
     * - 基于用户偏好和使用习惯
     * - 考虑用户的地理位置和系统语言
     * - 提供智能推荐
     *
     * @return 默认目标语言
     */
    suspend fun getDefaultTargetLanguage(): Result<Language>

    /**
     * 设置用户的默认源语言
     *
     * 🎯 设计考虑：
     * - 持久化保存用户偏好
     * - 影响后续翻译的默认设置
     * - 更新语言使用统计
     *
     * @param language 要设置的默认源语言
     * @return 设置操作的结果
     */
    suspend fun setDefaultSourceLanguage(language: Language): Result<Unit>

    /**
     * 设置用户的默认目标语言
     *
     * @param language 要设置的默认目标语言
     * @return 设置操作的结果
     */
    suspend fun setDefaultTargetLanguage(language: Language): Result<Unit>

    /**
     * 记录语言使用情况
     *
     * 🎯 设计考虑：
     * - 统计用户的语言使用偏好
     * - 为推荐算法提供数据
     * - 优化语言列表的排序
     *
     * @param sourceLanguage 使用的源语言
     * @param targetLanguage 使用的目标语言
     * @return 记录操作的结果
     */
    suspend fun recordLanguageUsage(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Unit>

    /**
     * 获取语言使用统计
     *
     * 🎯 设计考虑：
     * - 为用户展示使用习惯
     * - 支持数据分析和功能改进
     * - 提供个性化推荐依据
     *
     * @return 语言使用统计信息
     */
    suspend fun getLanguageUsageStatistics(): Result<LanguageUsageStatistics>

    /**
     * 获取推荐的语言对
     *
     * 🎯 设计考虑：
     * - 基于用户历史和全局统计
     * - 为快速翻译提供便利
     * - 支持智能推荐算法
     *
     * @param limit 返回的语言对数量
     * @return 推荐的语言对列表
     */
    suspend fun getRecommendedLanguagePairs(limit: Int = 3): Result<List<LanguagePair>>

    /**
     * 检查语言是否支持特定功能
     *
     * 🎯 设计考虑：
     * - 不同语言可能支持不同功能（如语音合成）
     * - 为UI提供功能可用性信息
     * - 支持渐进式功能开放
     *
     * @param language 要检查的语言
     * @param feature 要检查的功能
     * @return 是否支持该功能
     */
    suspend fun isLanguageFeatureSupported(
        language: Language,
        feature: LanguageFeature
    ): Result<Boolean>

    /**
     * 更新语言数据
     *
     * 🎯 设计考虑：
     * - 支持从服务器更新语言列表
     * - 添加新支持的语言
     * - 更新语言的显示名称和功能支持
     *
     * @param forceUpdate 是否强制更新
     * @return 更新操作的结果
     */
    suspend fun updateLanguageData(forceUpdate: Boolean = false): Result<Unit>

    /**
     * 🎓 学习要点：枚举类的使用
     * 
     * 为什么使用enum class定义语言功能？
     * 1. 类型安全 - 避免字符串魔法值
     * 2. 可扩展 - 容易添加新功能类型
     * 3. 可读性 - 代码更清晰易懂
     */
    enum class LanguageFeature {
        TEXT_TRANSLATION,      // 文本翻译
        VOICE_RECOGNITION,     // 语音识别
        VOICE_SYNTHESIS,       // 语音合成
        OCR_RECOGNITION,       // OCR文字识别
        OFFLINE_TRANSLATION    // 离线翻译
    }

    /**
     * 语言对数据类
     */
    data class LanguagePair(
        val sourceLanguage: Language,
        val targetLanguage: Language,
        val usageCount: Int = 0,
        val lastUsedTime: Long = 0L
    ) {
        /**
         * 获取语言对的显示文本
         */
        fun getDisplayText(): String {
            return "${sourceLanguage.displayName} → ${targetLanguage.displayName}"
        }

        /**
         * 检查是否为最近使用
         */
        fun isRecentlyUsed(thresholdHours: Int = 24): Boolean {
            val now = System.currentTimeMillis()
            val threshold = thresholdHours * 60 * 60 * 1000L
            return (now - lastUsedTime) < threshold
        }
    }

    /**
     * 语言使用统计信息
     */
    data class LanguageUsageStatistics(
        val totalTranslations: Int,                    // 总翻译次数
        val mostUsedSourceLanguage: Language?,         // 最常用源语言
        val mostUsedTargetLanguage: Language?,         // 最常用目标语言
        val languageUsageMap: Map<Language, Int>,      // 各语言使用次数
        val languagePairUsage: Map<LanguagePair, Int>, // 语言对使用次数
        val lastUpdateTime: Long                       // 最后更新时间
    ) {
        /**
         * 获取语言使用排行榜
         */
        fun getLanguageRanking(limit: Int = 5): List<Pair<Language, Int>> {
            return languageUsageMap.toList()
                .sortedByDescending { it.second }
                .take(limit)
        }

        /**
         * 获取最受欢迎的语言对
         */
        fun getPopularLanguagePairs(limit: Int = 3): List<LanguagePair> {
            return languagePairUsage.toList()
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
        }
    }

    /**
     * 🎓 学习要点：伴生对象的常量定义
     */
    companion object {
        /** 默认常用语言数量 */
        const val DEFAULT_FREQUENT_LANGUAGES_LIMIT = 5
        
        /** 默认搜索结果数量 */
        const val DEFAULT_SEARCH_LIMIT = 10
        
        /** 默认推荐语言对数量 */
        const val DEFAULT_RECOMMENDED_PAIRS_LIMIT = 3
        
        /** 语言数据缓存键 */
        const val LANGUAGE_DATA_CACHE_KEY = "supported_languages"
        
        /** 用户偏好存储键前缀 */
        const val USER_PREFERENCE_KEY_PREFIX = "language_pref_"
    }
}
