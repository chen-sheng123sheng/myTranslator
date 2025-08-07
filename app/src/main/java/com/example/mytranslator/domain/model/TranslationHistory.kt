package com.example.mytranslator.domain.model

import java.util.UUID

/**
 * 翻译历史记录领域模型
 *
 * 🎯 设计目的：
 * 1. 表示翻译历史记录的业务概念
 * 2. 封装翻译记录的核心属性和行为
 * 3. 提供类型安全的数据操作
 * 4. 支持业务规则验证和计算
 *
 * 🏗️ 领域模型设计原则：
 * - 业务导向：反映真实的业务概念
 * - 不可变性：使用data class确保数据一致性
 * - 自包含：包含所有必要的业务属性
 * - 行为丰富：提供业务相关的计算和验证方法
 *
 * 📱 使用场景：
 * - 翻译结果的存储和展示
 * - 历史记录的搜索和筛选
 * - 收藏功能的管理
 * - 统计分析的数据源
 *
 * 🎓 学习要点：
 * 领域模型的设计要点：
 * 1. 业务语言 - 使用业务领域的术语
 * 2. 数据完整性 - 确保数据的有效性
 * 3. 行为封装 - 将相关的业务逻辑封装在模型中
 * 4. 层次分离 - 与数据库实体和UI模型分离
 */
