package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.model.TranslationInput
import com.example.mytranslator.domain.model.TranslationResult
import com.example.mytranslator.domain.repository.LanguageRepository
import com.example.mytranslator.domain.repository.TranslationRepository

/**
 * 翻译用例
 *
 * 🎯 设计思想：
 * 1. UseCase模式 - 封装完整的业务逻辑流程
 * 2. 单一职责原则 - 专门负责翻译相关的业务逻辑
 * 3. 依赖注入 - 通过构造函数注入Repository依赖
 * 4. 错误处理 - 统一处理各种异常情况
 *
 * 🔧 技术特性：
 * - 完整的翻译业务流程：验证→缓存→翻译→保存→统计
 * - 智能缓存策略：避免重复翻译相同内容
 * - 自动语言检测：当源语言为"自动检测"时
 * - 历史记录管理：自动保存翻译结果
 * - 使用统计更新：记录语言使用情况
 *
 * 📱 使用场景：
 * - ViewModel调用执行翻译操作
 * - 批量翻译服务的核心逻辑
 * - 后台翻译任务的业务处理
 * - 翻译质量评估和优化
 *
 * 🎓 学习要点：
 * UseCase模式的核心价值：
 * 1. 业务逻辑集中 - 所有翻译相关逻辑都在这里
 * 2. 可复用性 - 不同UI层都可以使用相同的业务逻辑
 * 3. 可测试性 - 业务逻辑独立，便于单元测试
 * 4. 关注点分离 - ViewModel专注UI，UseCase专注业务
 */
