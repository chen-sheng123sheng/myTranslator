package com.example.mytranslator.domain.repository

import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.model.TranslationInput
import com.example.mytranslator.domain.model.TranslationResult

/**
 * 翻译仓库接口
 *
 * 🎯 设计思想：
 * 1. Repository模式 - 数据访问层的抽象接口
 * 2. 依赖倒置原则 - 高层模块不依赖低层模块，都依赖抽象
 * 3. 关注点分离 - 业务逻辑与数据访问逻辑分离
 * 4. 可测试性 - 便于创建Mock实现进行单元测试
 *
 * 🔧 技术特性：
 * - 使用suspend函数支持协程异步操作
 * - 使用Result类型封装成功/失败状态
 * - 支持多种翻译输入类型（文本/语音/图片）
 * - 提供缓存和历史记录管理
 *
 * 📱 使用场景：
 * - UseCase层调用进行翻译操作
 * - ViewModel层获取翻译历史
 * - 缓存管理和离线支持
 * - 多翻译服务的统一接口
 *
 * 🎓 学习要点：
 * Repository模式的核心价值：
 * 1. 抽象化 - UI层不需要知道数据来自网络还是本地
 * 2. 可替换 - 可以轻松切换不同的翻译服务
 * 3. 可测试 - 可以创建Mock实现进行单元测试
 * 4. 缓存策略 - 统一管理数据的缓存和同步
 */
interface TranslationRepository {

    /**
     * 执行翻译操作
     *
     * 🎯 设计考虑：
     * - 支持多种输入类型的统一翻译接口
     * - 使用Result封装结果，便于错误处理
     * - 异步操作，不阻塞UI线程
     * - 自动处理缓存查找和保存
     *
     * @param input 翻译输入（文本/语音/图片）
     * @param sourceLanguage 源语言（可以是自动检测）
     * @param targetLanguage 目标语言
     * @return 翻译结果，包装在Result中
     */
    suspend fun translate(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult>

    /**
     * 检测输入内容的语言
     *
     * 🎯 设计考虑：
     * - 当用户选择"自动检测"时调用
     * - 支持不同类型输入的语言检测
     * - 为翻译操作提供准确的源语言
     *
     * @param input 需要检测语言的输入
     * @return 检测到的语言，包装在Result中
     */
    suspend fun detectLanguage(input: TranslationInput): Result<Language>

    /**
     * 获取翻译历史记录
     *
     * 🎯 设计考虑：
     * - 支持分页加载，避免一次加载过多数据
     * - 按时间倒序排列，最新的在前面
     * - 支持不同输入类型的历史记录
     *
     * @param limit 限制返回的记录数量
     * @param offset 偏移量，用于分页
     * @return 翻译历史记录列表
     */
    suspend fun getTranslationHistory(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<TranslationResult>>

    /**
     * 保存翻译结果到历史记录
     *
     * 🎯 设计考虑：
     * - 自动在翻译完成后调用
     * - 支持历史记录的持久化存储
     * - 避免重复保存相同的翻译
     *
     * @param result 要保存的翻译结果
     * @return 保存操作的结果
     */
    suspend fun saveTranslationToHistory(result: TranslationResult): Result<Unit>

    /**
     * 删除翻译历史记录
     *
     * 🎯 设计考虑：
     * - 支持单个删除和批量删除
     * - 提供用户管理历史记录的能力
     * - 释放存储空间
     *
     * @param results 要删除的翻译结果列表
     * @return 删除操作的结果
     */
    suspend fun deleteTranslationHistory(results: List<TranslationResult>): Result<Unit>

    /**
     * 清空所有翻译历史记录
     *
     * 🎯 设计考虑：
     * - 提供一键清空功能
     * - 用于隐私保护和存储清理
     *
     * @return 清空操作的结果
     */
    suspend fun clearTranslationHistory(): Result<Unit>

    /**
     * 搜索翻译历史记录
     *
     * 🎯 设计考虑：
     * - 支持在历史记录中搜索特定内容
     * - 提高用户查找历史翻译的效率
     * - 支持模糊匹配和多语言搜索
     *
     * @param query 搜索关键词
     * @param limit 限制返回的记录数量
     * @return 匹配的翻译历史记录
     */
    suspend fun searchTranslationHistory(
        query: String,
        limit: Int = 20
    ): Result<List<TranslationResult>>

    /**
     * 获取缓存的翻译结果
     *
     * 🎯 设计考虑：
     * - 避免重复翻译相同内容
     * - 提高响应速度和节省API调用
     * - 支持离线查看最近的翻译
     *
     * @param input 翻译输入
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 缓存的翻译结果（如果存在）
     */
    suspend fun getCachedTranslation(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult?>

    /**
     * 获取翻译统计信息
     *
     * 🎯 设计考虑：
     * - 为用户提供使用统计
     * - 支持功能改进和用户行为分析
     * - 展示翻译服务的使用情况
     *
     * @return 翻译统计信息
     */
    suspend fun getTranslationStatistics(): Result<TranslationStatistics>

    /**
     * 设置翻译偏好配置
     *
     * 🎯 设计考虑：
     * - 支持用户自定义翻译行为
     * - 保存翻译服务偏好、质量设置等
     * - 提供个性化的翻译体验
     *
     * @param preferences 翻译偏好配置
     * @return 设置操作的结果
     */
    suspend fun setTranslationPreferences(preferences: TranslationPreferences): Result<Unit>

    /**
     * 获取翻译偏好配置
     *
     * @return 当前的翻译偏好配置
     */
    suspend fun getTranslationPreferences(): Result<TranslationPreferences>

    /**
     * 🎓 学习要点：数据类定义
     * 
     * 为什么在Repository接口中定义这些数据类？
     * 1. 保持接口的完整性和自包含性
     * 2. 避免外部依赖，提高接口的独立性
     * 3. 便于接口的理解和使用
     */

    /**
     * 翻译统计信息
     */
    data class TranslationStatistics(
        val totalTranslations: Int,           // 总翻译次数
        val todayTranslations: Int,           // 今日翻译次数
        val favoriteSourceLanguage: Language?, // 最常用源语言
        val favoriteTargetLanguage: Language?, // 最常用目标语言
        val averageTranslationTime: Long,     // 平均翻译时间（毫秒）
        val mostUsedInputType: String,        // 最常用输入类型
        val totalCharactersTranslated: Long   // 总翻译字符数
    )

    /**
     * 翻译偏好配置
     */
    data class TranslationPreferences(
        val defaultSourceLanguage: Language,     // 默认源语言
        val defaultTargetLanguage: Language,     // 默认目标语言
        val autoSaveToHistory: Boolean = true,   // 自动保存到历史
        val enableCache: Boolean = true,         // 启用缓存
        val cacheExpirationHours: Int = 24,      // 缓存过期时间（小时）
        val preferredTranslationProvider: String? = null, // 首选翻译服务
        val enableOfflineMode: Boolean = false,  // 启用离线模式
        val maxHistorySize: Int = 1000           // 最大历史记录数
    )

    /**
     * 🎓 学习要点：伴生对象的使用
     * 
     * 为接口提供常量和工厂方法
     */
    companion object {
        /** 默认历史记录限制 */
        const val DEFAULT_HISTORY_LIMIT = 50
        
        /** 默认搜索结果限制 */
        const val DEFAULT_SEARCH_LIMIT = 20
        
        /** 缓存键前缀 */
        const val CACHE_KEY_PREFIX = "translation_cache_"
        
        /** 历史记录表名 */
        const val HISTORY_TABLE_NAME = "translation_history"
    }
}
