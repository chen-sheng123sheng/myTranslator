package com.example.mytranslator.domain.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * 翻译结果数据模型
 *
 * 🎯 设计思想：
 * 1. 完整的翻译会话记录，不仅仅是结果文本
 * 2. 支持翻译历史、缓存、分享等多种功能需求
 * 3. 保持输入和输出的完整关联关系
 * 4. 提供丰富的元数据用于质量评估和用户体验
 *
 * 🔧 技术特性：
 * - 不可变数据结构，确保线程安全
 * - 包含完整的翻译上下文信息
 * - 支持不同输入类型的统一处理
 * - 提供实用方法简化常见操作
 *
 * 📱 使用场景：
 * - 翻译结果的UI展示
 * - 翻译历史记录的存储
 * - 翻译缓存的键值对应
 * - 分享功能的数据源
 * - 翻译质量的评估和统计
 *
 * 🎓 学习要点：
 * 为什么包含输入信息？
 * 1. 历史记录需要显示原文和译文
 * 2. 缓存机制需要匹配相同的输入
 * 3. 重新翻译需要访问原始数据
 * 4. 分享功能需要完整的翻译上下文
 */
data class TranslationResult(
    /**
     * 原始翻译输入
     *
     * 🎯 设计考虑：
     * - 保持输入和输出的完整关联
     * - 支持不同类型输入的统一处理
     * - 为缓存和历史记录提供查找键
     */
    val input: TranslationInput,

    /**
     * 翻译结果文本
     *
     * 🎯 设计考虑：
     * - 翻译的核心输出内容
     * - 用于UI显示和用户交互
     * - 支持复制、分享等操作
     */
    val translatedText: String,

    /**
     * 实际的源语言
     *
     * 🎯 设计考虑：
     * - 可能与用户选择的源语言不同（自动检测结果）
     * - 用于显示实际检测到的语言
     * - 帮助用户了解翻译的准确性
     */
    val sourceLanguage: Language,

    /**
     * 目标语言
     *
     * 🎯 设计考虑：
     * - 用户选择或系统默认的目标语言
     * - 用于语言切换和历史记录
     * - 支持翻译方向的显示
     */
    val targetLanguage: Language,

    /**
     * 翻译时间戳
     *
     * 🎯 设计考虑：
     * - 支持历史记录的时间排序
     * - 用于缓存过期判断
     * - 提供翻译会话的时间信息
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * 翻译置信度（可选）
     *
     * 🎯 设计考虑：
     * - 某些翻译API提供质量评分
     * - 帮助用户判断翻译结果的可靠性
     * - 用于翻译质量的统计和改进
     * - 范围：0.0-1.0，越高表示越可靠
     */
    val confidence: Float? = null,

    /**
     * 翻译服务提供商（可选）
     *
     * 🎯 设计考虑：
     * - 记录使用的翻译服务（百度、谷歌等）
     * - 用于服务质量比较和统计
     * - 支持多翻译源的切换和对比
     */
    val provider: String? = null,

    /**
     * 翻译耗时（毫秒，可选）
     *
     * 🎯 设计考虑：
     * - 用于性能监控和优化
     * - 帮助用户了解翻译速度
     * - 支持服务质量评估
     */
    val durationMs: Long? = null
) {

    companion object {
        /** 缓存有效期（毫秒） */
        const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24小时

        /** 高置信度阈值 */
        const val HIGH_CONFIDENCE_THRESHOLD = 0.8f

        /** 低置信度阈值 */
        const val LOW_CONFIDENCE_THRESHOLD = 0.5f

        /** 日期格式化器 */
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    /**
     * 获取原始文本内容
     *
     * 🎯 设计考虑：
     * - 统一处理不同类型的输入
     * - 为UI显示提供原文内容
     * - 支持历史记录和分享功能
     *
     * @return 原始文本内容
     */
    fun getOriginalText(): String {
        return when (input) {
            is TranslationInput.Text -> input.content
            is TranslationInput.Voice -> "[语音输入 ${input.durationMs/1000}秒]"
            is TranslationInput.Image -> "[图片输入]"
        }
    }

    /**
     * 获取输入类型的显示名称
     *
     * @return 输入类型名称
     */
    fun getInputTypeName(): String {
        return input.getTypeName()
    }

    /**
     * 获取翻译方向的显示文本
     *
     * 🎯 设计考虑：
     * - 为UI提供"中文 → 英文"格式的显示
     * - 支持语言切换按钮的状态显示
     *
     * @return 翻译方向文本
     */
    fun getTranslationDirection(): String {
        return "${sourceLanguage.displayName} → ${targetLanguage.displayName}"
    }

    /**
     * 获取格式化的翻译时间
     *
     * 🎯 设计考虑：
     * - 为历史记录提供用户友好的时间显示
     * - 支持不同的时间格式需求
     *
     * @return 格式化的时间字符串
     */
    fun getFormattedTime(): String {
        return dateFormatter.format(Date(timestamp))
    }

    /**
     * 获取相对时间描述
     *
     * 🎯 设计考虑：
     * - 提供"刚刚"、"5分钟前"等相对时间
     * - 更符合用户的时间感知习惯
     *
     * @return 相对时间描述
     */
    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 2592000_000 -> "${diff / 86400_000}天前"
            else -> getFormattedTime()
        }
    }

    /**
     * 检查缓存是否仍然有效
     *
     * 🎯 设计考虑：
     * - 避免过期缓存的使用
     * - 平衡性能和数据新鲜度
     *
     * @return 缓存是否有效
     */
    fun isCacheValid(): Boolean {
        val now = System.currentTimeMillis()
        return (now - timestamp) < CACHE_VALIDITY_MS
    }

    /**
     * 获取置信度等级描述
     *
     * 🎯 设计考虑：
     * - 为用户提供翻译质量的直观反馈
     * - 支持UI的质量指示器显示
     *
     * @return 置信度等级描述
     */
    fun getConfidenceLevel(): String {
        return when {
            confidence == null -> "未知"
            confidence >= HIGH_CONFIDENCE_THRESHOLD -> "高质量"
            confidence >= LOW_CONFIDENCE_THRESHOLD -> "中等质量"
            else -> "低质量"
        }
    }

    /**
     * 检查是否为高质量翻译
     *
     * @return 是否为高质量翻译
     */
    fun isHighQuality(): Boolean {
        return confidence != null && confidence >= HIGH_CONFIDENCE_THRESHOLD
    }

    /**
     * 生成用于分享的文本
     *
     * 🎯 设计考虑：
     * - 包含原文、译文和翻译方向
     * - 格式化为用户友好的分享内容
     * - 支持社交媒体分享
     *
     * @return 分享文本内容
     */
    fun toShareText(): String {
        val original = getOriginalText()
        val direction = getTranslationDirection()
        
        return buildString {
            appendLine("📝 翻译结果")
            appendLine()
            appendLine("原文：$original")
            appendLine("译文：$translatedText")
            appendLine("方向：$direction")
            appendLine()
            appendLine("来自 myTranslator")
        }
    }

    /**
     * 生成显示摘要
     *
     * 🎯 设计考虑：
     * - 为历史记录列表提供简洁的显示内容
     * - 限制长度避免UI布局问题
     *
     * @param maxLength 最大显示长度
     * @return 显示摘要
     */
    fun getDisplaySummary(maxLength: Int = 50): String {
        val original = getOriginalText()
        val preview = if (original.length > maxLength) {
            "${original.take(maxLength)}..."
        } else {
            original
        }
        return "$preview → ${translatedText.take(maxLength)}"
    }

    /**
     * 检查翻译结果是否有效
     *
     * 🎯 设计考虑：
     * - 验证翻译结果的完整性
     * - 避免显示无效或空的翻译结果
     *
     * @return 翻译结果是否有效
     */
    fun isValid(): Boolean {
        return translatedText.isNotBlank() && !input.isEmpty()
    }

    /**
     * 获取性能信息描述
     *
     * @return 性能信息字符串
     */
    fun getPerformanceInfo(): String? {
        return durationMs?.let { "翻译耗时：${it}ms" }
    }
}