class TranslateUseCase(
    private val translationRepository: TranslationRepository,
    private val languageRepository: LanguageRepository
) {



    /**
     * 翻译参数数据类
     *
     * 🎯 设计考虑：
     * - 明确的输入参数，避免参数过多的问题
     * - 支持可选参数，提供灵活的配置
     * - 便于参数验证和业务逻辑处理
     */
    data class Params(
        /** 翻译输入内容 */
        val input: TranslationInput,
        
        /** 源语言（可以是自动检测） */
        val sourceLanguage: Language,
        
        /** 目标语言 */
        val targetLanguage: Language,
        
        /** 是否启用缓存查找 */
        val enableCache: Boolean = true,
        
        /** 是否自动保存到历史记录 */
        val saveToHistory: Boolean = true,
        
        /** 是否更新语言使用统计 */
        val updateStatistics: Boolean = true,
        
        /** 翻译质量要求（可选） */
        val qualityLevel: QualityLevel = QualityLevel.STANDARD
    )

    /**
     * 翻译质量等级
     *
     * 🎯 设计考虑：
     * - 为不同场景提供不同的翻译质量选择
     * - 平衡翻译质量和响应速度
     * - 为未来的高级功能预留扩展
     */
    enum class QualityLevel {
        FAST,       // 快速翻译，优先速度
        STANDARD,   // 标准翻译，平衡质量和速度
        HIGH        // 高质量翻译，优先准确性
    }

    /**
     * 执行翻译操作
     *
     * 🎯 业务流程：
     * 1. 输入验证 - 检查输入数据的有效性
     * 2. 语言处理 - 处理自动检测语言
     * 3. 缓存查找 - 检查是否有可用的缓存结果
     * 4. 执行翻译 - 调用翻译服务
     * 5. 结果处理 - 保存历史和更新统计
     * 6. 错误处理 - 统一处理各种异常情况
     *
     * @param params 翻译参数
     * @return 翻译结果，包装在Result中
     */
    suspend fun execute(params: Params): Result<TranslationResult> {
        Log.d(TAG, "🎯 UseCase开始执行翻译")
        Log.d(TAG, "  输入: ${when(params.input) { is TranslationInput.Text -> params.input.content; else -> params.input.toString() }}")
        Log.d(TAG, "  ${params.sourceLanguage.code} -> ${params.targetLanguage.code}")
        Log.d(TAG, "  缓存: ${params.enableCache}, 历史: ${params.saveToHistory}, 统计: ${params.updateStatistics}")

        return try {
            // 第一步：输入验证
            Log.d(TAG, "📋 第一步：输入验证")
            validateInput(params)?.let { error ->
                Log.e(TAG, "❌ 输入验证失败: $error")
                return Result.failure(IllegalArgumentException(error))
            }

            // 第二步：处理源语言（自动检测）
            Log.d(TAG, "🔍 第二步：处理源语言")
            val actualSourceLanguage = resolveSourceLanguage(params.input, params.sourceLanguage)
                .getOrElse {
                    Log.e(TAG, "❌ 源语言处理失败")
                    return Result.failure(it)
                }
            Log.d(TAG, "  实际源语言: ${actualSourceLanguage.code}(${actualSourceLanguage.name})")

            // 第三步：缓存查找
            Log.d(TAG, "💾 第三步：缓存查找")
            if (params.enableCache) {
                val cachedResult = checkCache(params.input, actualSourceLanguage, params.targetLanguage)
                    .getOrNull()
                if (cachedResult != null && cachedResult.isCacheValid()) {
                    Log.d(TAG, "✅ 找到有效缓存，直接返回")
                    // 更新统计（即使是缓存结果）
                    if (params.updateStatistics) {
                        updateLanguageUsageStatistics(actualSourceLanguage, params.targetLanguage)
                    }
                    return Result.success(cachedResult)
                } else {
                    Log.d(TAG, "  无有效缓存，继续网络翻译")
                }
            } else {
                Log.d(TAG, "  缓存已禁用")
            }

            // 第四步：执行翻译
            Log.d(TAG, "🌐 第四步：执行网络翻译")
            val startTime = System.currentTimeMillis()
            val translationResult = performTranslation(params.input, actualSourceLanguage, params.targetLanguage)
                .getOrElse {
                    Log.e(TAG, "❌ 网络翻译失败")
                    return Result.failure(it)
                }
            val endTime = System.currentTimeMillis()
            Log.d(TAG, "✅ 网络翻译完成，耗时: ${endTime - startTime}ms")

            // 第五步：创建完整的翻译结果
            val completeResult = enhanceTranslationResult(
                result = translationResult,
                durationMs = endTime - startTime,
                qualityLevel = params.qualityLevel
            )

            // 第六步：后处理操作
            performPostProcessing(completeResult, params)

            Result.success(completeResult)

        } catch (e: Exception) {
            // 统一错误处理
            Result.failure(TranslationException("翻译失败: ${e.message}", e))
        }
    }

    /**
     * 验证输入参数
     *
     * 🎯 设计考虑：
     * - 在业务逻辑开始前进行完整验证
     * - 提供详细的错误信息
     * - 避免无效请求浪费资源
     */
    private fun validateInput(params: Params): String? {
        // 验证输入内容
        params.input.validate()?.let { return it }

        // 验证语言设置
        if (params.sourceLanguage == params.targetLanguage && !params.sourceLanguage.isAutoDetect()) {
            return "源语言和目标语言不能相同"
        }

        // 验证业务规则
        if (params.targetLanguage.isAutoDetect()) {
            return "目标语言不能设置为自动检测"
        }

        return null // 验证通过
    }

    /**
     * 解析实际的源语言
     *
     * 🎯 设计考虑：
     * - 处理"自动检测"的情况
     * - 为翻译提供准确的语言信息
     * - 支持语言检测失败的回退策略
     */
    private suspend fun resolveSourceLanguage(
        input: TranslationInput,
        sourceLanguage: Language
    ): Result<Language> {
        return if (sourceLanguage.isAutoDetect()) {
            // 调用语言检测
            translationRepository.detectLanguage(input)
                .recoverCatching { 
                    // 检测失败时的回退策略
                    languageRepository.getDefaultSourceLanguage().getOrThrow()
                }
        } else {
            Result.success(sourceLanguage)
        }
    }

    /**
     * 检查缓存
     *
     * 🎯 设计考虑：
     * - 避免重复翻译相同内容
     * - 提高响应速度
     * - 节省API调用成本
     */
    private suspend fun checkCache(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult?> {
        return translationRepository.getCachedTranslation(input, sourceLanguage, targetLanguage)
    }

    /**
     * 执行实际的翻译操作
     *
     * 🎯 设计考虑：
     * - 调用Repository进行翻译
     * - 处理翻译服务的各种异常
     * - 为结果添加质量评估
     */
    private suspend fun performTranslation(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult> {
        Log.d(TAG, "📡 调用Repository进行翻译")
        Log.d(TAG, "  文本: ${when(input) { is TranslationInput.Text -> input.content; else -> input.toString() }}")
        Log.d(TAG, "  ${sourceLanguage.code} -> ${targetLanguage.code}")

        val result = translationRepository.translate(input, sourceLanguage, targetLanguage)

        result.onSuccess { translationResult ->
            Log.d(TAG, "✅ Repository翻译成功: ${translationResult.translatedText}")
        }.onFailure { exception ->
            Log.e(TAG, "❌ Repository翻译失败: ${exception.message}", exception)
        }

        return result
    }

    /**
     * 增强翻译结果
     *
     * 🎯 设计考虑：
     * - 添加性能信息
     * - 根据质量等级调整置信度
     * - 为结果添加元数据
     */
    private fun enhanceTranslationResult(
        result: TranslationResult,
        durationMs: Long,
        qualityLevel: QualityLevel
    ): TranslationResult {
        return result.copy(
            durationMs = durationMs,
            // 根据质量等级调整置信度显示
            confidence = adjustConfidenceByQuality(result.confidence, qualityLevel)
        )
    }

    /**
     * 根据质量等级调整置信度
     */
    private fun adjustConfidenceByQuality(originalConfidence: Float?, qualityLevel: QualityLevel): Float? {
        return originalConfidence?.let { confidence ->
            when (qualityLevel) {
                QualityLevel.FAST -> confidence * 0.9f // 快速模式置信度稍低
                QualityLevel.STANDARD -> confidence     // 标准模式保持原值
                QualityLevel.HIGH -> minOf(confidence * 1.1f, 1.0f) // 高质量模式置信度稍高
            }
        }
    }

    /**
     * 执行后处理操作
     *
     * 🎯 设计考虑：
     * - 保存翻译历史
     * - 更新使用统计
     * - 处理副作用操作
     */
    private suspend fun performPostProcessing(result: TranslationResult, params: Params) {
        // 保存到历史记录
        if (params.saveToHistory) {
            translationRepository.saveTranslationToHistory(result)
                .onFailure { 
                    // 历史保存失败不影响翻译结果，只记录日志
                    println("保存翻译历史失败: ${it.message}")
                }
        }

        // 更新语言使用统计
        if (params.updateStatistics) {
            updateLanguageUsageStatistics(result.sourceLanguage, result.targetLanguage)
        }
    }

    /**
     * 更新语言使用统计
     */
    private suspend fun updateLanguageUsageStatistics(sourceLanguage: Language, targetLanguage: Language) {
        languageRepository.recordLanguageUsage(sourceLanguage, targetLanguage)
            .onFailure {
                // 统计更新失败不影响翻译结果
                println("更新语言统计失败: ${it.message}")
            }
    }

    /**
     * 翻译异常类
     *
     * 🎯 设计考虑：
     * - 提供特定的异常类型
     * - 便于错误处理和用户提示
     * - 保留原始异常信息用于调试
     */
    class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * 🎓 学习要点：伴生对象的使用
     */
    companion object {
        private const val TAG = "TranslateUseCase"

        /** 缓存有效期阈值 */
        const val CACHE_VALIDITY_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24小时

        /** 最大重试次数 */
        const val MAX_RETRY_COUNT = 3
        
        /** 翻译超时时间 */
        const val TRANSLATION_TIMEOUT_MS = 30_000L // 30秒
    }
}
