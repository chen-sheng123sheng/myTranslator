package com.example.mytranslator.domain.usecase

import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.repository.LanguageRepository

/**
 * 获取语言用例
 *
 * 🎯 设计思想：
 * 1. 专门负责语言获取和管理的业务逻辑
 * 2. 智能推荐算法：基于用户习惯和全局统计
 * 3. 用户体验优化：常用语言优先、搜索支持
 * 4. 缓存策略：平衡数据新鲜度和性能
 *
 * 🔧 技术特性：
 * - 多种获取策略：全部、常用、推荐、搜索
 * - 智能排序：使用频率、字母顺序、推荐权重
 * - 用户偏好管理：默认语言设置和记忆
 * - 功能检查：不同语言的功能支持情况
 *
 * 📱 使用场景：
 * - 语言选择对话框的数据源
 * - 翻译界面的默认语言设置
 * - 用户偏好设置页面
 * - 语言推荐和智能建议
 *
 * 🎓 学习要点：
 * UseCase的业务逻辑组织：
 * 1. 单一职责 - 只负责语言相关的业务逻辑
 * 2. 智能算法 - 基于数据的推荐和排序
 * 3. 用户体验 - 从业务层面优化用户交互
 * 4. 数据整合 - 整合多个数据源提供统一服务
 */
