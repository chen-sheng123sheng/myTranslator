package com.example.mytranslator.domain.model

/**
 * 语言数据模型
 *
 * 🎯 设计思想：
 * 1. 使用data class确保不可变性和类型安全
 * 2. 遵循ISO 639-1国际标准的语言代码
 * 3. 支持国际化显示，提供用户友好的界面
 * 4. 为语言选择、API调用、UI显示提供统一的数据结构
 *
 * 🔧 技术特性：
 * - data class自动生成equals()、hashCode()、toString()
 * - 不可变性保证线程安全
 * - 支持解构声明：val (code, name, display) = language
 * - 便于集合操作和比较
 *
 * 📱 使用场景：
 * - 语言选择对话框的数据源
 * - 翻译API调用的参数
 * - 翻译结果的语言信息存储
 * - 用户偏好设置的持久化
 */
data class Language(
    /**
     * 语言代码 (ISO 639-1标准)
     *
     * 🎯 设计考虑：
     * - 使用国际标准，确保与各种翻译API兼容
     * - 简短统一，便于存储和传输
     * - 大小写敏感，通常使用小写
     *
     * 📝 示例：
     * - "en" - 英语
     * - "zh" - 中文
     * - "ja" - 日语
     * - "ko" - 韩语
     * - "fr" - 法语
     */
    val code: String,

    /**
     * 英文名称
     *
     * 🎯 设计考虑：
     * - 使用英文便于开发者理解和调试
     * - 与API文档和国际标准保持一致
     * - 便于日志输出和错误排查
     *
     * 📝 示例：
     * - "English"
     * - "Chinese"
     * - "Japanese"
     * - "Korean"
     * - "French"
     */
    val name: String,

    /**
     * 本地化显示名称
     *
     * 🎯 设计考虑：
     * - 使用该语言的本地名称，用户更容易识别
     * - 提供更好的用户体验
     * - 支持多语言界面
     *
     * 📝 示例：
     * - "English" (英语用英语显示)
     * - "中文" (中文用中文显示)
     * - "日本語" (日语用日语显示)
     * - "한국어" (韩语用韩语显示)
     * - "Français" (法语用法语显示)
     */
    val displayName: String
) {
    /**
     * 🎯 学习要点：companion object的使用
     *
     * 为什么使用companion object？
     * 1. 类似Java的static，但更强大
     * 2. 可以实现接口，继承类
     * 3. 可以有扩展函数
     * 4. 便于创建工厂方法和常量
     */
    companion object {
        /**
         * 预定义的常用语言
         *
         * 🎯 设计考虑：
         * - 提供常用语言的快速访问
         * - 避免重复创建相同的Language对象
         * - 便于测试和开发时使用
         */

        /** 自动检测语言 - 特殊标识 */
        val AUTO_DETECT = Language(
            code = "auto",
            name = "Auto Detect",
            displayName = "自动检测"
        )

        /** 英语 */
        val ENGLISH = Language(
            code = "en",
            name = "English",
            displayName = "English"
        )

        /** 中文（简体） */
        val CHINESE_SIMPLIFIED = Language(
            code = "zh",
            name = "Chinese",
            displayName = "中文"
        )

        /** 日语 */
        val JAPANESE = Language(
            code = "ja",
            name = "Japanese",
            displayName = "日本語"
        )

        /** 韩语 */
        val KOREAN = Language(
            code = "ko",
            name = "Korean",
            displayName = "한국어"
        )

        /** 法语 */
        val FRENCH = Language(
            code = "fr",
            name = "French",
            displayName = "Français"
        )

        /** 德语 */
        val GERMAN = Language(
            code = "de",
            name = "German",
            displayName = "Deutsch"
        )

        /** 西班牙语 */
        val SPANISH = Language(
            code = "es",
            name = "Spanish",
            displayName = "Español"
        )

        /**
         * 获取所有支持的语言列表
         *
         * 🎯 设计考虑：
         * - 返回不可变列表，防止意外修改
         * - 便于语言选择对话框使用
         * - 可以根据需要扩展更多语言
         *
         * @return 支持的语言列表
         */
        fun getSupportedLanguages(): List<Language> {
            return listOf(
                AUTO_DETECT,
                ENGLISH,
                CHINESE_SIMPLIFIED,
                JAPANESE,
                KOREAN,
                FRENCH,
                GERMAN,
                SPANISH
            )
        }

        /**
         * 根据语言代码查找Language对象
         *
         * 🎯 设计考虑：
         * - 便于从API响应或用户设置中恢复Language对象
         * - 使用nullable返回类型，处理未知语言代码
         * - 大小写不敏感，提高容错性
         *
         * @param code 语言代码
         * @return 对应的Language对象，如果未找到则返回null
         */
        fun findByCode(code: String): Language? {
            return getSupportedLanguages().find { 
                it.code.equals(code, ignoreCase = true) 
            }
        }

        /**
         * 获取默认的源语言
         * 
         * 🎯 设计考虑：
         * - 为新用户提供合理的默认值
         * - 自动检测通常是最用户友好的选择
         */
        fun getDefaultSourceLanguage(): Language = AUTO_DETECT

        /**
         * 获取默认的目标语言
         * 
         * 🎯 设计考虑：
         * - 根据应用的主要用户群体选择
         * - 英语作为国际通用语言是合理的默认选择
         */
        fun getDefaultTargetLanguage(): Language = ENGLISH
    }

    /**
     * 🎯 学习要点：自定义方法
     * 
     * data class可以添加自定义方法，增强功能性
     */

    /**
     * 检查是否为自动检测语言
     * 
     * @return 如果是自动检测语言返回true
     */
    fun isAutoDetect(): Boolean = code == "auto"

    /**
     * 获取用于UI显示的文本
     * 
     * 🎯 设计考虑：
     * - 优先使用本地化名称，提供更好的用户体验
     * - 如果本地化名称为空，回退到英文名称
     * 
     * @return 适合UI显示的语言名称
     */
    fun getDisplayText(): String {
        return displayName.ifEmpty { name }
    }
}
