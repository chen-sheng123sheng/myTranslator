package com.example.mytranslator.data.repository

import android.util.Log
import com.example.mytranslator.data.mapper.TranslationMapper
import com.example.mytranslator.data.network.api.TranslationApi
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.model.TranslationInput
import com.example.mytranslator.domain.model.TranslationResult
import com.example.mytranslator.domain.repository.TranslationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 翻译仓库实现类
 *
 * 🎯 设计思想：
 * 1. Repository模式实现 - 将Domain接口转换为具体的技术实现
 * 2. 多数据源整合 - 网络API + 本地缓存 + 偏好设置
 * 3. 错误处理统一 - 将技术异常转换为业务异常
 * 4. 性能优化 - 缓存策略和异步处理
 *
 * 🔧 技术特性：
 * - 协程异步处理，不阻塞UI线程
 * - 内存缓存提高响应速度
 * - 网络错误的优雅处理和重试
 * - 数据转换的类型安全保证
 *
 * 📱 使用场景：
 * - UseCase层调用的具体实现
 * - 网络API和本地存储的协调
 * - 翻译缓存和历史记录管理
 * - 错误处理和用户反馈
 *
 * 🎓 学习要点：
 * Repository实现的核心职责：
 * 1. 数据源协调 - 决定从哪里获取数据
 * 2. 缓存策略 - 平衡性能和数据新鲜度
 * 3. 错误转换 - 技术错误到业务错误的映射
 * 4. 异步处理 - 协程的正确使用
 */
