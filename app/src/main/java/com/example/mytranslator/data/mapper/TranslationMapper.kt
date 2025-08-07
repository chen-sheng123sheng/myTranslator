package com.example.mytranslator.data.mapper

import android.util.Log

import com.example.mytranslator.data.model.request.TranslationRequest
import com.example.mytranslator.data.model.response.TranslationResponse
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.model.TranslationInput
import com.example.mytranslator.domain.model.TranslationResult
import com.example.mytranslator.data.network.api.TranslationApi.LanguageDetectionResponse


/**
 * 翻译数据转换器
 *
 * 🎯 设计思想：
 * 1. 数据转换职责 - 专门负责API模型与Domain模型的转换
 * 2. 错误处理转换 - 将API错误转换为业务异常
 * 3. 数据验证和清理 - 确保转换后数据的完整性
 * 4. 单向转换原则 - 明确转换方向，避免循环依赖
 *
 * 🔧 技术特性：
 * - 静态方法设计，无状态转换
 * - 空安全处理，避免空指针异常
 * - 类型安全转换，编译时检查
 * - 异常安全，转换失败时提供详细信息
 *
 * 📱 使用场景：
 * - Repository实现中的数据转换
 * - API响应到Domain模型的映射
 * - Domain模型到API请求的映射
 * - 错误响应到业务异常的转换
 *
 * 🎓 学习要点：
 * Mapper模式的核心价值：
 * 1. 解耦 - API变化不影响Domain层
 * 2. 转换 - 处理不同数据格式的差异
 * 3. 验证 - 确保数据转换的正确性
 * 4. 清理 - 处理脏数据和异常情况
 */
object TranslationMapper {

    private const val TAG = "TranslationMapper"

    /**
     * 将Domain模型转换为API请求模型
     *
     * 🎯 设计考虑：
     * - 处理不同类型的TranslationInput
     * - 生成API所需的签名和参数
     * - 验证转换后数据的有效性
     *
     * @param input 翻译输入
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param appId 应用ID（可选）
     * @param secretKey 密钥（用于签名，可选）
     * @return API请求模型
     * @throws IllegalArgumentException 当输入数据无效时
     */
    fun toApiRequest(
        input: TranslationInput,
        sourceLanguage: Language,
        targetLanguage: Language,
        appId: String? = null,
        secretKey: String? = null
    ): TranslationRequest {
        // 提取文本内容
        val queryText = when (input) {
            is TranslationInput.Text -> input.content
            is TranslationInput.Voice -> throw UnsupportedOperationException("语音翻译暂未支持")
            is TranslationInput.Image -> throw UnsupportedOperationException("图片翻译暂未支持")
        }

        // 处理语言代码
        val fromCode = if (sourceLanguage.isAutoDetect()) "auto" else sourceLanguage.code
        val toCode = targetLanguage.code

        // 生成签名相关参数
        val salt = if (appId != null && secretKey != null) {
            System.currentTimeMillis().toString()
        } else null

        val signature = if (appId != null && secretKey != null && salt != null) {
            generateSignature(appId, queryText, salt, secretKey)
        } else null

        // 添加详细日志
        Log.d(TAG, "🔧 翻译请求参数生成:")
        Log.d(TAG, "  输入文本: $queryText")
        Log.d(TAG, "  源语言: ${sourceLanguage.code}")
        Log.d(TAG, "  目标语言: ${targetLanguage.code}")
        Log.d(TAG, "  APP ID: ${appId ?: "未配置"}")
        Log.d(TAG, "  Secret Key: ${if (secretKey != null) "已配置(${secretKey.length}位)" else "未配置"}")
        Log.d(TAG, "  Salt: ${salt ?: "未生成"}")
        Log.d(TAG, "  Signature: ${signature ?: "未生成"}")

        if (appId == null || secretKey == null) {
            Log.e(TAG, "❌ API配置不完整，无法生成有效签名")
        }

        // 创建请求对象
        val request = TranslationRequest(
            query = queryText,
            sourceLanguage = fromCode,
            targetLanguage = toCode,
            appId = appId,
            salt = salt,
            signature = signature
        )

        // 验证请求
        request.validate()?.let { error ->
            throw IllegalArgumentException("请求参数无效: $error")
        }

        return request
    }

    /**
     * 将API响应转换为Domain模型
     *
     * 🎯 设计考虑：
     * - 处理API成功和失败响应
     * - 转换语言代码为Language对象
     * - 生成完整的翻译结果信息
     * - 处理置信度和元数据
     *
     * @param response API响应
     * @param originalInput 原始输入
     * @param requestTime 请求时间戳
     * @return Domain翻译结果
     * @throws TranslationApiException 当API响应错误时
     */
    fun toDomainResult(
        response: TranslationResponse,
        originalInput: TranslationInput,
        requestTime: Long = System.currentTimeMillis()
    ): TranslationResult {
        // 检查响应是否成功
        if (!response.isSuccessful()) {
            val errorInfo = response.getErrorInfo() ?: "未知错误"
            throw TranslationApiException("翻译失败: $errorInfo")
        }

        // 获取翻译文本
        val translatedText = response.getTranslatedText()
            ?: throw TranslationApiException("翻译结果为空")

        // 转换语言对象
        val sourceLanguage = response.detectedSourceLanguage?.let { code ->
            Language.findByCode(code) ?: createUnknownLanguage(code)
        } ?: throw TranslationApiException("缺少源语言信息")

        val targetLanguage = response.targetLanguage?.let { code ->
            Language.findByCode(code) ?: createUnknownLanguage(code)
        } ?: throw TranslationApiException("缺少目标语言信息")

        // 获取置信度
        val confidence = response.getAverageConfidence()

        // 获取处理时间
        val durationMs = response.processingTime

        // 确定翻译服务提供商
        val provider = determineProvider(response)

        // 创建翻译结果
        return TranslationResult(
            input = originalInput,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            timestamp = requestTime,
            confidence = confidence,
            provider = provider,
            durationMs = durationMs
        )
    }

