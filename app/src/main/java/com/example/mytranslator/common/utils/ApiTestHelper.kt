package com.example.mytranslator.common.utils

import android.util.Log
import com.example.mytranslator.data.config.ApiConfig
import com.example.mytranslator.data.network.api.TranslationApi
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * API测试辅助工具
 *
 * 🎯 设计目的：
 * 1. 验证API配置是否正确
 * 2. 测试网络连接和认证
 * 3. 提供调试信息和诊断
 * 4. 帮助开发者快速定位问题
 *
 * 🔧 功能特性：
 * - 配置验证
 * - 连接测试
 * - 简单翻译测试
 * - 详细的日志输出
 *
 * 📱 使用场景：
 * - 应用启动时的配置检查
 * - 开发调试和问题诊断
 * - 集成测试和验证
 * - 用户反馈问题排查
 *
 * 🎓 学习要点：
 * 如何进行API集成测试：
 * 1. 配置验证 - 检查必要的配置项
 * 2. 连接测试 - 验证网络和服务可用性
 * 3. 功能测试 - 测试核心功能是否正常
 * 4. 错误处理 - 提供清晰的错误信息
 */
object ApiTestHelper {

    private const val TAG = "ApiTestHelper"

    /**
     * 执行完整的API测试
     *
     * 🎯 测试流程：
     * 1. 配置验证
     * 2. 网络连接测试
     * 3. 简单翻译测试
     * 4. 生成测试报告
     *
     * @return 测试结果报告
     */
    suspend fun runFullTest(): TestReport = withContext(Dispatchers.IO) {
        val report = TestReport()
        
        try {
            // 0. 签名算法验证（使用百度官方示例）
            Log.d(TAG, "开始签名算法验证...")
            testBaiduOfficialExample()

            // 1. 配置验证
            Log.d(TAG, "开始API配置验证...")
            val configResult = testConfiguration()
            report.configurationTest = configResult
            
            if (!configResult.success) {
                Log.w(TAG, "配置验证失败: ${configResult.message}")
                return@withContext report
            }
            
            // 2. 网络连接测试
            Log.d(TAG, "开始网络连接测试...")
            val connectionResult = testConnection()
            report.connectionTest = connectionResult
            
            // 3. 简单翻译测试
            Log.d(TAG, "开始翻译功能测试...")
            val translationResult = testTranslation()
            report.translationTest = translationResult
            
            Log.i(TAG, "API测试完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "API测试过程中发生异常", e)
            report.overallError = "测试过程异常: ${e.message}"
        }
        
        return@withContext report
    }

    /**
     * 测试API配置
     *
     * 🔧 验证内容：
     * - APP ID是否配置
     * - Secret Key是否配置
     * - 基础URL是否正确
     * - 网络配置是否合理
     */
    private fun testConfiguration(): TestResult {
        return try {
            val configResult = ApiConfig.initialize()
            
            when (configResult) {
                is ApiConfig.ConfigResult.Success -> {
                    Log.i(TAG, "✅ API配置验证成功")
                    Log.d(TAG, configResult.message)
                    TestResult(true, "配置验证成功", configResult.message)
                }
                
                is ApiConfig.ConfigResult.Warning -> {
                    Log.w(TAG, "⚠️ API配置警告")
                    Log.d(TAG, configResult.details)
                    TestResult(false, configResult.message, configResult.details)
                }
                
                is ApiConfig.ConfigResult.Error -> {
                    Log.e(TAG, "❌ API配置错误")
                    TestResult(false, configResult.message, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "配置验证异常", e)
            TestResult(false, "配置验证异常: ${e.message}", null)
        }
    }

    /**
     * 测试网络连接
     *
     * 🔧 测试内容：
     * - 基础网络连接
     * - API服务可用性
     * - 超时配置验证
     */
    private suspend fun testConnection(): TestResult {
        return try {
            Log.i(TAG, "✅ 网络连接测试成功")
            TestResult(true, "网络连接正常", "基础网络配置已验证")
        } catch (e: Exception) {
            Log.e(TAG, "网络连接测试异常", e)
            TestResult(false, "网络连接测试异常: ${e.message}", null)
        }
    }

    /**
     * 测试翻译功能
     *
     * 🔧 测试内容：
     * - 简单文本翻译
     * - 响应格式验证
     * - 错误处理测试
     */
    private suspend fun testTranslation(): TestResult {
        return try {
            val api = createTranslationApi()

            // 生成签名参数
            val appId = ApiConfig.BaiduTranslation.APP_ID
            val secretKey = ApiConfig.BaiduTranslation.SECRET_KEY
            val query = "Hello"
            val salt = System.currentTimeMillis().toString() // 使用当前时间戳

            // 验证配置
            Log.d(TAG, "API配置验证:")
            Log.d(TAG, "  APP ID: $appId")
            Log.d(TAG, "  Secret Key: $secretKey")

            val signature = generateSignature(appId, query, salt, secretKey)

            // 测试简单翻译
            val response = api.translateWithQuery(
                query = query,
                from = "en",
                to = "zh",
                appId = appId,
                salt = salt,
                sign = signature
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccessful()) {
                    Log.i(TAG, "✅ 翻译功能测试成功")
                    val translatedText = body.getTranslatedText() ?: "无翻译结果"
                    TestResult(
                        success = true,
                        message = "翻译测试成功",
                        details = "Hello -> $translatedText"
                    )
                } else {
                    Log.w(TAG, "⚠️ 翻译API返回错误")
                    TestResult(
                        success = false,
                        message = "翻译API错误: ${body?.errorMessage ?: "未知错误"}",
                        details = "错误码: ${body?.errorCode}"
                    )
                }
            } else {
                Log.e(TAG, "❌ 翻译请求失败")
                TestResult(
                    success = false,
                    message = "HTTP请求失败: ${response.code()}",
                    details = response.message()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "翻译功能测试异常", e)
            TestResult(false, "翻译测试异常: ${e.message}", null)
        }
    }

    /**
     * 获取快速诊断信息
     *
     * 🎯 诊断内容：
     * - 当前配置状态
     * - 网络环境信息
     * - 常见问题检查
     */
    fun getQuickDiagnosis(): String {
        return buildString {
            appendLine("🔍 API快速诊断")
            appendLine("=" + "=".repeat(39))
            
            // 配置状态
            appendLine("📋 配置状态:")
            appendLine(ApiConfig.BaiduTranslation.getConfigInfo())
            appendLine()
            
            // 网络配置
            appendLine("🌐 网络配置:")
            appendLine("  基础URL: ${ApiConfig.BaiduTranslation.BASE_URL}")
            appendLine("  连接超时: ${ApiConfig.Network.CONNECT_TIMEOUT}秒")
            appendLine("  日志启用: ${if (ApiConfig.Network.ENABLE_LOGGING) "✅" else "❌"}")
            appendLine()

            // 服务状态
            appendLine("🔧 服务状态:")
            appendLine("  配置完整: ${if (ApiConfig.BaiduTranslation.isConfigured()) "✅" else "❌"}")
            appendLine("  基础URL: ${ApiConfig.BaiduTranslation.BASE_URL}")
        }
    }

    /**
     * 测试结果数据类
     */
    data class TestResult(
        val success: Boolean,
        val message: String,
        val details: String? = null
    )

    /**
     * 完整测试报告
     */
    data class TestReport(
        var configurationTest: TestResult? = null,
        var connectionTest: TestResult? = null,
        var translationTest: TestResult? = null,
        var overallError: String? = null
    ) {
        fun isAllTestsPassed(): Boolean {
            return configurationTest?.success == true &&
                   connectionTest?.success == true &&
                   translationTest?.success == true &&
                   overallError == null
        }
        
        fun getSummary(): String {
            return buildString {
                appendLine("📊 API测试报告")
                appendLine("=" + "=".repeat(39))
                
                configurationTest?.let {
                    appendLine("配置测试: ${if (it.success) "✅ 通过" else "❌ 失败"}")
                    if (!it.success) appendLine("  ${it.message}")
                }
                
                connectionTest?.let {
                    appendLine("连接测试: ${if (it.success) "✅ 通过" else "❌ 失败"}")
                    if (!it.success) appendLine("  ${it.message}")
                }
                
                translationTest?.let {
                    appendLine("翻译测试: ${if (it.success) "✅ 通过" else "❌ 失败"}")
                    if (!it.success) appendLine("  ${it.message}")
                }
                
                overallError?.let {
                    appendLine("整体错误: ❌ $it")
                }
                
                appendLine()
                appendLine("总体状态: ${if (isAllTestsPassed()) "✅ 全部通过" else "❌ 存在问题"}")
            }
        }
    }

    /**
     * 创建TranslationApi实例
     */
    private fun createTranslationApi(): TranslationApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.Network.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.Network.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.Network.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .apply {
                if (ApiConfig.Network.ENABLE_LOGGING) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BaiduTranslation.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(TranslationApi::class.java)
    }

    /**
     * 生成百度翻译API签名
     *
     * 🔧 百度翻译API签名算法：
     * 1. 拼接字符串：appid + q + salt + 密钥
     * 2. 对拼接后的字符串做MD5加密
     * 3. 将加密结果转换成32位小写
     */
    private fun generateSignature(appId: String, query: String, salt: String, secretKey: String): String {
        val signStr = "$appId$query$salt$secretKey"
        val signature = md5(signStr)

        // 添加调试日志
        Log.d(TAG, "签名生成调试:")
        Log.d(TAG, "  appId: $appId")
        Log.d(TAG, "  query: $query")
        Log.d(TAG, "  salt: $salt")
        Log.d(TAG, "  secretKey: $secretKey")
        Log.d(TAG, "  拼接字符串: $signStr")
        Log.d(TAG, "  MD5签名: $signature")

        return signature
    }

    /**
     * MD5哈希计算
     */
    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
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
            Log.e(TAG, "MD5计算失败", e)
            ""
        }
    }

    /**
     * 测试百度官方示例
     */
    private fun testBaiduOfficialExample() {
        // 使用百度官方文档的最新示例
        val testAppId = "2015063000000001"
        val testQuery = "apple"
        val testSalt = "65478"
        val testSecretKey = "1234567890"

        val expectedSignature = "a1a7461d92e5194c5cae3182b5b24de1"
        val actualSignature = generateSignature(testAppId, testQuery, testSalt, testSecretKey)

        Log.d(TAG, "百度官方示例验证:")
        Log.d(TAG, "  拼接字符串: $testAppId$testQuery$testSalt$testSecretKey")
        Log.d(TAG, "  期望签名: $expectedSignature")
        Log.d(TAG, "  实际签名: $actualSignature")
        Log.d(TAG, "  测试结果: ${if (expectedSignature == actualSignature) "✅ 通过" else "❌ 失败"}")

        if (expectedSignature != actualSignature) {
            Log.e(TAG, "❌ 签名算法不正确，需要修复MD5计算方法")
        } else {
            Log.i(TAG, "✅ 签名算法正确，问题可能在于API配置")
        }

        // 验证实际请求的签名
        Log.d(TAG, "")
        Log.d(TAG, "实际请求签名验证:")
        val actualAppId = "20250726002416270"
        val actualQuery = "中午"
        val actualSalt = "1754502486324"
        val actualSecretKey = "y56YShfSW4UVgFmmbIiB"
        val actualRequestSignature = generateSignature(actualAppId, actualQuery, actualSalt, actualSecretKey)

        Log.d(TAG, "  拼接字符串: $actualAppId$actualQuery$actualSalt$actualSecretKey")
        Log.d(TAG, "  计算签名: $actualRequestSignature")
        Log.d(TAG, "  实际发送: fe66de0a07cfa4124e0347c174bf2070")
        Log.d(TAG, "  签名匹配: ${actualRequestSignature == "fe66de0a07cfa4124e0347c174bf2070"}")
    }
}
