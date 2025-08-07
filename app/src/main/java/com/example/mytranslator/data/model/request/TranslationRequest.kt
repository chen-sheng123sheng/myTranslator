package com.example.mytranslator.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * 翻译请求数据模型
 *
 * 🎯 设计思想：
 * 1. API数据模型 - 专门用于网络请求的数据结构
 * 2. 与Domain模型分离 - 避免API变化影响业务逻辑
 * 3. JSON序列化支持 - 使用Gson注解进行字段映射
 * 4. 多API兼容 - 设计通用的请求格式
 *
 * 🔧 技术特性：
 * - 使用@SerializedName注解处理字段映射
 * - 支持可选参数和默认值
 * - 便于API参数的验证和处理
 * - 支持不同翻译服务的参数适配
 *
 * 📱 使用场景：
 * - Retrofit网络请求的参数对象
 * - API调用前的数据准备
 * - 不同翻译服务的参数转换
 * - 请求日志和调试信息
 *
 * 🎓 学习要点：
 * 为什么需要单独的请求模型？
 * 1. 关注点分离 - API格式与业务模型分离
 * 2. 版本兼容 - API变化不影响Domain层
 * 3. 多服务支持 - 不同API有不同的参数格式
 * 4. 序列化控制 - 精确控制JSON字段映射
 */