    /**
     * 将语言检测响应转换为Language对象
     *
     * 🎯 设计考虑：
     * - 处理语言检测的成功和失败
     * - 转换语言代码为标准Language对象
     * - 提供检测失败时的回退策略
     *
     * @param response 语言检测响应
     * @return 检测到的语言
     * @throws LanguageDetectionException 当检测失败时
     */
    fun toDetectedLanguage(
        response: LanguageDetectionResponse
    ): Language {
        if (!response.isSuccessful()) {
            throw LanguageDetectionException("语言检测失败: ${response.errorMessage}")
        }

        val detectedCode = response.getDetectedLanguage()
            ?: throw LanguageDetectionException("语言检测结果为空")

        return Language.findByCode(detectedCode)
            ?: createUnknownLanguage(detectedCode)
    }

    /**
     * 将支持的语言列表响应转换为Language列表
     *
     * 🎯 设计考虑：
     * - 过滤和验证API返回的语言列表
     * - 转换为标准的Language对象
     * - 处理未知语言的情况
     *
     * @param response 支持的语言列表响应
     * @return Language对象列表
     */
    fun toSupportedLanguages(
        response: com.example.mytranslator.data.network.api.TranslationApi.SupportedLanguagesResponse
    ): List<Language> {
        if (!response.isSuccessful()) {
            // 如果API失败，返回预定义的语言列表
            return Language.getSupportedLanguages()
        }

        val languageMap = response.getLanguageMap()
        val languages = mutableListOf<Language>()

        // 添加自动检测选项
        languages.add(Language.AUTO_DETECT)

        // 转换API返回的语言
        languageMap.forEach { (code, name) ->
            val language = Language.findByCode(code) ?: Language(
                code = code,
                name = name,
                displayName = name
            )
            languages.add(language)
        }

        return languages.distinctBy { it.code }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成API签名
     *
     * 🎯 设计考虑：
     * - 按照百度翻译API的签名规则
     * - 使用MD5哈希算法
     * - 确保签名的安全性
     */
    private fun generateSignature(appId: String, query: String, salt: String, secretKey: String): String {
        val signStr = "$appId$query$salt$secretKey"
        val signature = md5(signStr)

        Log.d(TAG, "🔐 签名生成详情:")
        Log.d(TAG, "  拼接字符串: $signStr")
        Log.d(TAG, "  MD5签名: $signature")

        return signature
    }

    /**
     * MD5哈希计算
     */
    private fun md5(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))

            // 转换为16进制字符串
            val hexString = StringBuilder()
            for (byte in digest) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }

            hexString.toString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 创建未知语言对象
     *
     * 🎯 设计考虑：
     * - 处理API返回的未知语言代码
     * - 提供合理的默认显示名称
     * - 避免因未知语言导致的异常
     */
    private fun createUnknownLanguage(code: String): Language {
        return Language(
            code = code,
            name = "Unknown ($code)",
            displayName = "未知语言 ($code)"
        )
    }

    /**
     * 确定翻译服务提供商
     *
     * 🎯 设计考虑：
     * - 根据响应特征判断服务提供商
     * - 用于统计和质量分析
     * - 支持多服务提供商的识别
     */
    private fun determineProvider(response: TranslationResponse): String {
        return when {
            // 根据响应特征判断提供商
            response.errorCode != null -> "baidu" // 百度API有错误码字段
            response.usage != null -> "baidu"     // 百度API有使用情况字段
            else -> "unknown"
        }
    }

    /**
     * 🎓 学习要点：自定义异常类
     *
     * 为什么定义专门的异常类？
     * 1. 类型安全 - 明确异常的来源和类型
     * 2. 错误处理 - 便于上层代码的异常处理
     * 3. 调试便利 - 提供详细的错误信息
     * 4. 业务语义 - 异常名称体现业务含义
     */

    /**
     * 翻译API异常
     */
    class TranslationApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * 语言检测异常
     */
    class LanguageDetectionException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * 数据转换异常
     */
    class DataMappingException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * 🎓 学习要点：object中的常量和工具方法
     *
     * 在object中不需要companion object，直接定义即可
     */
    /** 默认翻译服务提供商 */
    const val DEFAULT_PROVIDER = "baidu"

    /** 签名算法类型 */
    const val SIGNATURE_ALGORITHM = "MD5"

    /**
     * 验证语言代码格式
     *
     * @param code 语言代码
     * @return 是否为有效格式
     */
    fun isValidLanguageCode(code: String): Boolean {
        return code.matches(Regex("^[a-z]{2,3}$")) || code == "auto"
    }

    /**
     * 清理翻译文本
     *
     * 🎯 设计考虑：
     * - 去除多余的空白字符
     * - 处理特殊字符和编码问题
     * - 统一文本格式
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    fun cleanTranslatedText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ") // 合并多个空白字符
            .replace(Regex("[\r\n]+"), "\n") // 规范换行符
    }

    /**
     * 验证翻译结果的完整性
     *
     * @param result 翻译结果
     * @return 验证错误信息，成功时返回null
     */
    fun validateTranslationResult(result: TranslationResult): String? {
        return when {
            result.translatedText.isBlank() -> "翻译结果为空"
            result.input.isEmpty() -> "输入内容为空"
            result.sourceLanguage == result.targetLanguage && !result.sourceLanguage.isAutoDetect() -> "源语言和目标语言相同"
            else -> null
        }
    }
}