class TranslationRepositoryImpl(
    private val translationApi: TranslationApi,
    private val appId: String? = null,
    private val secretKey: String? = null
) : TranslationRepository {

    // 内存缓存：翻译结果缓存
    private val translationCache = ConcurrentHashMap<String, TranslationResult>()
    
    // 内存缓存：翻译历史记录
    private val historyCache = mutableListOf<TranslationResult>()
    
    // 缓存大小限制
    private val maxCacheSize = 100
    private val maxHistorySize = 1000

    /**
     * 执行翻译操作
     *
     * 🎯 实现策略：
     * 1. 参数验证和预处理
     * 2. 缓存查找（可选）
     * 3. 网络API调用
     * 4. 数据转换和验证
     * 5. 缓存更新
     * 6. 错误处理和重试
     */
    override suspend fun translate(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 开始翻译请求")
            Log.d(TAG, "  输入: ${when(input) { is TranslationInput.Text -> input.content; else -> input.toString() }}")
            Log.d(TAG, "  ${sourceLanguage.code} -> ${targetLanguage.code}")
            Log.d(TAG, "  Repository配置: appId=${appId?.take(8)}..., secretKey=${if (secretKey != null) "已配置" else "未配置"}")

            // 1. 生成缓存键
            val cacheKey = generateCacheKey(input, sourceLanguage, targetLanguage)

            // 2. 检查缓存
            translationCache[cacheKey]?.let { cachedResult ->
                if (cachedResult.isCacheValid()) {
                    Log.d(TAG, "✅ 使用缓存结果")
                    return@withContext Result.success(cachedResult)
                } else {
                    // 移除过期缓存
                    translationCache.remove(cacheKey)
                    Log.d(TAG, "🗑️ 移除过期缓存")
                }
            }

            // 3. 准备API请求
            val request = TranslationMapper.toApiRequest(
                input = input,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                appId = appId,
                secretKey = secretKey
            )

            // 4. 执行网络请求
            Log.d(TAG, "🌐 发送网络请求...")
            Log.d(TAG, "📋 请求详情:")
            Log.d(TAG, "  query: ${request.query}")
            Log.d(TAG, "  from: ${request.sourceLanguage}")
            Log.d(TAG, "  to: ${request.targetLanguage}")
            Log.d(TAG, "  appId: ${request.appId}")
            Log.d(TAG, "  salt: ${request.salt}")
            Log.d(TAG, "  sign: ${request.signature}")

            val requestTime = System.currentTimeMillis()
            // 使用GET请求方式调用百度翻译API
            val response = translationApi.translateWithQuery(
                query = request.query,
                from = request.sourceLanguage,
                to = request.targetLanguage,
                appId = request.appId,
                salt = request.salt,
                sign = request.signature
            )

            // 5. 检查HTTP响应
            Log.d(TAG, "📡 收到HTTP响应: ${response.code()}")
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ HTTP请求失败: ${response.code()} ${response.message()}")
                return@withContext Result.failure(
                    Exception("网络请求失败: HTTP ${response.code()}")
                )
            }

            val responseBody = response.body()
            if (responseBody == null) {
                Log.e(TAG, "❌ 响应体为空")
                return@withContext Result.failure(Exception("响应体为空"))
            }

            // 记录API响应详情
            Log.d(TAG, "📄 API响应详情:")
            Log.d(TAG, "  错误码: ${responseBody.errorCode}")
            Log.d(TAG, "  错误信息: ${responseBody.errorMessage}")
            Log.d(TAG, "  翻译结果数量: ${responseBody.translationResults?.size ?: 0}")

            if (!responseBody.isSuccessful()) {
                Log.e(TAG, "❌ API返回错误: ${responseBody.errorCode} - ${responseBody.errorMessage}")
                return@withContext Result.failure(
                    Exception("翻译API错误: ${responseBody.errorMessage ?: responseBody.errorCode}")
                )
            }

            // 6. 转换为Domain模型
            val translationResult = TranslationMapper.toDomainResult(
                response = responseBody,
                originalInput = input,
                requestTime = requestTime
            )

            // 7. 验证结果
            TranslationMapper.validateTranslationResult(translationResult)?.let { error ->
                return@withContext Result.failure(Exception("翻译结果验证失败: $error"))
            }

            // 8. 更新缓存
            updateCache(cacheKey, translationResult)

            Result.success(translationResult)

        } catch (e: TranslationMapper.TranslationApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("翻译失败: ${e.message}", e))
        }
    }

    /**
     * 检测输入内容的语言
     */
    override suspend fun detectLanguage(input: TranslationInput): Result<Language> = withContext(Dispatchers.IO) {
        try {
            val text = when (input) {
                is TranslationInput.Text -> input.content
                else -> return@withContext Result.failure(
                    UnsupportedOperationException("暂不支持该类型的语言检测")
                )
            }

            val response = translationApi.detectLanguage(text, appId)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("语言检测请求失败: HTTP ${response.code()}")
                )
            }

            val responseBody = response.body()
                ?: return@withContext Result.failure(Exception("语言检测响应体为空"))

            val detectedLanguage = TranslationMapper.toDetectedLanguage(responseBody)
            Result.success(detectedLanguage)

        } catch (e: TranslationMapper.LanguageDetectionException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("语言检测失败: ${e.message}", e))
        }
    }

    /**
     * 获取翻译历史记录
     */
    override suspend fun getTranslationHistory(
        limit: Int,
        offset: Int
    ): Result<List<TranslationResult>> = withContext(Dispatchers.Default) {
        try {
            val history = historyCache
                .sortedByDescending { it.timestamp } // 按时间倒序
                .drop(offset) // 跳过offset个
                .take(limit) // 取limit个

            Result.success(history)
        } catch (e: Exception) {
            Result.failure(Exception("获取翻译历史失败: ${e.message}", e))
        }
    }

    /**
     * 保存翻译结果到历史记录
     */
    override suspend fun saveTranslationToHistory(result: TranslationResult): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // 检查是否已存在相同的翻译
            val existingIndex = historyCache.indexOfFirst { existing ->
                existing.input == result.input &&
                existing.sourceLanguage == result.sourceLanguage &&
                existing.targetLanguage == result.targetLanguage
            }

            if (existingIndex >= 0) {
                // 更新现有记录的时间戳
                historyCache[existingIndex] = result.copy(timestamp = System.currentTimeMillis())
            } else {
                // 添加新记录
                historyCache.add(0, result) // 添加到开头
                
                // 限制历史记录大小
                if (historyCache.size > maxHistorySize) {
                    historyCache.removeAt(historyCache.size - 1)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("保存翻译历史失败: ${e.message}", e))
        }
    }

    /**
     * 删除翻译历史记录
     */
    override suspend fun deleteTranslationHistory(results: List<TranslationResult>): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            results.forEach { result ->
                historyCache.removeAll { it.timestamp == result.timestamp }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("删除翻译历史失败: ${e.message}", e))
        }
    }

    /**
     * 清空所有翻译历史记录
     */
    override suspend fun clearTranslationHistory(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            historyCache.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("清空翻译历史失败: ${e.message}", e))
        }
    }

    /**
     * 搜索翻译历史记录
     */
    override suspend fun searchTranslationHistory(
        query: String,
        limit: Int
    ): Result<List<TranslationResult>> = withContext(Dispatchers.Default) {
        try {
            val queryLower = query.lowercase()
            val searchResults = historyCache.filter { result ->
                val originalText = when (result.input) {
                    is TranslationInput.Text -> result.input.content.lowercase()
                    else -> ""
                }
                val translatedText = result.translatedText.lowercase()
                
                originalText.contains(queryLower) || translatedText.contains(queryLower)
            }
            .sortedByDescending { it.timestamp }
            .take(limit)

            Result.success(searchResults)
        } catch (e: Exception) {
            Result.failure(Exception("搜索翻译历史失败: ${e.message}", e))
        }
    }

    /**
     * 获取缓存的翻译结果
     */
    override suspend fun getCachedTranslation(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult?> = withContext(Dispatchers.Default) {
        try {
            val cacheKey = generateCacheKey(input, sourceLanguage, targetLanguage)
            val cachedResult = translationCache[cacheKey]
            
            if (cachedResult != null && cachedResult.isCacheValid()) {
                Result.success(cachedResult)
            } else {
                // 移除过期缓存
                cachedResult?.let { translationCache.remove(cacheKey) }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(Exception("获取缓存翻译失败: ${e.message}", e))
        }
    }

    /**
     * 获取翻译统计信息
     */
    override suspend fun getTranslationStatistics(): Result<TranslationRepository.TranslationStatistics> = withContext(Dispatchers.Default) {
        try {
            val totalTranslations = historyCache.size
            val today = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            
            val todayTranslations = historyCache.count { 
                (today - it.timestamp) < oneDayMs 
            }

            // 统计最常用的语言
            val sourceLanguages = historyCache.groupBy { it.sourceLanguage }
            val targetLanguages = historyCache.groupBy { it.targetLanguage }
            
            val favoriteSource = sourceLanguages.maxByOrNull { it.value.size }?.key
            val favoriteTarget = targetLanguages.maxByOrNull { it.value.size }?.key

            // 计算平均翻译时间
            val averageTime = historyCache.mapNotNull { it.durationMs }.average().toLong()

            // 统计最常用的输入类型
            val inputTypes = historyCache.groupBy { it.input.getTypeName() }
            val mostUsedInputType = inputTypes.maxByOrNull { it.value.size }?.key ?: "文本翻译"

            // 计算总翻译字符数
            val totalChars = historyCache.sumOf { result ->
                when (result.input) {
                    is TranslationInput.Text -> result.input.content.length.toLong()
                    else -> 0L
                }
            }

            val statistics = TranslationRepository.TranslationStatistics(
                totalTranslations = totalTranslations,
                todayTranslations = todayTranslations,
                favoriteSourceLanguage = favoriteSource,
                favoriteTargetLanguage = favoriteTarget,
                averageTranslationTime = averageTime,
                mostUsedInputType = mostUsedInputType,
                totalCharactersTranslated = totalChars
            )

            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(Exception("获取翻译统计失败: ${e.message}", e))
        }
    }

    /**
     * 设置翻译偏好配置
     */
    override suspend fun setTranslationPreferences(preferences: TranslationRepository.TranslationPreferences): Result<Unit> {
        // TODO: 实现偏好设置的持久化存储
        return Result.success(Unit)
    }

    /**
     * 获取翻译偏好配置
     */
    override suspend fun getTranslationPreferences(): Result<TranslationRepository.TranslationPreferences> {
        // TODO: 从持久化存储中读取偏好设置
        val defaultPreferences = TranslationRepository.TranslationPreferences(
            defaultSourceLanguage = Language.AUTO_DETECT,
            defaultTargetLanguage = Language.ENGLISH
        )
        return Result.success(defaultPreferences)
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成缓存键
     */
    private fun generateCacheKey(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): String {
        val inputHash = when (input) {
            is TranslationInput.Text -> input.content.hashCode()
            else -> input.hashCode()
        }
        return "${sourceLanguage.code}-${targetLanguage.code}-$inputHash"
    }

    /**
     * 更新缓存
     */
    private fun updateCache(cacheKey: String, result: TranslationResult) {
        translationCache[cacheKey] = result
        
        // 限制缓存大小
        if (translationCache.size > maxCacheSize) {
            // 移除最旧的缓存项
            val oldestKey = translationCache.entries
                .minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { translationCache.remove(it) }
        }
    }

    /**
     * 🎓 学习要点：伴生对象的工厂方法
     */
    companion object {
        private const val TAG = "TranslationRepository"

        /** 默认缓存大小 */
        const val DEFAULT_CACHE_SIZE = 100

        /** 默认历史记录大小 */
        const val DEFAULT_HISTORY_SIZE = 1000

        /**
         * 创建Repository实例的工厂方法
         *
         * @param api 翻译API接口
         * @param appId 应用ID
         * @param secretKey 密钥
         * @return Repository实例
         */
        fun create(
            api: TranslationApi,
            appId: String? = null,
            secretKey: String? = null
        ): TranslationRepositoryImpl {
            return TranslationRepositoryImpl(api, appId, secretKey)
        }
    }
}