data class TranslationRequest(
    /**
     * 要翻译的文本内容
     *
     * 🎯 设计考虑：
     * - 使用通用字段名，适配多种翻译API
     * - 支持长文本翻译
     * - 自动处理特殊字符和编码
     */
    @SerializedName("q")
    val query: String,

    /**
     * 源语言代码
     *
     * 🎯 设计考虑：
     * - 使用ISO 639-1标准代码
     * - 支持"auto"自动检测
     * - 与Domain层Language模型的code字段对应
     */
    @SerializedName("from")
    val sourceLanguage: String,

    /**
     * 目标语言代码
     *
     * 🎯 设计考虑：
     * - 必须是具体的语言代码，不能是"auto"
     * - 与Domain层Language模型的code字段对应
     */
    @SerializedName("to")
    val targetLanguage: String,

    /**
     * API密钥（可选）
     *
     * 🎯 设计考虑：
     * - 某些API需要在请求体中包含密钥
     * - 支持动态密钥配置
     * - 安全性考虑：不在日志中显示
     */
    @SerializedName("appid")
    val appId: String? = null,

    /**
     * 时间戳（用于签名）
     *
     * 🎯 设计考虑：
     * - 某些API需要时间戳进行签名验证
     * - 防止重放攻击
     * - 自动生成当前时间戳
     */
    @SerializedName("salt")
    val salt: String? = null,

    /**
     * 签名（用于API验证）
     *
     * 🎯 设计考虑：
     * - API安全验证机制
     * - 基于密钥、查询内容、时间戳生成
     * - 防止API滥用
     */
    @SerializedName("sign")
    val signature: String? = null,

    /**
     * 翻译质量等级（可选）
     *
     * 🎯 设计考虑：
     * - 某些API支持质量等级选择
     * - 平衡翻译质量和响应速度
     * - 对应Domain层的QualityLevel
     */
    @SerializedName("quality")
    val quality: String? = null,

    /**
     * 翻译领域（可选）
     *
     * 🎯 设计考虑：
     * - 专业领域翻译优化
     * - 提高特定领域的翻译准确性
     * - 如：医学、法律、技术等
     */
    @SerializedName("domain")
    val domain: String? = null,

    /**
     * 客户端类型标识
     *
     * 🎯 设计考虑：
     * - API统计和分析
     * - 客户端版本追踪
     * - 服务质量监控
     */
    @SerializedName("client")
    val clientType: String = "android",

    /**
     * API版本号
     *
     * 🎯 设计考虑：
     * - API版本兼容性
     * - 功能特性控制
     * - 向后兼容支持
     */
    @SerializedName("version")
    val apiVersion: String = "1.0"
) {

    /**
     * 🎓 学习要点：数据验证方法
     *
     * 为什么在数据模型中添加验证？
     * 1. 早期错误发现 - 在网络请求前验证
     * 2. API要求检查 - 确保符合API规范
     * 3. 调试便利 - 提供详细的错误信息
     */

    /**
     * 验证请求参数是否有效
     *
     * 🎯 设计考虑：
     * - 在发送网络请求前进行验证
     * - 提供详细的错误信息
     * - 避免无效请求浪费资源
     *
     * @return 验证结果，成功返回null，失败返回错误信息
     */
    fun validate(): String? {
        return when {
            query.isBlank() -> "翻译内容不能为空"
            query.length > MAX_QUERY_LENGTH -> "翻译内容超过${MAX_QUERY_LENGTH}字符限制"
            sourceLanguage.isBlank() -> "源语言不能为空"
            targetLanguage.isBlank() -> "目标语言不能为空"
            targetLanguage == "auto" -> "目标语言不能设置为自动检测"
            sourceLanguage == targetLanguage && sourceLanguage != "auto" -> "源语言和目标语言不能相同"
            else -> null // 验证通过
        }
    }

    /**
     * 检查是否需要API签名
     *
     * 🎯 设计考虑：
     * - 某些API需要签名验证
     * - 根据API类型决定是否需要签名
     *
     * @return 是否需要签名
     */
    fun requiresSignature(): Boolean {
        return appId != null && salt != null
    }

    /**
     * 获取用于签名的字符串
     *
     * 🎯 设计考虑：
     * - 生成API签名所需的原始字符串
     * - 按照API规范的顺序拼接参数
     * - 用于MD5或其他哈希算法
     *
     * @param secretKey API密钥
     * @return 签名原始字符串
     */
    fun getSignatureString(secretKey: String): String {
        return buildString {
            append(appId ?: "")
            append(query)
            append(salt ?: "")
            append(secretKey)
        }
    }

    /**
     * 转换为查询参数Map
     *
     * 🎯 设计考虑：
     * - 某些API使用GET请求，需要查询参数
     * - 过滤空值参数
     * - 便于URL构建
     *
     * @return 查询参数Map
     */
    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        
        map["q"] = query
        map["from"] = sourceLanguage
        map["to"] = targetLanguage
        map["client"] = clientType
        map["version"] = apiVersion
        
        appId?.let { map["appid"] = it }
        salt?.let { map["salt"] = it }
        signature?.let { map["sign"] = it }
        quality?.let { map["quality"] = it }
        domain?.let { map["domain"] = it }
        
        return map
    }

    /**
     * 获取请求摘要（用于日志和缓存键）
     *
     * 🎯 设计考虑：
     * - 生成请求的唯一标识
     * - 用于缓存键生成
     * - 日志记录和调试
     * - 不包含敏感信息
     *
     * @return 请求摘要字符串
     */
    fun getSummary(): String {
        return "TranslationRequest(${sourceLanguage}→${targetLanguage}, ${query.take(50)}${if (query.length > 50) "..." else ""})"
    }

    /**
     * 创建安全的日志字符串
     *
     * 🎯 设计考虑：
     * - 隐藏敏感信息（API密钥、签名）
     * - 保留调试所需的关键信息
     * - 符合隐私保护要求
     *
     * @return 安全的日志字符串
     */
    fun toSafeLogString(): String {
        return "TranslationRequest(" +
                "query='${query.take(100)}${if (query.length > 100) "..." else ""}', " +
                "from='$sourceLanguage', " +
                "to='$targetLanguage', " +
                "appId='${appId?.take(8)}***', " +
                "hasSignature=${signature != null}" +
                ")"
    }

    /**
     * 🎓 学习要点：伴生对象的常量定义
     */
    companion object {
        /** 最大查询长度 */
        const val MAX_QUERY_LENGTH = 5000
        
        /** 默认客户端类型 */
        const val DEFAULT_CLIENT_TYPE = "android"
        
        /** 默认API版本 */
        const val DEFAULT_API_VERSION = "1.0"
        
        /** 自动检测语言代码 */
        const val AUTO_DETECT_LANGUAGE = "auto"

        /**
         * 创建简单的翻译请求
         *
         * 🎯 设计考虑：
         * - 提供便捷的创建方法
         * - 适用于基本翻译场景
         * - 减少样板代码
         *
         * @param query 翻译内容
         * @param from 源语言
         * @param to 目标语言
         * @return 翻译请求对象
         */
        fun createSimple(query: String, from: String, to: String): TranslationRequest {
            return TranslationRequest(
                query = query,
                sourceLanguage = from,
                targetLanguage = to
            )
        }

        /**
         * 创建带签名的翻译请求
         *
         * @param query 翻译内容
         * @param from 源语言
         * @param to 目标语言
         * @param appId 应用ID
         * @param salt 时间戳
         * @param signature 签名
         * @return 翻译请求对象
         */
        fun createWithSignature(
            query: String,
            from: String,
            to: String,
            appId: String,
            salt: String,
            signature: String
        ): TranslationRequest {
            return TranslationRequest(
                query = query,
                sourceLanguage = from,
                targetLanguage = to,
                appId = appId,
                salt = salt,
                signature = signature
            )
        }
    }
}
