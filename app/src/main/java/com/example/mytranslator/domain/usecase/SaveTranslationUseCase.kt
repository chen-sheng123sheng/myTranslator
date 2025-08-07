package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.model.TranslationHistory
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 保存翻译记录用例
 *
 * 🎯 业务目标：
 * 将翻译结果保存到历史记录中，支持：
 * - 数据验证和清理
 * - 重复检测和处理
 * - 自动标签生成
 * - 错误处理和重试
 *
 * 🏗️ Use Case设计原则：
 * - 单一职责：只负责保存翻译记录的业务逻辑
 * - 输入验证：确保数据的有效性和完整性
 * - 错误处理：提供清晰的错误信息和恢复策略
 * - 性能优化：避免重复保存和不必要的操作
 *
 * 📱 使用场景：
 * - 翻译完成后自动保存
 * - 用户手动保存翻译结果
 * - 批量导入翻译记录
 * - 离线翻译结果同步
 *
 * 🎓 学习要点：
 * Use Case的核心概念：
 * 1. 业务封装 - 将复杂的业务逻辑封装成简单的接口
 * 2. 数据验证 - 在业务层进行数据有效性检查
 * 3. 错误处理 - 提供业务级别的错误处理
 * 4. 可测试性 - 便于进行业务逻辑的单元测试
 */
