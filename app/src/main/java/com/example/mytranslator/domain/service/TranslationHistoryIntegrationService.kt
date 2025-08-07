package com.example.mytranslator.domain.service

import android.util.Log
import com.example.mytranslator.domain.model.TranslationResult
import com.example.mytranslator.domain.usecase.SaveTranslationRequest
import com.example.mytranslator.domain.usecase.SaveTranslationUseCase
import com.example.mytranslator.domain.usecase.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 翻译历史记录集成服务
 *
 * 🎯 设计目的：
 * 1. 连接现有翻译流程和新的历史记录系统
 * 2. 将TranslationResult转换为历史记录格式
 * 3. 提供统一的历史记录保存接口
 * 4. 处理数据转换和错误恢复
 *
 * 🏗️ 集成策略：
 * - 适配器模式：转换不同的数据格式
 * - 服务层设计：封装复杂的业务逻辑
 * - 错误隔离：历史记录保存失败不影响翻译功能
 * - 异步处理：不阻塞主要的翻译流程
 *
 * 📱 使用场景：
 * - TranslateUseCase中的历史记录保存
 * - 批量导入翻译记录
 * - 数据迁移和同步
 * - 离线翻译结果保存
 *
 * 🎓 学习要点：
 * 系统集成的设计模式：
 * 1. 适配器模式 - 连接不兼容的接口
 * 2. 服务层 - 封装复杂的业务逻辑
 * 3. 错误隔离 - 防止次要功能影响主要功能
 * 4. 数据转换 - 处理不同系统间的数据格式
 */
