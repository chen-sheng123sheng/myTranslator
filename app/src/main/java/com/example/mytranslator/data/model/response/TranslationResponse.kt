package com.example.mytranslator.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * 翻译响应数据模型
 *
 * 🎯 设计思想：
 * 1. API响应模型 - 专门用于解析网络响应的数据结构
 * 2. 错误处理支持 - 统一处理API成功和失败响应
 * 3. 多API兼容 - 设计通用的响应格式解析
 * 4. 数据完整性 - 保留API返回的所有有用信息
 *
 * 🔧 技术特性：
 * - 使用@SerializedName注解处理字段映射
 * - 支持嵌套对象和数组解析
 * - 提供响应验证和错误检查
 * - 便于转换为Domain模型
 *
 * 📱 使用场景：
 * - Retrofit网络响应的解析对象
 * - API响应数据的验证和处理
 * - 错误信息的提取和转换
 * - Domain模型的数据源
 *
 * 🎓 学习要点：
 * 为什么需要单独的响应模型？
 * 1. API格式适配 - 不同API有不同的响应格式
 * 2. 错误处理 - 统一处理各种API错误情况
 * 3. 数据转换 - 将API数据转换为业务模型
 * 4. 版本兼容 - API响应格式变化的适配
 */
data class TranslationResponse(
    /**
     * 响应状态码
     *
     * 🎯 设计考虑：
     * - API调用是否成功的标识
     * - 不同API有不同的成功码
     * - 用于错误处理和状态判断
     */
    @SerializedName("error_code")
    val errorCode: String? = null,

    /**
     * 错误信息
     *
     * 🎯 设计考虑：
     * - API返回的错误描述
     * - 用于用户提示和调试
     * - 支持多语言错误信息
     */
    @SerializedName("error_msg")
    val errorMessage: String? = null,

    /**
     * 翻译结果数组
     *
     * 🎯 设计考虑：
     * - 某些API返回多个翻译结果
     * - 支持批量翻译响应
     * - 包含翻译文本和相关信息
     */
    @SerializedName("trans_result")
    val translationResults: List<TranslationResult>? = null,

    /**
     * 检测到的源语言
     *
     * 🎯 设计考虑：
     * - 当使用自动检测时返回实际语言
     * - 用于用户确认和显示
     * - 提高翻译准确性
     */
    @SerializedName("from")
    val detectedSourceLanguage: String? = null,

    /**
     * 目标语言
     *
     * 🎯 设计考虑：
     * - 确认翻译的目标语言
     * - 用于结果验证
     */
    @SerializedName("to")
    val targetLanguage: String? = null,

    /**
     * 翻译置信度（可选）
     *
     * 🎯 设计考虑：
     * - 某些API提供翻译质量评分
     * - 范围通常是0.0-1.0
     * - 用于质量评估和用户提示
     */
    @SerializedName("confidence")
    val confidence: Float? = null,

    /**
     * API调用消耗的配额
     *
     * 🎯 设计考虑：
     * - 某些API返回配额使用情况
     * - 用于成本控制和监控
     * - 帮助用户了解使用情况
     */
    @SerializedName("usage")
    val usage: Usage? = null,

    /**
     * 服务器处理时间（毫秒）
     *
     * 🎯 设计考虑：
     * - API性能监控
     * - 用户体验优化参考
     * - 服务质量评估
     */
    @SerializedName("processing_time")
    val processingTime: Long? = null
) {

    /**
     * 翻译结果子对象
     */
    data class TranslationResult(
        /**
         * 原始文本
         */
        @SerializedName("src")
        val sourceText: String,

        /**
         * 翻译结果
         */
        @SerializedName("dst")
        val translatedText: String,

        /**
         * 该结果的置信度（可选）
         */
        @SerializedName("confidence")
        val confidence: Float? = null
    )

    /**
     * API使用情况子对象
     */
    data class Usage(
        /**
         * 本次调用消耗的字符数
         */
        @SerializedName("char_count")
        val characterCount: Int? = null,

        /**
         * 剩余配额
         */
        @SerializedName("remaining_quota")
        val remainingQuota: Long? = null,

        /**
         * 总配额
         */
        @SerializedName("total_quota")
        val totalQuota: Long? = null
    )

    /**
     * 🎓 学习要点：响应验证方法
     *
     * 为什么需要响应验证？
     * 1. API可靠性 - 确保响应数据的完整性
     * 2. 错误处理 - 统一处理各种异常情况
     * 3. 数据质量 - 验证翻译结果的有效性
     */

    /**
     * 检查响应是否成功
     *
     * 🎯 设计考虑：
     * - 统一的成功判断逻辑
     * - 适配不同API的成功标识
     * - 为后续处理提供可靠判断
     *
     * @return 是否成功
     */
    fun isSuccessful(): Boolean {
        return when {
            // 有错误码且不是成功码
            errorCode != null && !isSuccessCode(errorCode) -> false
            // 有错误信息
            !errorMessage.isNullOrBlank() -> false
            // 没有翻译结果
            translationResults.isNullOrEmpty() -> false
            // 翻译结果为空
            translationResults.any { it.translatedText.isBlank() } -> false
            else -> true
        }
    }

    /**
     * 获取错误信息
     *
     * 🎯 设计考虑：
     * - 提供用户友好的错误信息
     * - 统一错误信息格式
     * - 支持错误码到错误信息的映射
     *
     * @return 错误信息，成功时返回null
     */
    fun getErrorInfo(): String? {
        return when {
            !isSuccessful() -> {
                when {
                    !errorMessage.isNullOrBlank() -> errorMessage
                    errorCode != null -> getErrorMessageByCode(errorCode)
                    translationResults.isNullOrEmpty() -> "翻译结果为空"
                    else -> "未知错误"
                }
            }
            else -> null
        }
    }

    /**
     * 获取第一个翻译结果
     *
     * 🎯 设计考虑：
     * - 大多数情况下只需要第一个结果
     * - 简化调用方的代码
     * - 提供安全的访问方式
     *
     * @return 第一个翻译结果，如果没有则返回null
     */
    fun getFirstTranslation(): TranslationResult? {
        return translationResults?.firstOrNull()
    }

    /**
     * 获取翻译文本
     *
     * 🎯 设计考虑：
     * - 直接获取翻译后的文本
     * - 处理多个结果的合并
     * - 提供便捷的访问方式
     *
     * @return 翻译文本，如果没有则返回null
     */
    fun getTranslatedText(): String? {
        return when {
            translationResults.isNullOrEmpty() -> null
            translationResults.size == 1 -> translationResults.first().translatedText
            else -> {
                // 多个结果时合并
                translationResults.joinToString("\n") { it.translatedText }
            }
        }
    }

    /**
     * 获取原始文本
     *
     * @return 原始文本，如果没有则返回null
     */
    fun getSourceText(): String? {
        return when {
            translationResults.isNullOrEmpty() -> null
            translationResults.size == 1 -> translationResults.first().sourceText
            else -> {
                // 多个结果时合并
                translationResults.joinToString("\n") { it.sourceText }
            }
        }
    }

    /**
     * 获取平均置信度
     *
     * 🎯 设计考虑：
     * - 综合评估翻译质量
     * - 处理多个结果的置信度
     * - 为质量评估提供数据
     *
     * @return 平均置信度，如果没有则返回null
     */
    fun getAverageConfidence(): Float? {
        val confidences = mutableListOf<Float>()
        
        // 收集所有置信度
        confidence?.let { confidences.add(it) }
        translationResults?.forEach { result ->
            result.confidence?.let { confidences.add(it) }
        }
        
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            null
        }
    }

    /**
     * 获取响应摘要
     *
     * 🎯 设计考虑：
     * - 用于日志记录和调试
     * - 提供响应的关键信息
     * - 便于问题排查
     *
     * @return 响应摘要字符串
     */
    fun getSummary(): String {
        return if (isSuccessful()) {
            val text = getTranslatedText()?.take(50) ?: "无翻译结果"
            "TranslationResponse(success, text='$text${if ((getTranslatedText()?.length ?: 0) > 50) "..." else ""}')"
        } else {
            "TranslationResponse(error='${getErrorInfo()}')"
        }
    }

    /**
     * 验证响应数据的完整性
     *
     * 🎯 设计考虑：
     * - 确保数据可以正确转换为Domain模型
     * - 检查必要字段的存在
     * - 提供详细的验证信息
     *
     * @return 验证结果，成功返回null，失败返回错误信息
     */
    fun validateData(): String? {
        return when {
            !isSuccessful() -> getErrorInfo()
            detectedSourceLanguage.isNullOrBlank() -> "缺少源语言信息"
            targetLanguage.isNullOrBlank() -> "缺少目标语言信息"
            getTranslatedText().isNullOrBlank() -> "翻译结果为空"
            else -> null // 验证通过
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查是否为成功状态码
     */
    private fun isSuccessCode(code: String): Boolean {
        return code in listOf("0", "200", "52000", "success")
    }

    /**
     * 根据错误码获取错误信息
     */
    private fun getErrorMessageByCode(code: String): String {
        return when (code) {
            "52001" -> "请求超时，请重试"
            "52002" -> "系统错误，请稍后重试"
            "52003" -> "未授权用户，请检查API密钥"
            "54000" -> "必填参数为空"
            "54001" -> "签名错误，请检查API配置"
            "54003" -> "访问频率受限，请稍后重试"
            "54004" -> "账户余额不足"
            "54005" -> "长query请求频繁"
            "58000" -> "客户端IP非法"
            "58001" -> "译文语言方向不支持"
            "58002" -> "服务当前已关闭"
            "90107" -> "认证未通过或未生效"
            else -> "翻译失败，错误码：$code"
        }
    }

    /**
     * 🎓 学习要点：伴生对象的工厂方法
     */
    companion object {
        /**
         * 创建错误响应
         *
         * 🎯 设计考虑：
         * - 便于测试和Mock数据创建
         * - 统一错误响应的创建方式
         * - 简化错误处理代码
         *
         * @param errorCode 错误码
         * @param errorMessage 错误信息
         * @return 错误响应对象
         */
        fun createError(errorCode: String, errorMessage: String): TranslationResponse {
            return TranslationResponse(
                errorCode = errorCode,
                errorMessage = errorMessage
            )
        }

        /**
         * 创建成功响应
         *
         * @param sourceText 原文
         * @param translatedText 译文
         * @param sourceLanguage 源语言
         * @param targetLanguage 目标语言
         * @param confidence 置信度
         * @return 成功响应对象
         */
        fun createSuccess(
            sourceText: String,
            translatedText: String,
            sourceLanguage: String,
            targetLanguage: String,
            confidence: Float? = null
        ): TranslationResponse {
            return TranslationResponse(
                translationResults = listOf(
                    TranslationResult(
                        sourceText = sourceText,
                        translatedText = translatedText,
                        confidence = confidence
                    )
                ),
                detectedSourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                confidence = confidence
            )
        }
    }
}
