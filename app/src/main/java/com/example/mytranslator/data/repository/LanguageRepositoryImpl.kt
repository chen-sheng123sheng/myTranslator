package com.example.mytranslator.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mytranslator.MyTranslatorApplication
import com.example.mytranslator.data.config.ApiConfig
import com.example.mytranslator.data.mapper.TranslationMapper
import com.example.mytranslator.data.network.api.TranslationApi
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.repository.LanguageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 语言仓库实现类
 *
 * 🎯 设计目的：
 * 1. 从百度翻译API动态获取支持的语言列表
 * 2. 提供本地缓存机制，减少网络请求
 * 3. 实现回退策略，确保应用的健壮性
 * 4. 支持语言搜索和排序功能
 *
 * 🏗️ 架构设计：
 * - Repository模式：封装数据访问逻辑
 * - 网络优先策略：优先从API获取最新数据
 * - 本地回退机制：API失败时使用预定义语言
 * - 缓存策略：避免重复的网络请求
 *
 * 🔧 技术特性：
 * - 协程支持：所有操作都是异步的
 * - 错误处理：完善的异常处理和日志记录
 * - 类型安全：使用Result类型封装操作结果
 * - 性能优化：智能缓存和批量操作
 *
 * 🎓 学习要点：
 * Repository模式的实现：
 * 1. 数据源抽象：隐藏具体的数据获取方式
 * 2. 缓存策略：平衡性能和数据新鲜度
 * 3. 错误处理：提供有意义的错误信息
 * 4. 测试友好：便于Mock和单元测试
 */