class GetLanguagesUseCase(
    private val languageRepository: LanguageRepository
) {

    /**
     * 获取所有支持的语言
     *
     * 🎯 设计考虑：
     * - 支持多种排序策略
     * - 可选包含自动检测选项
     * - 智能缓存和数据更新
     *
     * @param sortStrategy 排序策略
     * @param includeAutoDetect 是否包含自动检测
     * @return 语言列表
     */
    suspend fun getAllLanguages(
        sortStrategy: SortStrategy = SortStrategy.BY_USAGE,
        includeAutoDetect: Boolean = true
    ): Result<List<Language>> {
        return try {
            val languages = languageRepository.getSupportedLanguages(
                includeAutoDetect = includeAutoDetect,
                sortByUsage = sortStrategy == SortStrategy.BY_USAGE
            ).getOrThrow()

            val sortedLanguages = applySortStrategy(languages, sortStrategy)
            Result.success(sortedLanguages)

        } catch (e: Exception) {
            Result.failure(LanguageException("获取语言列表失败: ${e.message}", e))
        }
    }

    /**
     * 获取常用语言列表
     *
     * 🎯 设计考虑：
     * - 基于用户使用历史
     * - 为快速选择提供便利
     * - 支持个性化推荐
     *
     * @param limit 返回数量限制
     * @param includeDefaults 是否包含默认语言
     * @return 常用语言列表
     */
    suspend fun getFrequentLanguages(
        limit: Int = 5,
        includeDefaults: Boolean = true
    ): Result<List<Language>> {
        return try {
            val frequentLanguages = languageRepository.getFrequentlyUsedLanguages(limit)
                .getOrThrow()
                .toMutableList()

            // 如果常用语言不足，补充默认语言
            if (includeDefaults && frequentLanguages.size < limit) {
                val defaultLanguages = getDefaultLanguages().getOrThrow()
                defaultLanguages.forEach { defaultLang ->
                    if (!frequentLanguages.contains(defaultLang) && frequentLanguages.size < limit) {
                        frequentLanguages.add(defaultLang)
                    }
                }
            }

            Result.success(frequentLanguages.take(limit))

        } catch (e: Exception) {
            Result.failure(LanguageException("获取常用语言失败: ${e.message}", e))
        }
    }

    /**
     * 搜索语言
     *
     * 🎯 设计考虑：
     * - 支持多字段搜索：代码、英文名、本地名
     * - 智能匹配和模糊搜索
     * - 搜索结果按相关性排序
     *
     * @param query 搜索关键词
     * @param limit 结果数量限制
     * @return 匹配的语言列表
     */
    suspend fun searchLanguages(
        query: String,
        limit: Int = 10
    ): Result<List<Language>> {
        return try {
            if (query.isBlank()) {
                return getFrequentLanguages(limit)
            }

            val searchResults = languageRepository.searchLanguages(query.trim(), limit)
                .getOrThrow()

            // 按相关性排序
            val sortedResults = sortByRelevance(searchResults, query)
            Result.success(sortedResults)

        } catch (e: Exception) {
            Result.failure(LanguageException("搜索语言失败: ${e.message}", e))
        }
    }

    /**
     * 获取推荐的语言对
     *
     * 🎯 设计考虑：
     * - 基于用户历史和全局统计
     * - 为快速翻译提供便利
     * - 支持智能推荐算法
     *
     * @param limit 推荐数量
     * @return 推荐的语言对列表
     */
    suspend fun getRecommendedLanguagePairs(
        limit: Int = 3
    ): Result<List<LanguageRepository.LanguagePair>> {
        return try {
            val recommendedPairs = languageRepository.getRecommendedLanguagePairs(limit)
                .getOrThrow()

            // 如果推荐不足，补充默认语言对
            if (recommendedPairs.size < limit) {
                val defaultPairs = generateDefaultLanguagePairs()
                val allPairs = (recommendedPairs + defaultPairs).distinctBy { 
                    "${it.sourceLanguage.code}-${it.targetLanguage.code}" 
                }
                return Result.success(allPairs.take(limit))
            }

            Result.success(recommendedPairs)

        } catch (e: Exception) {
            Result.failure(LanguageException("获取推荐语言对失败: ${e.message}", e))
        }
    }

    /**
     * 获取用户的默认语言设置
     *
     * 🎯 设计考虑：
     * - 提供完整的默认语言配置
     * - 支持智能推荐和用户偏好
     * - 为新用户提供合理的默认值
     *
     * @return 默认语言配置
     */
    suspend fun getDefaultLanguageSettings(): Result<DefaultLanguageSettings> {
        return try {
            val sourceLanguage = languageRepository.getDefaultSourceLanguage().getOrThrow()
            val targetLanguage = languageRepository.getDefaultTargetLanguage().getOrThrow()
            val usageStats = languageRepository.getLanguageUsageStatistics().getOrNull()

            val settings = DefaultLanguageSettings(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isFirstTime = usageStats?.totalTranslations == 0,
                recommendedPairs = getRecommendedLanguagePairs().getOrNull() ?: emptyList()
            )

            Result.success(settings)

        } catch (e: Exception) {
            Result.failure(LanguageException("获取默认语言设置失败: ${e.message}", e))
        }
    }

    /**
     * 更新用户的默认语言设置
     *
     * 🎯 设计考虑：
     * - 同时更新源语言和目标语言
     * - 记录用户偏好变更
     * - 影响后续的推荐算法
     *
     * @param sourceLanguage 新的默认源语言
     * @param targetLanguage 新的默认目标语言
     * @return 更新结果
     */
    suspend fun updateDefaultLanguageSettings(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Unit> {
        return try {
            // 验证语言设置
            validateLanguageSettings(sourceLanguage, targetLanguage)

            // 更新源语言
            languageRepository.setDefaultSourceLanguage(sourceLanguage).getOrThrow()
            
            // 更新目标语言
            languageRepository.setDefaultTargetLanguage(targetLanguage).getOrThrow()

            // 记录语言使用（用于推荐算法）
            languageRepository.recordLanguageUsage(sourceLanguage, targetLanguage)
                .onFailure { 
                    // 记录失败不影响设置更新
                    println("记录语言使用失败: ${it.message}")
                }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(LanguageException("更新默认语言设置失败: ${e.message}", e))
        }
    }

    /**
     * 检查语言功能支持
     *
     * 🎯 设计考虑：
     * - 为UI提供功能可用性信息
     * - 支持渐进式功能开放
     * - 帮助用户了解语言能力
     *
     * @param language 要检查的语言
     * @param feature 要检查的功能
     * @return 是否支持该功能
     */
    suspend fun checkLanguageFeatureSupport(
        language: Language,
        feature: LanguageRepository.LanguageFeature
    ): Result<Boolean> {
        return languageRepository.isLanguageFeatureSupported(language, feature)
    }

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
    suspend fun getLanguageUsageStatistics(): Result<LanguageRepository.LanguageUsageStatistics> {
        return languageRepository.getLanguageUsageStatistics()
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 应用排序策略
     */
    private suspend fun applySortStrategy(languages: List<Language>, strategy: SortStrategy): List<Language> {
        return when (strategy) {
            SortStrategy.ALPHABETICAL -> languages.sortedBy { it.displayName }
            SortStrategy.BY_USAGE -> languages // Repository已经按使用频率排序
            SortStrategy.BY_CODE -> languages.sortedBy { it.code }
            SortStrategy.SMART -> applySmartSort(languages)
        }
    }

    /**
     * 智能排序：综合考虑使用频率、字母顺序等因素
     */
    private suspend fun applySmartSort(languages: List<Language>): List<Language> {
        val usageStats = languageRepository.getLanguageUsageStatistics().getOrNull()
        val usageMap = usageStats?.languageUsageMap ?: emptyMap()

        return languages.sortedWith { lang1, lang2 ->
            val usage1 = usageMap[lang1] ?: 0
            val usage2 = usageMap[lang2] ?: 0
            
            when {
                // 自动检测总是排在第一位
                lang1.isAutoDetect() -> -1
                lang2.isAutoDetect() -> 1
                // 使用频率高的排在前面
                usage1 != usage2 -> usage2.compareTo(usage1)
                // 使用频率相同时按字母顺序
                else -> lang1.displayName.compareTo(lang2.displayName)
            }
        }
    }

    /**
     * 按搜索相关性排序
     */
    private fun sortByRelevance(languages: List<Language>, query: String): List<Language> {
        val queryLower = query.lowercase()
        
        return languages.sortedWith { lang1, lang2 ->
            val score1 = calculateRelevanceScore(lang1, queryLower)
            val score2 = calculateRelevanceScore(lang2, queryLower)
            score2.compareTo(score1) // 分数高的排在前面
        }
    }

    /**
     * 计算搜索相关性分数
     */
    private fun calculateRelevanceScore(language: Language, query: String): Int {
        var score = 0
        
        // 完全匹配得分最高
        if (language.code.equals(query, ignoreCase = true)) score += 100
        if (language.name.equals(query, ignoreCase = true)) score += 90
        if (language.displayName.equals(query, ignoreCase = true)) score += 90
        
        // 开头匹配得分较高
        if (language.name.startsWith(query, ignoreCase = true)) score += 50
        if (language.displayName.startsWith(query, ignoreCase = true)) score += 50
        
        // 包含匹配得分一般
        if (language.name.contains(query, ignoreCase = true)) score += 20
        if (language.displayName.contains(query, ignoreCase = true)) score += 20
        
        return score
    }

    /**
     * 获取默认语言列表
     */
    private suspend fun getDefaultLanguages(): Result<List<Language>> {
        return try {
            val sourceLanguage = languageRepository.getDefaultSourceLanguage().getOrThrow()
            val targetLanguage = languageRepository.getDefaultTargetLanguage().getOrThrow()
            
            val defaultLanguages = mutableListOf<Language>()
            if (!sourceLanguage.isAutoDetect()) defaultLanguages.add(sourceLanguage)
            defaultLanguages.add(targetLanguage)
            
            // 添加一些常见语言
            val commonLanguages = listOf(
                Language.ENGLISH,
                Language.CHINESE_SIMPLIFIED,
                Language.JAPANESE,
                Language.KOREAN
            )
            
            commonLanguages.forEach { lang ->
                if (!defaultLanguages.contains(lang)) {
                    defaultLanguages.add(lang)
                }
            }
            
            Result.success(defaultLanguages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成默认语言对
     */
    private fun generateDefaultLanguagePairs(): List<LanguageRepository.LanguagePair> {
        return listOf(
            LanguageRepository.LanguagePair(Language.AUTO_DETECT, Language.ENGLISH),
            LanguageRepository.LanguagePair(Language.CHINESE_SIMPLIFIED, Language.ENGLISH),
            LanguageRepository.LanguagePair(Language.ENGLISH, Language.CHINESE_SIMPLIFIED)
        )
    }

    /**
     * 验证语言设置
     */
    private fun validateLanguageSettings(sourceLanguage: Language, targetLanguage: Language) {
        if (sourceLanguage == targetLanguage && !sourceLanguage.isAutoDetect()) {
            throw IllegalArgumentException("源语言和目标语言不能相同")
        }
        
        if (targetLanguage.isAutoDetect()) {
            throw IllegalArgumentException("目标语言不能设置为自动检测")
        }
    }

    // ==================== 数据类定义 ====================

    /**
     * 排序策略枚举
     */
    enum class SortStrategy {
        ALPHABETICAL,   // 按字母顺序
        BY_USAGE,       // 按使用频率
        BY_CODE,        // 按语言代码
        SMART           // 智能排序
    }

    /**
     * 默认语言设置
     */
    data class DefaultLanguageSettings(
        val sourceLanguage: Language,
        val targetLanguage: Language,
        val isFirstTime: Boolean,
        val recommendedPairs: List<LanguageRepository.LanguagePair>
    )

    /**
     * 语言异常类
     */
    class LanguageException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        /** 默认常用语言数量 */
        const val DEFAULT_FREQUENT_LIMIT = 5
        
        /** 默认搜索结果数量 */
        const val DEFAULT_SEARCH_LIMIT = 10
        
        /** 默认推荐语言对数量 */
        const val DEFAULT_RECOMMENDED_PAIRS_LIMIT = 3
    }
}
