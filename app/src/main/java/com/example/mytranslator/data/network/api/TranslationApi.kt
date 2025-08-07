package com.example.mytranslator.data.network.api

import com.example.mytranslator.data.model.request.TranslationRequest
import com.example.mytranslator.data.model.response.TranslationResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * 翻译API接口
 *
 * 🎯 设计思想：
 * 1. Retrofit接口定义 - 声明式HTTP客户端
 * 2. 多API支持 - 设计通用的接口，支持不同翻译服务
 * 3. 错误处理 - 使用Response包装，便于处理HTTP错误
 * 4. 扩展性 - 为未来的API功能预留接口
 *
 * 🔧 技术特性：
 * - 使用Retrofit注解定义HTTP请求
 * - 支持POST和GET请求方式
 * - 自动JSON序列化和反序列化
 * - 协程支持的异步调用
 *
 * 📱 使用场景：
 * - Repository实现中的网络调用
 * - 不同翻译服务的API适配
 * - 网络请求的统一管理
 * - API版本和兼容性处理
 *
 * 🎓 学习要点：
 * Retrofit接口设计原则：
 * 1. 声明式 - 通过注解声明HTTP请求
 * 2. 类型安全 - 编译时检查参数和返回类型
 * 3. 异步优先 - 使用suspend函数支持协程
 * 4. 错误处理 - 使用Response包装处理HTTP状态码
 */
interface TranslationApi {

    /**
     * 执行翻译（POST请求）
     *
     * 🎯 设计考虑：
     * - 使用POST请求支持长文本翻译
     * - JSON格式传输，支持复杂参数
     * - 适用于大多数现代翻译API
     *
     * @param request 翻译请求对象
     * @return 翻译响应，包装在Response中
     */
    @POST("api/trans/vip/translate")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    suspend fun translate(@Body request: TranslationRequest): Response<TranslationResponse>

    /**
     * 执行翻译（GET请求）
     *
     * 🎯 设计考虑：
     * - 某些API只支持GET请求
     * - 参数通过URL查询字符串传递
     * - 适用于简单的翻译请求
     *
     * @param query 要翻译的文本
     * @param from 源语言代码
     * @param to 目标语言代码
     * @param appId 应用ID（可选）
     * @param salt 时间戳（可选）
     * @param sign 签名（可选）
     * @return 翻译响应，包装在Response中
     */
    @GET("api/trans/vip/translate")
    suspend fun translateWithQuery(
        @Query("q") query: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("appid") appId: String? = null,
        @Query("salt") salt: String? = null,
        @Query("sign") sign: String? = null
    ): Response<TranslationResponse>

    /**
     * 检测语言
     *
     * 🎯 设计考虑：
     * - 自动检测输入文本的语言
     * - 为翻译提供准确的源语言
     * - 支持多语言检测
     *
     * @param text 要检测的文本
     * @param appId 应用ID（可选）
     * @return 语言检测响应
     */
    @POST("api/trans/vip/language")
    @FormUrlEncoded
    suspend fun detectLanguage(
        @Field("q") text: String,
        @Field("appid") appId: String? = null
    ): Response<LanguageDetectionResponse>

    /**
     * 获取支持的语言列表
     *
     * 🎯 设计考虑：
     * - 动态获取API支持的语言
     * - 支持语言列表的更新
     * - 为语言选择提供数据源
     *
     * @param appId 应用ID（可选）
     * @return 支持的语言列表响应
     */
    @GET("api/trans/vip/languagelist")
    suspend fun getSupportedLanguages(
        @Query("appid") appId: String? = null
    ): Response<SupportedLanguagesResponse>

    /**
     * 批量翻译
     *
     * 🎯 设计考虑：
     * - 支持多个文本的批量翻译
     * - 提高翻译效率
     * - 减少API调用次数
     *
     * @param requests 批量翻译请求列表
     * @return 批量翻译响应
     */
    @POST("api/trans/vip/batch")
    @Headers("Content-Type: application/json")
    suspend fun batchTranslate(
        @Body requests: BatchTranslationRequest
    ): Response<BatchTranslationResponse>

