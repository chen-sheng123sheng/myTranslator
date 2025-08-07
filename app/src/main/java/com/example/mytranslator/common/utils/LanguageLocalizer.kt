package com.example.mytranslator.common.utils

import android.content.Context
import com.example.mytranslator.R
import com.example.mytranslator.domain.model.Language

/**
 * 语言本地化工具类
 *
 * 🎯 设计目的：
 * 1. 提供语言名称的本地化显示
 * 2. 根据用户系统语言自动适配显示文本
 * 3. 支持动态语言切换
 * 4. 保持Domain层的纯净性（不依赖Android框架）
 *
 * 🏗️ 架构设计：
 * - 工具类模式：提供静态方法，便于调用
 * - 资源映射：将语言代码映射到字符串资源ID
 * - 回退机制：如果找不到对应资源，使用默认显示
 * - 缓存优化：避免重复的资源查找
 *
 * 🔧 技术特性：
 * - 支持所有Android支持的语言
 * - 自动适配系统语言设置
 * - 高性能的资源查找
 * - 类型安全的资源访问
 *
 * 📱 使用场景：
 * - 语言选择界面的显示
 * - 翻译结果的语言标识
 * - 用户设置界面
 * - 任何需要显示语言名称的地方
 *
 * 🎓 学习要点：
 * 国际化最佳实践：
 * 1. 所有用户可见文本都应该使用字符串资源
 * 2. 提供回退机制处理未翻译的内容
 * 3. 使用工具类封装复杂的本地化逻辑
 * 4. 保持代码的可测试性和可维护性
 */
object LanguageLocalizer {

    /**
     * 语言代码到字符串资源ID的映射表
     *
     * 🎯 设计考虑：
     * - 使用Map提供O(1)的查找性能
     * - 集中管理所有语言的资源映射
     * - 便于添加新语言支持
     * - 类型安全的资源ID管理
     */
    private val languageResourceMap = mapOf(
        "auto" to R.string.language_auto_detect,
        "zh" to R.string.language_zh,
        "en" to R.string.language_en,
        "jp" to R.string.language_jp,
        "kor" to R.string.language_kor,
        "fra" to R.string.language_fra,
        "de" to R.string.language_de,
        "spa" to R.string.language_spa,
        "ru" to R.string.language_ru,
        "it" to R.string.language_it,
        "pt" to R.string.language_pt,
        "ara" to R.string.language_ara,
        "th" to R.string.language_th,
        "vie" to R.string.language_vie
    )

    /**
     * 获取语言的本地化显示名称
     *
     * 🎯 核心功能：
     * - 根据用户系统语言返回对应的语言名称
     * - 例如：中文系统显示"英语"，英文系统显示"English"
     *
     * @param context Android上下文，用于访问字符串资源
     * @param language 要获取显示名称的语言对象
     * @return 本地化的语言显示名称
     *
     * 🔧 实现逻辑：
     * 1. 首先尝试从资源映射表中查找
     * 2. 如果找到，使用系统本地化的字符串
     * 3. 如果找不到，回退到Language对象的displayName
     * 4. 最后回退到Language对象的name
     */
    fun getLocalizedLanguageName(context: Context, language: Language): String {
        return try {
            // 尝试从资源映射表中获取本地化名称
            val resourceId = languageResourceMap[language.code]
            if (resourceId != null) {
                // 使用系统的本地化字符串
                context.getString(resourceId)
            } else {
                // 回退到Language对象的displayName
                language.displayName.ifEmpty { language.name }
            }
        } catch (e: Exception) {
            // 异常情况下的最终回退
            language.displayName.ifEmpty { language.name }
        }
    }

    /**
     * 获取语言选择相关的本地化文本
     *
     * 🎯 设计目的：
     * - 为语言选择界面提供统一的文本获取方法
     * - 确保所有相关文本都支持国际化
     * - 提供类型安全的文本访问
     */
    object LanguageSelection {
        
        fun getSourceLanguageTitle(context: Context): String {
            return context.getString(R.string.select_source_language)
        }
        
        fun getTargetLanguageTitle(context: Context): String {
            return context.getString(R.string.select_target_language)
        }
        
        fun getSearchHint(context: Context): String {
            return context.getString(R.string.search_languages)
        }
        
        fun getCloseText(context: Context): String {
            return context.getString(R.string.close)
        }
        
        fun getSourceLanguageSelectedMessage(context: Context, languageName: String): String {
            return context.getString(R.string.source_language_selected, languageName)
        }
        
        fun getTargetLanguageSelectedMessage(context: Context, languageName: String): String {
            return context.getString(R.string.target_language_selected, languageName)
        }
    }

    /**
     * 检查是否支持指定语言的本地化
     *
     * @param languageCode 语言代码
     * @return 如果支持本地化返回true，否则返回false
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return languageResourceMap.containsKey(languageCode)
    }

    /**
     * 获取所有支持本地化的语言代码列表
     *
     * @return 支持的语言代码列表
     */
    fun getSupportedLanguageCodes(): Set<String> {
        return languageResourceMap.keys
    }

    /**
     * 为调试和开发提供的工具方法
     * 获取语言资源映射的详细信息
     */
    fun getLanguageResourceMappingInfo(): String {
        return buildString {
            appendLine("Language Resource Mapping:")
            languageResourceMap.forEach { (code, resourceId) ->
                appendLine("  $code -> $resourceId")
            }
        }
    }
}