data class TranslationHistory(
    /**
     * 唯一标识符
     * 使用UUID确保全局唯一性
     */
    val id: String = UUID.randomUUID().toString(),

    /**
     * 原始文本
     * 用户输入的待翻译文本
     */
    val originalText: String,

    /**
     * 翻译结果
     * 翻译服务返回的译文
     */
    val translatedText: String,

    /**
     * 源语言代码
     * 例如：en, zh, jp等
     */
    val sourceLanguageCode: String,

    /**
     * 目标语言代码
     * 例如：en, zh, jp等
     */
    val targetLanguageCode: String,

    /**
     * 源语言显示名称
     * 例如：English, 中文, 日本語等
     */
    val sourceLanguageName: String,

    /**
     * 目标语言显示名称
     * 例如：English, 中文, 日本語等
     */
    val targetLanguageName: String,

    /**
     * 翻译时间戳
     * Unix时间戳，毫秒级
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * 是否收藏
     * 用户标记的收藏状态
     */
    val isFavorite: Boolean = false,

    /**
     * 翻译服务提供商
     * 例如：baidu, google, youdao等
     */
    val translationProvider: String,

    /**
     * 翻译质量评分
     * 0.0-1.0，可选字段，用于质量评估
     */
    val qualityScore: Double? = null,

    /**
     * 使用次数
     * 记录该翻译被查看或使用的次数
     */
    val usageCount: Int = 0,

    /**
     * 最后访问时间
     * 记录最后一次查看该翻译的时间
     */
    val lastAccessTime: Long = timestamp,

    /**
     * 标签列表
     * 用户自定义的标签，用于分类和搜索
     */
    val tags: List<String> = emptyList(),

    /**
     * 备注信息
     * 用户添加的备注或说明
     */
    val notes: String? = null
) {

    /**
     * 检查翻译记录是否有效
     *
     * 🎯 业务规则验证：
     * - 原文和译文不能为空
     * - 语言代码必须有效
     * - 时间戳必须合理
     */
    fun isValid(): Boolean {
        return originalText.isNotBlank() &&
                translatedText.isNotBlank() &&
                sourceLanguageCode.isNotBlank() &&
                targetLanguageCode.isNotBlank() &&
                timestamp > 0 &&
                translationProvider.isNotBlank()
    }

    /**
     * 获取语言对描述
     *
     * @return 格式化的语言对字符串，例如："中文 → English"
     */
    fun getLanguagePairDescription(): String {
        return "$sourceLanguageName → $targetLanguageName"
    }

    /**
     * 获取简化的语言对代码
     *
     * @return 格式化的语言对代码，例如："zh-en"
     */
    fun getLanguagePairCode(): String {
        return "$sourceLanguageCode-$targetLanguageCode"
    }

    /**
     * 检查是否为今日翻译
     *
     * @return 如果是今天的翻译返回true
     */
    fun isToday(): Boolean {
        val today = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        return (today - timestamp) < dayInMillis
    }

    /**
     * 检查是否为本周翻译
     *
     * @return 如果是本周的翻译返回true
     */
    fun isThisWeek(): Boolean {
        val now = System.currentTimeMillis()
        val weekInMillis = 7 * 24 * 60 * 60 * 1000
        return (now - timestamp) < weekInMillis
    }

    /**
     * 检查是否为本月翻译
     *
     * @return 如果是本月的翻译返回true
     */
    fun isThisMonth(): Boolean {
        val now = System.currentTimeMillis()
        val monthInMillis = 30L * 24 * 60 * 60 * 1000
        return (now - timestamp) < monthInMillis
    }

    /**
     * 获取翻译文本的预览
     *
     * @param maxLength 最大长度，默认50个字符
     * @return 截断后的文本预览
     */
    fun getOriginalTextPreview(maxLength: Int = 50): String {
        return if (originalText.length <= maxLength) {
            originalText
        } else {
            originalText.take(maxLength) + "..."
        }
    }

    /**
     * 获取译文的预览
     *
     * @param maxLength 最大长度，默认50个字符
     * @return 截断后的译文预览
     */
    fun getTranslatedTextPreview(maxLength: Int = 50): String {
        return if (translatedText.length <= maxLength) {
            translatedText
        } else {
            translatedText.take(maxLength) + "..."
        }
    }

    /**
     * 检查是否包含指定的搜索关键词
     *
     * @param query 搜索关键词
     * @param ignoreCase 是否忽略大小写，默认true
     * @return 如果包含关键词返回true
     */
    fun containsQuery(query: String, ignoreCase: Boolean = true): Boolean {
        if (query.isBlank()) return true
        
        return originalText.contains(query, ignoreCase) ||
                translatedText.contains(query, ignoreCase) ||
                sourceLanguageName.contains(query, ignoreCase) ||
                targetLanguageName.contains(query, ignoreCase) ||
                tags.any { it.contains(query, ignoreCase) } ||
                notes?.contains(query, ignoreCase) == true
    }

    /**
     * 创建收藏状态切换后的副本
     *
     * @return 切换收藏状态后的新实例
     */
    fun toggleFavorite(): TranslationHistory {
        return copy(isFavorite = !isFavorite)
    }

    /**
     * 创建增加使用次数后的副本
     *
     * @return 使用次数+1且更新最后访问时间的新实例
     */
    fun incrementUsage(): TranslationHistory {
        return copy(
            usageCount = usageCount + 1,
            lastAccessTime = System.currentTimeMillis()
        )
    }

    /**
     * 创建添加标签后的副本
     *
     * @param tag 要添加的标签
     * @return 添加标签后的新实例
     */
    fun addTag(tag: String): TranslationHistory {
        if (tag.isBlank() || tags.contains(tag)) return this
        return copy(tags = tags + tag)
    }

    /**
     * 创建移除标签后的副本
     *
     * @param tag 要移除的标签
     * @return 移除标签后的新实例
     */
    fun removeTag(tag: String): TranslationHistory {
        return copy(tags = tags - tag)
    }

    /**
     * 创建更新备注后的副本
     *
     * @param newNotes 新的备注内容
     * @return 更新备注后的新实例
     */
    fun updateNotes(newNotes: String?): TranslationHistory {
        return copy(notes = newNotes?.takeIf { it.isNotBlank() })
    }

    companion object {
        /**
         * 创建一个用于测试的示例翻译记录
         */
        fun createSample(): TranslationHistory {
            return TranslationHistory(
                originalText = "Hello, world!",
                translatedText = "你好，世界！",
                sourceLanguageCode = "en",
                targetLanguageCode = "zh",
                sourceLanguageName = "English",
                targetLanguageName = "中文",
                translationProvider = "baidu"
            )
        }
    }
}