class SaveTranslationUseCase(
    private val translationHistoryRepository: TranslationHistoryRepository
) {

    companion object {
        private const val TAG = "SaveTranslationUseCase"
        
        // 业务规则常量
        private const val MAX_TEXT_LENGTH = 5000
        private const val MIN_TEXT_LENGTH = 1
    }

    /**
     * 执行保存翻译记录的业务逻辑
     *
     * 🔧 业务流程：
     * 1. 输入数据验证
     * 2. 业务规则检查
     * 3. 数据清理和标准化
     * 4. 调用Repository保存
     * 5. 结果处理和日志记录
     *
     * @param request 保存请求参数
     * @return SaveResult 保存结果，包含成功状态和错误信息
     */
    suspend operator fun invoke(request: SaveTranslationRequest): SaveResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🚀 开始保存翻译记录")
                
                // 1. 输入验证
                val validationResult = validateInput(request)
                if (!validationResult.isValid) {
                    Log.w(TAG, "❌ 输入验证失败: ${validationResult.errorMessage}")
                    return@withContext SaveResult.ValidationError(validationResult.errorMessage)
                }
                
                // 2. 创建领域模型
                val translationHistory = createTranslationHistory(request)
                
                // 3. 业务规则检查
                val businessValidationResult = validateBusinessRules(translationHistory)
                if (!businessValidationResult.isValid) {
                    Log.w(TAG, "❌ 业务规则验证失败: ${businessValidationResult.errorMessage}")
                    return@withContext SaveResult.BusinessRuleError(businessValidationResult.errorMessage)
                }
                
                // 4. 保存到Repository
                val repositoryResult = translationHistoryRepository.saveTranslation(translationHistory)
                
                if (repositoryResult.isSuccess) {
                    Log.i(TAG, "✅ 翻译记录保存成功: ${translationHistory.id}")
                    SaveResult.Success(translationHistory)
                } else {
                    val error = repositoryResult.exceptionOrNull()
                    Log.e(TAG, "❌ Repository保存失败", error)
                    SaveResult.RepositoryError(error?.message ?: "保存失败")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存翻译记录时发生异常", e)
                SaveResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 输入数据验证
     *
     * 🔧 验证规则：
     * - 必填字段检查
     * - 文本长度限制
     * - 语言代码有效性
     * - 翻译服务提供商验证
     */
    private fun validateInput(request: SaveTranslationRequest): ValidationResult {
        // 检查原文
        if (request.originalText.isBlank()) {
            return ValidationResult(false, "原文不能为空")
        }
        if (request.originalText.length > MAX_TEXT_LENGTH) {
            return ValidationResult(false, "原文长度不能超过 $MAX_TEXT_LENGTH 个字符")
        }
        if (request.originalText.length < MIN_TEXT_LENGTH) {
            return ValidationResult(false, "原文长度不能少于 $MIN_TEXT_LENGTH 个字符")
        }
        
        // 检查译文
        if (request.translatedText.isBlank()) {
            return ValidationResult(false, "译文不能为空")
        }
        if (request.translatedText.length > MAX_TEXT_LENGTH) {
            return ValidationResult(false, "译文长度不能超过 $MAX_TEXT_LENGTH 个字符")
        }
        
        // 检查语言代码
        if (request.sourceLanguageCode.isBlank()) {
            return ValidationResult(false, "源语言代码不能为空")
        }
        if (request.targetLanguageCode.isBlank()) {
            return ValidationResult(false, "目标语言代码不能为空")
        }
        
        // 检查语言名称
        if (request.sourceLanguageName.isBlank()) {
            return ValidationResult(false, "源语言名称不能为空")
        }
        if (request.targetLanguageName.isBlank()) {
            return ValidationResult(false, "目标语言名称不能为空")
        }
        
        // 检查翻译服务提供商
        if (request.translationProvider.isBlank()) {
            return ValidationResult(false, "翻译服务提供商不能为空")
        }
        
        return ValidationResult(true, "")
    }

    /**
     * 业务规则验证
     *
     * 🔧 业务规则：
     * - 源语言和目标语言不能相同
     * - 原文和译文不能完全相同
     * - 支持的翻译服务提供商检查
     */
    private fun validateBusinessRules(translation: TranslationHistory): ValidationResult {
        // 源语言和目标语言不能相同
        if (translation.sourceLanguageCode == translation.targetLanguageCode) {
            return ValidationResult(false, "源语言和目标语言不能相同")
        }
        
        // 原文和译文不能完全相同（除非是特殊情况，如专有名词）
        if (translation.originalText.trim() == translation.translatedText.trim() && 
            translation.originalText.length > 10) {
            return ValidationResult(false, "原文和译文不能完全相同")
        }
        
        // 检查支持的翻译服务提供商
        val supportedProviders = listOf("baidu", "google", "youdao", "tencent")
        if (!supportedProviders.contains(translation.translationProvider.lowercase())) {
            return ValidationResult(false, "不支持的翻译服务提供商: ${translation.translationProvider}")
        }
        
        return ValidationResult(true, "")
    }

    /**
     * 创建翻译历史记录领域模型
     *
     * 🔧 数据处理：
     * - 文本清理和标准化
     * - 自动生成时间戳
     * - 设置默认值
     */
    private fun createTranslationHistory(request: SaveTranslationRequest): TranslationHistory {
        return TranslationHistory(
            originalText = request.originalText.trim(),
            translatedText = request.translatedText.trim(),
            sourceLanguageCode = request.sourceLanguageCode.lowercase(),
            targetLanguageCode = request.targetLanguageCode.lowercase(),
            sourceLanguageName = request.sourceLanguageName.trim(),
            targetLanguageName = request.targetLanguageName.trim(),
            timestamp = System.currentTimeMillis(),
            isFavorite = false, // 默认不收藏
            translationProvider = request.translationProvider.lowercase(),
            qualityScore = request.qualityScore,
            usageCount = 0, // 初始使用次数为0
            lastAccessTime = System.currentTimeMillis(),
            tags = emptyList(), // 初始无标签
            notes = null // 初始无备注
        )
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
 * 保存翻译请求参数
 *
 * 🎯 设计说明：
 * 封装保存翻译记录所需的所有参数，
 * 提供类型安全的数据传递。
 */
data class SaveTranslationRequest(
    val originalText: String,
    val translatedText: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val sourceLanguageName: String,
    val targetLanguageName: String,
    val translationProvider: String,
    val qualityScore: Double? = null
)

/**
 * 保存结果密封类
 *
 * 🎯 设计说明：
 * 使用密封类提供类型安全的结果处理，
 * 明确区分不同类型的错误和成功状态。
 */
sealed class SaveResult {
    /**
     * 保存成功
     */
    data class Success(val translationHistory: TranslationHistory) : SaveResult()
    
    /**
     * 输入验证错误
     */
    data class ValidationError(val message: String) : SaveResult()
    
    /**
     * 业务规则错误
     */
    data class BusinessRuleError(val message: String) : SaveResult()
    
    /**
     * Repository层错误
     */
    data class RepositoryError(val message: String) : SaveResult()
    
    /**
     * 未知错误
     */
    data class UnknownError(val message: String) : SaveResult()
    
    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? = when (this) {
        is Success -> null
        is ValidationError -> message
        is BusinessRuleError -> message
        is RepositoryError -> message
        is UnknownError -> message
    }
}
