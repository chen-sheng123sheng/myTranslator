package com.example.mytranslator.data.local

import com.example.mytranslator.data.local.entity.TranslationHistoryEntity
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/**
 * 🧪 简化的Room数据库配置验证测试
 *
 * 这个测试类用于验证我们的Room数据库Entity配置是否正确
 */
class TranslationDatabaseTest {

    /**
     * 测试1：验证Entity类的基本创建
     */
    @Test
    fun test_entity_creation() {
        // 🎯 测试目标：验证Entity类可以正常创建

        // 创建测试数据
        val testTranslation = TranslationHistoryEntity(
            id = UUID.randomUUID().toString(),
            originalText = "Hello World",
            translatedText = "你好世界",
            sourceLanguageCode = "en",
            targetLanguageCode = "zh",
            sourceLanguageName = "English",
            targetLanguageName = "Chinese",
            timestamp = System.currentTimeMillis(),
            isFavorite = false,
            translationProvider = "baidu"
        )

        // 验证Entity属性
        assertNotNull("ID不应该为空", testTranslation.id)
        assertEquals("原文应该匹配", "Hello World", testTranslation.originalText)
        assertEquals("译文应该匹配", "你好世界", testTranslation.translatedText)
        assertEquals("源语言代码应该匹配", "en", testTranslation.sourceLanguageCode)
        assertEquals("目标语言代码应该匹配", "zh", testTranslation.targetLanguageCode)
        assertFalse("默认不应该收藏", testTranslation.isFavorite)
        assertEquals("翻译提供商应该匹配", "baidu", testTranslation.translationProvider)

        println("✅ 测试1通过：Entity创建正常")
    }

    /**
     * 测试2：验证Entity的默认值和计算属性
     */
    @Test
    fun test_entity_default_values() {
        // 🎯 测试目标：验证Entity的默认值和计算属性

        val originalText = "Hello"
        val translatedText = "你好"

        val testTranslation = TranslationHistoryEntity(
            id = UUID.randomUUID().toString(),
            originalText = originalText,
            translatedText = translatedText,
            sourceLanguageCode = "en",
            targetLanguageCode = "zh",
            sourceLanguageName = "English",
            targetLanguageName = "Chinese",
            timestamp = System.currentTimeMillis()
            // 注意：isFavorite和translationProvider使用默认值
            // originalTextLength和translatedTextLength会自动计算
        )

        // 验证默认值
        assertFalse("默认不应该收藏", testTranslation.isFavorite)
        assertEquals("默认翻译提供商应该是baidu", "baidu", testTranslation.translationProvider)

        // 验证计算属性
        assertEquals("原文长度应该正确计算", originalText.length, testTranslation.originalTextLength)
        assertEquals("译文长度应该正确计算", translatedText.length, testTranslation.translatedTextLength)

        println("✅ 测试2通过：Entity默认值和计算属性正常")
    }

    /**
     * 测试3：验证Entity的数据完整性
     */
    @Test
    fun test_entity_data_integrity() {
        // 🎯 测试目标：验证Entity能正确处理各种数据类型

        val currentTime = System.currentTimeMillis()

        val testTranslation = TranslationHistoryEntity(
            id = "test-id-123",
            originalText = "Test with special chars: !@#$%^&*()",
            translatedText = "测试特殊字符：！@#￥%……&*（）",
            sourceLanguageCode = "en",
            targetLanguageCode = "zh",
            sourceLanguageName = "English",
            targetLanguageName = "中文",
            timestamp = currentTime,
            isFavorite = true,
            translationProvider = "custom_provider"
        )

        // 验证所有字段都正确设置
        assertEquals("ID应该匹配", "test-id-123", testTranslation.id)
        assertTrue("原文应该包含特殊字符", testTranslation.originalText.contains("!@#$%"))
        assertTrue("译文应该包含中文特殊字符", testTranslation.translatedText.contains("！@#￥"))
        assertEquals("时间戳应该匹配", currentTime, testTranslation.timestamp)
        assertTrue("应该被收藏", testTranslation.isFavorite)
        assertEquals("自定义提供商应该匹配", "custom_provider", testTranslation.translationProvider)

        println("✅ 测试3通过：Entity数据完整性正常")
    }
}

/**
 * 🎓 简化测试设计学习要点：
 *
 * 1. 基础验证：
 *    - Entity类的基本创建和属性设置
 *    - 默认值的正确应用
 *    - 计算属性的自动计算
 *
 * 2. 数据完整性：
 *    - 各种数据类型的正确处理
 *    - 特殊字符的支持
 *    - 时间戳的精确性
 *
 * 3. 测试策略：
 *    - 从简单到复杂逐步验证
 *    - 先确保基础配置正确
 *    - 再进行复杂的数据库操作测试
 */