class TranslationHistoryIntegrationService(
    private val saveTranslationUseCase: SaveTranslationUseCase
) {

    companion object {
        private const val TAG = "TranslationHistoryIntegrationService"
    }

    /**
     * 保存翻译结果到历史记录
     *
     * 🔧 集成流程：
     * 1. 验证翻译结果的有效性
     * 2. 转换为历史记录保存格式
     * 3. 调用历史记录保存用例
     * 4. 处理保存结果和错误
     * 5. 记录操作日志
     *
     * @param translationResult 翻译结果
     * @return 保存操作的结果
     */
    suspend fun saveTranslationToHistory(translationResult: TranslationResult): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 开始保存翻译结果到历史记录")
                Log.d(TAG, "  原文: ${translationResult.getOriginalText()}")
                Log.d(TAG, "  译文: ${translationResult.translatedText}")
                Log.d(TAG, "  语言: ${translationResult.sourceLanguage.code} -> ${translationResult.targetLanguage.code}")
                
                // 1. 验证翻译结果
                val validationResult = validateTranslationResult(translationResult)
                if (!validationResult.isValid) {
                    Log.w(TAG, "❌ 翻译结果验证失败: ${validationResult.errorMessage}")
                    return@withContext Result.failure(
                        IllegalArgumentException(validationResult.errorMessage)
                    )
                }
                
                // 2. 转换为保存请求
                val saveRequest = convertToSaveRequest(translationResult)
                
                // 3. 执行保存操作
                val saveResult = saveTranslationUseCase(saveRequest)
                
                // 4. 处理保存结果
                when (saveResult) {
                    is SaveResult.Success -> {
                        Log.i(TAG, "✅ 翻译历史记录保存成功: ${saveResult.translationHistory.id}")
                        Result.success(Unit)
                    }
                    
                    is SaveResult.ValidationError -> {
                        Log.w(TAG, "❌ 保存验证失败: ${saveResult.message}")
                        Result.failure(IllegalArgumentException(saveResult.message))
                    }
                    
                    is SaveResult.BusinessRuleError -> {
                        Log.w(TAG, "❌ 业务规则错误: ${saveResult.message}")
                        Result.failure(IllegalStateException(saveResult.message))
                    }
                    
                    is SaveResult.RepositoryError -> {
                        Log.e(TAG, "❌ Repository保存失败: ${saveResult.message}")
                        Result.failure(Exception(saveResult.message))
                    }
                    
                    is SaveResult.UnknownError -> {
                        Log.e(TAG, "❌ 未知保存错误: ${saveResult.message}")
                        Result.failure(Exception(saveResult.message))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存翻译历史记录时发生异常", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 批量保存翻译结果到历史记录
     *
     * @param translationResults 翻译结果列表
     * @return 批量保存操作的结果
     */
    suspend fun saveTranslationsToHistoryBatch(
        translationResults: List<TranslationResult>
    ): Result<BatchSaveResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 开始批量保存翻译结果: ${translationResults.size} 条")
                
                if (translationResults.isEmpty()) {
                    return@withContext Result.success(
                        BatchSaveResult(
                            totalCount = 0,
                            successCount = 0,
                            failureCount = 0,
                            errors = emptyList()
                        )
                    )
                }
                
                val results = mutableListOf<SaveResult>()
                var successCount = 0
                var failureCount = 0
                val errors = mutableListOf<String>()
                
                // 逐个处理每个翻译结果
                for ((index, result) in translationResults.withIndex()) {
                    try {
                        val saveResult = saveTranslationToHistory(result)
                        
                        if (saveResult.isSuccess) {
                            successCount++
                        } else {
                            failureCount++
                            val error = saveResult.exceptionOrNull()?.message ?: "保存失败"
                            errors.add("第${index + 1}条记录: $error")
                        }
                        
                    } catch (e: Exception) {
                        failureCount++
                        errors.add("第${index + 1}条记录: ${e.message}")
                    }
                }
                
                Log.i(TAG, "✅ 批量保存完成: 成功 $successCount, 失败 $failureCount")
                
                Result.success(
                    BatchSaveResult(
                        totalCount = translationResults.size,
                        successCount = successCount,
                        failureCount = failureCount,
                        errors = errors
                    )
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量保存时发生异常", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 验证翻译结果的有效性
     *
     * @param translationResult 翻译结果
     * @return 验证结果
     */
    private fun validateTranslationResult(translationResult: TranslationResult): ValidationResult {
        // 检查原文
        val originalText = translationResult.getOriginalText()
        if (originalText.isBlank()) {
            return ValidationResult(false, "原文不能为空")
        }
        
        // 检查译文
        if (translationResult.translatedText.isBlank()) {
            return ValidationResult(false, "译文不能为空")
        }
        
        // 检查语言信息
        if (translationResult.sourceLanguage.code.isBlank()) {
            return ValidationResult(false, "源语言代码不能为空")
        }
        
        if (translationResult.targetLanguage.code.isBlank()) {
            return ValidationResult(false, "目标语言代码不能为空")
        }
        
        // 检查文本长度
        if (originalText.length > 5000) {
            return ValidationResult(false, "原文长度不能超过5000个字符")
        }
        
        if (translationResult.translatedText.length > 5000) {
            return ValidationResult(false, "译文长度不能超过5000个字符")
        }
        
        return ValidationResult(true, "")
    }

    /**
     * 将TranslationResult转换为SaveTranslationRequest
     *
     * @param translationResult 翻译结果
     * @return 保存请求
     */
    private fun convertToSaveRequest(translationResult: TranslationResult): SaveTranslationRequest {
        return SaveTranslationRequest(
            originalText = translationResult.getOriginalText(),
            translatedText = translationResult.translatedText,
            sourceLanguageCode = translationResult.sourceLanguage.code,
            targetLanguageCode = translationResult.targetLanguage.code,
            sourceLanguageName = translationResult.sourceLanguage.name,
            targetLanguageName = translationResult.targetLanguage.name,
            translationProvider = determineTranslationProvider(translationResult),
            qualityScore = translationResult.confidence?.let { it / 100.0 } // 转换为0-1范围
        )
    }

    /**
     * 确定翻译服务提供商
     *
     * @param translationResult 翻译结果
     * @return 翻译服务提供商名称
     */
    private fun determineTranslationProvider(translationResult: TranslationResult): String {
        // 根据翻译结果的特征判断服务提供商
        // 这里可以根据实际情况进行扩展
        return "baidu" // 当前主要使用百度翻译
    }

    /**
     * 验证结果数据类
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )
}

/**
 * 批量保存结果数据类
 */
data class BatchSaveResult(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    /**
     * 获取成功率
     */
    fun getSuccessRate(): Double = if (totalCount > 0) {
        successCount.toDouble() / totalCount
    } else 0.0
    
    /**
     * 是否全部成功
     */
    fun isAllSuccess(): Boolean = failureCount == 0
    
    /**
     * 是否有部分成功
     */
    fun hasPartialSuccess(): Boolean = successCount > 0 && failureCount > 0
}