class LanguageRepositoryImpl(
    private val translationApi: TranslationApi,
    private val context: Context
) : LanguageRepository {

    companion object {
        private const val TAG = "LanguageRepositoryImpl"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24小时

        // SharedPreferences相关常量
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_DEFAULT_SOURCE_LANGUAGE = "default_source_language"
        private const val KEY_DEFAULT_TARGET_LANGUAGE = "default_target_language"
        private const val KEY_LANGUAGE_USAGE_PREFIX = "usage_"
        private const val KEY_LANGUAGE_PAIR_PREFIX = "pair_"
        private const val KEY_TOTAL_TRANSLATIONS = "total_translations"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
    }

    // 语言列表缓存
    private var cachedLanguages: List<Language>? = null
    private var cacheTimestamp: Long = 0

    // SharedPreferences实例
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取所有支持的语言列表
     *
     * 🎯 实现策略：
     * 1. 检查缓存是否有效
     * 2. 使用预定义的语言列表（百度翻译API不提供动态语言列表接口）
     * 3. 根据参数进行过滤和排序
     *
     * 📝 说明：
     * 百度翻译API没有提供获取支持语言列表的接口，
     * 我们使用官方文档中的静态语种列表作为数据源。
     */
    override suspend fun getSupportedLanguages(
        includeAutoDetect: Boolean,
        sortByUsage: Boolean
    ): Result<List<Language>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🌐 开始获取支持的语言列表...")

            // 检查缓存
            if (isCacheValid()) {
                Log.d(TAG, "✅ 使用缓存的语言列表")
                val languages = procesLanguageList(cachedLanguages!!, includeAutoDetect, sortByUsage)
                return@withContext Result.success(languages)
            }

            // 使用预定义的语言列表
            val languages = getStaticLanguageList()

            // 更新缓存
            cachedLanguages = languages
            cacheTimestamp = System.currentTimeMillis()

            // 处理语言列表
            val processedLanguages = procesLanguageList(languages, includeAutoDetect, sortByUsage)

            Log.d(TAG, "✅ 成功获取 ${processedLanguages.size} 种语言")
            Result.success(processedLanguages)

        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取语言列表失败", e)

            // 回退到预定义语言列表
            val fallbackLanguages = Language.getSupportedLanguages()
            val processedLanguages = procesLanguageList(fallbackLanguages, includeAutoDetect, sortByUsage)

            Log.w(TAG, "⚠️ 使用预定义语言列表，共 ${processedLanguages.size} 种语言")
            Result.success(processedLanguages)
        }
    }

    /**
     * 获取静态语言列表
     *
     * 基于百度翻译API官方文档的语种列表
     * https://ai.baidu.com/ai-doc/MT/4kqryjku9#语种列表
     *
     * 📝 说明：百度翻译API没有提供获取支持语言列表的接口，
     * 我们使用官方文档中的静态语种列表作为数据源。
     *
     * 🌍 国际化设计：
     * - 使用语言代码作为key，通过strings.xml获取本地化名称
     * - 支持中英文显示，根据用户系统语言自动切换
     * - 便于维护和扩展更多语言支持
     */
    private fun getStaticLanguageList(): List<Language> {
        Log.d(TAG, "📋 使用静态语言列表...")

        return listOf(
            // 自动检测
            createLanguageWithI18n("auto"),

            // 常用语言（按使用频率排序）
            createLanguageWithI18n("zh"),
            createLanguageWithI18n("en"),
            createLanguageWithI18n("jp"),
            createLanguageWithI18n("kor"),
            createLanguageWithI18n("fra"),
            createLanguageWithI18n("de"),
            createLanguageWithI18n("spa"),
            createLanguageWithI18n("it"),
            createLanguageWithI18n("pt"),
            createLanguageWithI18n("ru"),
            createLanguageWithI18n("ara"),
            createLanguageWithI18n("th"),
            createLanguageWithI18n("vie"),
            createLanguageWithI18n("nl"),
            createLanguageWithI18n("hi"),
            createLanguageWithI18n("id"),
            createLanguageWithI18n("may"),
            createLanguageWithI18n("cht"),
            createLanguageWithI18n("per"),

            // 欧洲语言
            createLanguageWithI18n("nor"),
            createLanguageWithI18n("swe"),
            createLanguageWithI18n("dan"),
            createLanguageWithI18n("fin"),
            createLanguageWithI18n("cs"),
            createLanguageWithI18n("sk"),
            createLanguageWithI18n("hu"),
            createLanguageWithI18n("rom"),
            createLanguageWithI18n("bul"),
            createLanguageWithI18n("hrv"),
            createLanguageWithI18n("srp"),
            createLanguageWithI18n("slo"),
            createLanguageWithI18n("ukr"),
            createLanguageWithI18n("bel"),
            createLanguageWithI18n("est"),
            createLanguageWithI18n("lav"),
            createLanguageWithI18n("lit"),
            createLanguageWithI18n("pl"),
            createLanguageWithI18n("tr"),
            createLanguageWithI18n("el"),
            createLanguageWithI18n("heb"),

            // 南亚和东南亚语言
            createLanguageWithI18n("urd"),
            createLanguageWithI18n("ben"),
            createLanguageWithI18n("tam"),
            createLanguageWithI18n("tel"),
            createLanguageWithI18n("kan"),
            createLanguageWithI18n("mal"),
            createLanguageWithI18n("guj"),
            createLanguageWithI18n("pan"),
            createLanguageWithI18n("sin"),
            createLanguageWithI18n("nep"),
            createLanguageWithI18n("asm"),
            createLanguageWithI18n("ori"),
            createLanguageWithI18n("fil"),
            createLanguageWithI18n("tgl"),
            createLanguageWithI18n("hkm"),
            createLanguageWithI18n("bur"),

            // 非洲语言
            createLanguageWithI18n("swa"),
            createLanguageWithI18n("som"),
            createLanguageWithI18n("amh"),
            createLanguageWithI18n("xho"),
            createLanguageWithI18n("afr"),

            // 其他欧洲语言
            createLanguageWithI18n("ice"),
            createLanguageWithI18n("wel"),
            createLanguageWithI18n("gle"),
            createLanguageWithI18n("baq"),
            createLanguageWithI18n("cat"),
            createLanguageWithI18n("glg"),
            createLanguageWithI18n("mlt"),
            createLanguageWithI18n("lat"),
            createLanguageWithI18n("epo"),

            // 中亚和高加索语言
            createLanguageWithI18n("arm"),
            createLanguageWithI18n("geo"),
            createLanguageWithI18n("aze"),
            createLanguageWithI18n("tgk"),
            createLanguageWithI18n("kur"),
            createLanguageWithI18n("tat")
        )
    }

    /**
     * 创建国际化的Language对象
     *
     * 🌍 国际化设计说明：
     * 这个方法通过语言代码从strings.xml中获取本地化的语言名称，
     * 实现了真正的国际化支持。
     *
     * 🎯 设计优势：
     * 1. 分离关注点 - UI文本与业务逻辑分离
     * 2. 易于维护 - 所有语言名称集中在strings.xml中
     * 3. 国际化支持 - 根据用户系统语言自动显示
     * 4. 扩展性 - 可以轻松添加更多语言支持
     *
     * @param languageCode 语言代码（如"zh", "en", "jp"等）
     * @return 包含国际化名称的Language对象
     */
    private fun createLanguageWithI18n(languageCode: String): Language {
        // 获取应用上下文
        val context = MyTranslatorApplication.instance

        // 构建资源名称
        val resourceName = "language_$languageCode"

        // 获取资源ID
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "string",
            context.packageName
        )

        // 获取本地化的语言名称
        val localizedName = if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            // 如果没有找到对应的资源，使用语言代码作为fallback
            languageCode.uppercase()
        }

        // 创建Language对象
        // 注意：这里我们使用本地化名称作为name和displayName
        // 在实际应用中，可能需要更复杂的逻辑来处理英文名称和本地名称
        return Language(
            code = languageCode,
            name = localizedName,  // 使用本地化名称
            displayName = localizedName  // 使用本地化名称
        )
    }

    /**
     * 处理语言列表（过滤和排序）
     */
    private fun procesLanguageList(
        languages: List<Language>,
        includeAutoDetect: Boolean,
        sortByUsage: Boolean
    ): List<Language> {
        var result = languages

        // 过滤自动检测选项
        if (!includeAutoDetect) {
            result = result.filter { !it.isAutoDetect() }
        }

        // 排序
        if (sortByUsage) {
            // 按使用频率排序（这里使用预定义的顺序作为示例）
            val priorityOrder = listOf("auto", "zh", "en", "ja", "ko", "fr", "de", "es")
            result = result.sortedBy { language ->
                val index = priorityOrder.indexOf(language.code)
                if (index >= 0) index else Int.MAX_VALUE
            }
        } else {
            // 按字母顺序排序
            result = result.sortedBy { it.name }
        }

        return result
    }

    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return cachedLanguages != null && 
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS
    }

    /**
     * 获取常用语言列表
     */
    override suspend fun getFrequentlyUsedLanguages(limit: Int): Result<List<Language>> {
        return try {
            // 获取所有语言，然后返回最常用的几种
            val allLanguages = getSupportedLanguages(includeAutoDetect = true, sortByUsage = true)
                .getOrThrow()
            
            val frequentLanguages = allLanguages.take(limit)
            Result.success(frequentLanguages)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取常用语言失败", e)
            Result.failure(e)
        }
    }

    /**
     * 搜索语言
     */
    override suspend fun searchLanguages(query: String, limit: Int): Result<List<Language>> {
        return try {
            val allLanguages = getSupportedLanguages(includeAutoDetect = true, sortByUsage = false)
                .getOrThrow()
            
            val filteredLanguages = allLanguages.filter { language ->
                language.name.contains(query, ignoreCase = true) ||
                language.displayName.contains(query, ignoreCase = true) ||
                language.code.contains(query, ignoreCase = true)
            }.take(limit)
            
            Result.success(filteredLanguages)
            
        } catch (e: Exception) {
            Log.e(TAG, "搜索语言失败", e)
            Result.failure(e)
        }
    }

    /**
     * 根据语言代码获取语言
     */
    override suspend fun getLanguageByCode(code: String): Result<Language?> {
        return try {
            val allLanguages = getSupportedLanguages(includeAutoDetect = true, sortByUsage = false)
                .getOrThrow()
            
            val language = allLanguages.find { it.code.equals(code, ignoreCase = true) }
            Result.success(language)
            
        } catch (e: Exception) {
            Log.e(TAG, "根据代码获取语言失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取默认源语言
     *
     * 🎯 设计思路：
     * 1. 优先返回用户设置的偏好
     * 2. 如果未设置，返回系统推荐的默认值
     * 3. 使用SharedPreferences持久化存储
     */
    override suspend fun getDefaultSourceLanguage(): Result<Language> {
        return try {
            val languageCode = preferences.getString(KEY_DEFAULT_SOURCE_LANGUAGE, null)
            val language = if (languageCode != null) {
                // 用户有设置偏好，尝试获取对应的Language对象
                getLanguageByCode(languageCode).getOrNull() ?: Language.AUTO_DETECT
            } else {
                // 用户未设置，使用默认值
                Language.AUTO_DETECT
            }
            Result.success(language)
        } catch (e: Exception) {
            Log.e(TAG, "获取默认源语言失败", e)
            Result.success(Language.AUTO_DETECT) // 失败时返回安全的默认值
        }
    }

    /**
     * 获取默认目标语言
     *
     * 🎯 设计思路：
     * 1. 优先返回用户设置的偏好
     * 2. 考虑用户的系统语言环境
     * 3. 提供智能的默认推荐
     */
    override suspend fun getDefaultTargetLanguage(): Result<Language> {
        return try {
            val languageCode = preferences.getString(KEY_DEFAULT_TARGET_LANGUAGE, null)
            val language = if (languageCode != null) {
                getLanguageByCode(languageCode).getOrNull() ?: Language.ENGLISH
            } else {
                // 根据系统语言智能推荐
                getSmartDefaultTargetLanguage()
            }
            Result.success(language)
        } catch (e: Exception) {
            Log.e(TAG, "获取默认目标语言失败", e)
            Result.success(Language.ENGLISH)
        }
    }

    /**
     * 设置默认源语言
     *
     * 🎯 用户偏好持久化：
     * - 保存用户的语言选择偏好
     * - 更新使用统计
     * - 提供即时反馈
     */
    override suspend fun setDefaultSourceLanguage(language: Language): Result<Unit> {
        return try {
            preferences.edit()
                .putString(KEY_DEFAULT_SOURCE_LANGUAGE, language.code)
                .apply()

            Log.d(TAG, "默认源语言已设置为: ${language.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "设置默认源语言失败", e)
            Result.failure(e)
        }
    }

    /**
     * 设置默认目标语言
     */
    override suspend fun setDefaultTargetLanguage(language: Language): Result<Unit> {
        return try {
            preferences.edit()
                .putString(KEY_DEFAULT_TARGET_LANGUAGE, language.code)
                .apply()

            Log.d(TAG, "默认目标语言已设置为: ${language.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "设置默认目标语言失败", e)
            Result.failure(e)
        }
    }

    /**
     * 记录语言使用情况
     *
     * 🎯 使用统计的价值：
     * 1. 为用户提供个性化推荐
     * 2. 优化语言列表排序
     * 3. 分析用户行为模式
     * 4. 改进产品功能
     */
    override suspend fun recordLanguageUsage(
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<Unit> {
        return try {
            val editor = preferences.edit()

            // 记录单个语言使用次数
            val sourceKey = KEY_LANGUAGE_USAGE_PREFIX + sourceLanguage.code
            val targetKey = KEY_LANGUAGE_USAGE_PREFIX + targetLanguage.code
            val sourceCount = preferences.getInt(sourceKey, 0) + 1
            val targetCount = preferences.getInt(targetKey, 0) + 1

            // 记录语言对使用次数
            val pairKey = KEY_LANGUAGE_PAIR_PREFIX + "${sourceLanguage.code}_${targetLanguage.code}"
            val pairCount = preferences.getInt(pairKey, 0) + 1

            // 记录总翻译次数
            val totalCount = preferences.getInt(KEY_TOTAL_TRANSLATIONS, 0) + 1

            // 更新最后使用时间
            val currentTime = System.currentTimeMillis()

            editor.putInt(sourceKey, sourceCount)
                .putInt(targetKey, targetCount)
                .putInt(pairKey, pairCount)
                .putInt(KEY_TOTAL_TRANSLATIONS, totalCount)
                .putLong(KEY_LAST_UPDATE_TIME, currentTime)
                .apply()

            Log.d(TAG, "语言使用统计已更新: ${sourceLanguage.code} -> ${targetLanguage.code}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "记录语言使用失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取语言使用统计
     *
     * 🎯 统计数据的应用：
     * - 显示用户的使用习惯
     * - 提供数据驱动的推荐
     * - 支持功能优化决策
     */
    override suspend fun getLanguageUsageStatistics(): Result<LanguageRepository.LanguageUsageStatistics> {
        return try {
            val totalTranslations = preferences.getInt(KEY_TOTAL_TRANSLATIONS, 0)
            val lastUpdateTime = preferences.getLong(KEY_LAST_UPDATE_TIME, 0)

            // 获取所有语言使用统计（String格式）
            val languageUsageStringMap = mutableMapOf<String, Int>()
            val languagePairStringMap = mutableMapOf<String, Int>()

            preferences.all.forEach { (key, value) ->
                when {
                    key.startsWith(KEY_LANGUAGE_USAGE_PREFIX) && value is Int -> {
                        val languageCode = key.removePrefix(KEY_LANGUAGE_USAGE_PREFIX)
                        languageUsageStringMap[languageCode] = value
                    }
                    key.startsWith(KEY_LANGUAGE_PAIR_PREFIX) && value is Int -> {
                        val pairCode = key.removePrefix(KEY_LANGUAGE_PAIR_PREFIX)
                        languagePairStringMap[pairCode] = value
                    }
                }
            }

            // 转换为Language对象的Map
            val languageUsageMap = convertToLanguageMap(languageUsageStringMap)
            val languagePairUsage = convertToLanguagePairMap(languagePairStringMap)

            // 找出最常用的语言
            val mostUsedSourceLanguage = findMostUsedLanguage(languageUsageStringMap)
            val mostUsedTargetLanguage = findMostUsedLanguage(languageUsageStringMap)

            val statistics = LanguageRepository.LanguageUsageStatistics(
                totalTranslations = totalTranslations,
                mostUsedSourceLanguage = mostUsedSourceLanguage,
                mostUsedTargetLanguage = mostUsedTargetLanguage,
                languageUsageMap = languageUsageMap,
                languagePairUsage = languagePairUsage,
                lastUpdateTime = lastUpdateTime
            )

            Result.success(statistics)

        } catch (e: Exception) {
            Log.e(TAG, "获取语言使用统计失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取推荐的语言对
     *
     * 🎯 智能推荐算法：
     * 基于用户历史使用数据推荐最可能使用的语言对
     */
    override suspend fun getRecommendedLanguagePairs(limit: Int): Result<List<LanguageRepository.LanguagePair>> {
        return try {
            val statistics = getLanguageUsageStatistics().getOrThrow()

            // 直接从统计数据中获取推荐的语言对
            val recommendedPairs = statistics.getPopularLanguagePairs(limit)

            Result.success(recommendedPairs)

        } catch (e: Exception) {
            Log.e(TAG, "获取推荐语言对失败", e)
            Result.success(emptyList()) // 失败时返回空列表
        }
    }

    /**
     * 检查语言功能支持
     *
     * 🎯 功能检查的意义：
     * 不同语言可能支持不同的功能（如语音合成、OCR等）
     */
    override suspend fun isLanguageFeatureSupported(
        language: Language,
        feature: LanguageRepository.LanguageFeature
    ): Result<Boolean> {
        return try {
            // 根据实际API能力进行判断
            val isSupported = when (feature) {
                LanguageRepository.LanguageFeature.TEXT_TRANSLATION -> {
                    // 所有语言都支持文本翻译
                    true
                }
                LanguageRepository.LanguageFeature.VOICE_RECOGNITION -> {
                    // 假设主要语言支持语音识别
                    language.code in listOf("zh", "en", "ja", "ko", "fr", "de", "es")
                }
                LanguageRepository.LanguageFeature.VOICE_SYNTHESIS -> {
                    // 假设主要语言支持语音合成
                    language.code in listOf("zh", "en", "ja", "ko", "fr", "de", "es")
                }
                LanguageRepository.LanguageFeature.OCR_RECOGNITION -> {
                    // 假设部分语言支持OCR识别
                    language.code in listOf("zh", "en", "ja", "ko")
                }
                LanguageRepository.LanguageFeature.OFFLINE_TRANSLATION -> {
                    // 假设少数语言支持离线翻译
                    language.code in listOf("zh", "en")
                }
            }

            Result.success(isSupported)

        } catch (e: Exception) {
            Log.e(TAG, "检查语言功能支持失败", e)
            Result.success(false) // 失败时返回不支持
        }
    }

    /**
     * 更新语言数据
     *
     * 🎯 数据更新策略：
     * 强制从API重新获取最新的语言列表
     */
    override suspend fun updateLanguageData(forceUpdate: Boolean): Result<Unit> {
        return try {
            if (forceUpdate) {
                // 清除缓存，强制重新获取
                cachedLanguages = null
                cacheTimestamp = 0
            }

            // 重新加载语言列表
            getSupportedLanguages(includeAutoDetect = true, sortByUsage = false)

            Log.d(TAG, "语言数据更新完成")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "更新语言数据失败", e)
            Result.failure(e)
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 智能推荐默认目标语言
     *
     * 🎯 推荐逻辑：
     * 1. 根据系统语言环境
     * 2. 考虑地理位置
     * 3. 提供合理的默认选择
     */
    private fun getSmartDefaultTargetLanguage(): Language {
        val systemLanguage = java.util.Locale.getDefault().language
        return when (systemLanguage) {
            "zh" -> Language.ENGLISH  // 中文用户推荐英语
            "en" -> Language.CHINESE_SIMPLIFIED  // 英文用户推荐中文
            "ja" -> Language.ENGLISH  // 日语用户推荐英语
            "ko" -> Language.ENGLISH  // 韩语用户推荐英语
            else -> Language.ENGLISH  // 其他语言用户推荐英语
        }
    }

    /**
     * 查找最常用的语言
     */
    private suspend fun findMostUsedLanguage(usageMap: Map<String, Int>): Language? {
        return usageMap.maxByOrNull { it.value }?.let { (code, _) ->
            getLanguageByCode(code).getOrNull()
        }
    }

    /**
     * 将String代码的Map转换为Language对象的Map
     */
    private suspend fun convertToLanguageMap(stringMap: Map<String, Int>): Map<Language, Int> {
        val result = mutableMapOf<Language, Int>()
        stringMap.forEach { (code, count) ->
            getLanguageByCode(code).getOrNull()?.let { language ->
                result[language] = count
            }
        }
        return result
    }

    /**
     * 将String代码的语言对Map转换为LanguagePair对象的Map
     */
    private suspend fun convertToLanguagePairMap(stringMap: Map<String, Int>): Map<LanguageRepository.LanguagePair, Int> {
        val result = mutableMapOf<LanguageRepository.LanguagePair, Int>()
        stringMap.forEach { (pairCode, count) ->
            val codes = pairCode.split("_")
            if (codes.size == 2) {
                val sourceLanguage = getLanguageByCode(codes[0]).getOrNull()
                val targetLanguage = getLanguageByCode(codes[1]).getOrNull()
                if (sourceLanguage != null && targetLanguage != null) {
                    val languagePair = LanguageRepository.LanguagePair(
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        usageCount = count,
                        lastUsedTime = System.currentTimeMillis()
                    )
                    result[languagePair] = count
                }
            }
        }
        return result
    }
}