    /**
     * 获取API使用统计
     *
     * 🎯 设计考虑：
     * - 查询API使用情况
     * - 监控配额和成本
     * - 用于使用分析
     *
     * @param appId 应用ID
     * @param startDate 开始日期（YYYYMMDD格式）
     * @param endDate 结束日期（YYYYMMDD格式）
     * @return API使用统计响应
     */
    @GET("api/trans/vip/usage")
    suspend fun getUsageStatistics(
        @Query("appid") appId: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<UsageStatisticsResponse>

    /**
     * 🎓 学习要点：数据类定义
     *
     * 为什么在API接口中定义响应数据类？
     * 1. 接口完整性 - 保持API接口的自包含性
     * 2. 类型安全 - 确保响应数据的类型正确
     * 3. 文档作用 - 清晰地描述API响应格式
     */

    /**
     * 语言检测响应
     */
    data class LanguageDetectionResponse(
        @com.google.gson.annotations.SerializedName("error_code")
        val errorCode: String? = null,
        
        @com.google.gson.annotations.SerializedName("error_msg")
        val errorMessage: String? = null,
        
        @com.google.gson.annotations.SerializedName("data")
        val data: LanguageDetectionData? = null
    ) {
        data class LanguageDetectionData(
            @com.google.gson.annotations.SerializedName("src")
            val detectedLanguage: String
        )
        
        fun isSuccessful(): Boolean = errorCode == null || errorCode == "52000"
        fun getDetectedLanguage(): String? = data?.detectedLanguage
    }

    /**
     * 支持的语言列表响应
     */
    data class SupportedLanguagesResponse(
        @com.google.gson.annotations.SerializedName("error_code")
        val errorCode: String? = null,
        
        @com.google.gson.annotations.SerializedName("error_msg")
        val errorMessage: String? = null,
        
        @com.google.gson.annotations.SerializedName("data")
        val languages: Map<String, String>? = null
    ) {
        fun isSuccessful(): Boolean = errorCode == null || errorCode == "52000"
        fun getLanguageMap(): Map<String, String> = languages ?: emptyMap()
    }

    /**
     * 批量翻译请求
     */
    data class BatchTranslationRequest(
        @com.google.gson.annotations.SerializedName("requests")
        val requests: List<TranslationRequest>
    )

    /**
     * 批量翻译响应
     */
    data class BatchTranslationResponse(
        @com.google.gson.annotations.SerializedName("error_code")
        val errorCode: String? = null,
        
        @com.google.gson.annotations.SerializedName("error_msg")
        val errorMessage: String? = null,
        
        @com.google.gson.annotations.SerializedName("results")
        val results: List<TranslationResponse>? = null
    ) {
        fun isSuccessful(): Boolean = errorCode == null || errorCode == "52000"
        fun getResultsList(): List<TranslationResponse> = results ?: emptyList()
    }

    /**
     * API使用统计响应
     */
    data class UsageStatisticsResponse(
        @com.google.gson.annotations.SerializedName("error_code")
        val errorCode: String? = null,
        
        @com.google.gson.annotations.SerializedName("error_msg")
        val errorMessage: String? = null,
        
        @com.google.gson.annotations.SerializedName("data")
        val data: UsageData? = null
    ) {
        data class UsageData(
            @com.google.gson.annotations.SerializedName("total_chars")
            val totalCharacters: Long,
            
            @com.google.gson.annotations.SerializedName("total_requests")
            val totalRequests: Int,
            
            @com.google.gson.annotations.SerializedName("remaining_quota")
            val remainingQuota: Long
        )
        
        fun isSuccessful(): Boolean = errorCode == null || errorCode == "52000"
        fun getUsageData(): UsageData? = data
    }

    /**
     * 🎓 学习要点：伴生对象的常量定义
     */
    companion object {
        /** 百度翻译API基础URL */
        const val BAIDU_BASE_URL = "https://fanyi-api.baidu.com/"
        
        /** Google翻译API基础URL */
        const val GOOGLE_BASE_URL = "https://translation.googleapis.com/"
        
        /** 默认超时时间（秒） */
        const val DEFAULT_TIMEOUT_SECONDS = 30L
        
        /** 最大重试次数 */
        const val MAX_RETRY_COUNT = 3
        
        /** 批量翻译最大数量 */
        const val MAX_BATCH_SIZE = 10

        /**
         * 创建百度翻译API的基础URL
         */
        fun createBaiduBaseUrl(): String = BAIDU_BASE_URL

        /**
         * 创建Google翻译API的基础URL
         */
        fun createGoogleBaseUrl(): String = GOOGLE_BASE_URL

        /**
         * 验证批量请求大小
         */
        fun validateBatchSize(size: Int): Boolean = size in 1..MAX_BATCH_SIZE
    }
}
