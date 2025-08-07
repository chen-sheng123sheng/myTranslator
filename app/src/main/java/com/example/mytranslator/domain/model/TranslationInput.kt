package com.example.mytranslator.domain.model

import java.io.File

/**
 * 翻译输入抽象模型
 *
 * 🎯 设计思想：
 * 1. 使用sealed class实现类型安全的多态设计
 * 2. 为不同类型的翻译输入提供统一接口
 * 3. 支持未来扩展（语音、图片等输入方式）
 * 4. 编译时确保所有输入类型都被正确处理
 *
 * 🔧 技术特性：
 * - sealed class限制继承，确保类型安全
 * - when表达式的穷尽性检查，避免遗漏处理
 * - 每个子类可以携带不同的数据结构
 * - 支持模式匹配和类型判断
 *
 * 📱 使用场景：
 * - 翻译用例的统一输入参数
 * - UI层向业务层传递不同类型的输入
 * - 翻译历史记录的输入类型标识
 * - 不同翻译方式的路由和处理
 *
 * 🎓 学习要点：
 * sealed class vs enum class的选择：
 * - enum class：固定的常量值，不能携带不同数据
 * - sealed class：可以有不同的数据结构，更灵活
 */
sealed class TranslationInput {

    /**
     * 文本翻译输入
     *
     * 🎯 设计考虑：
     * - 支持长文本翻译（最大5000字符）
     * - 提供内容验证功能
     * - 为UI层提供字符计数支持
     *
     * @param content 要翻译的文本内容
     * @param maxLength 最大字符长度限制，默认5000
     */
    data class Text(
        val content: String,
        val maxLength: Int = MAX_TEXT_LENGTH
    ) : TranslationInput() {

        companion object {
            /** 文本翻译的最大字符长度 */
            const val MAX_TEXT_LENGTH = 5000
            
            /** 最小有效字符长度 */
            const val MIN_TEXT_LENGTH = 1
        }

        /**
         * 验证文本内容是否有效
         *
         * 🎯 设计考虑：
         * - 在业务层进行数据验证，确保数据质量
         * - 提供详细的错误信息，便于UI层显示
         * - 支持不同的验证规则扩展
         *
         * @return 验证结果，成功返回null，失败返回错误信息
         */
        fun validateText(): String? {
            return when {
                content.isBlank() -> "文本内容不能为空"
                content.length < MIN_TEXT_LENGTH -> "文本内容太短"
                content.length > maxLength -> "文本内容超过${maxLength}字符限制"
                else -> null // 验证通过
            }
        }

        /**
         * 获取处理后的文本内容
         *
         * 🎯 设计考虑：
         * - 统一文本预处理逻辑
         * - 去除多余空白字符
         * - 为API调用准备标准化的文本
         *
         * @return 处理后的文本内容
         */
        fun getProcessedContent(): String {
            return content.trim()
        }

        /**
         * 获取字符统计信息
         *
         * 🎯 设计考虑：
         * - 为UI层提供字符计数功能
         * - 支持实时字符统计显示
         *
         * @return 字符统计信息
         */
        fun getCharacterCount(): Int {
            return content.length
        }

        /**
         * 检查是否接近字符限制
         *
         * @param warningThreshold 警告阈值（0.0-1.0）
         * @return 是否接近限制
         */
        fun isNearLimit(warningThreshold: Double = 0.9): Boolean {
            return content.length >= (maxLength * warningThreshold)
        }
    }

    /**
     * 语音翻译输入（预留扩展）
     *
     * 🎯 未来设计考虑：
     * - 支持多种音频格式（MP3、WAV、AAC等）
     * - 音频质量和时长限制
     * - 语音识别的准确度优化
     * - 实时语音翻译支持
     *
     * 📝 实现时需要考虑：
     * - 音频文件的存储和管理
     * - 语音识别API的集成
     * - 音频权限的处理
     * - 网络传输的优化
     */
    data class Voice(
        val audioFile: File,
        val durationMs: Long,
        val sampleRate: Int = 16000,
        val maxDurationMs: Long = MAX_VOICE_DURATION
    ) : TranslationInput() {

        companion object {
            /** 语音翻译的最大时长（毫秒） */
            const val MAX_VOICE_DURATION = 60_000L // 60秒
            
            /** 最小有效时长（毫秒） */
            const val MIN_VOICE_DURATION = 500L // 0.5秒
        }

        /**
         * 验证语音文件是否有效（预留）
         */
        fun validateVoice(): String? {
            return when {
                !audioFile.exists() -> "音频文件不存在"
                audioFile.length() == 0L -> "音频文件为空"
                durationMs < MIN_VOICE_DURATION -> "录音时间太短"
                durationMs > maxDurationMs -> "录音时间超过${maxDurationMs/1000}秒限制"
                else -> null
            }
        }
    }

    /**
     * 图片翻译输入（预留扩展）
     *
     * 🎯 未来设计考虑：
     * - 支持多种图片格式（JPEG、PNG、WebP等）
     * - OCR文字识别区域选择
     * - 图片质量和大小限制
     * - 批量图片翻译支持
     *
     * 📝 实现时需要考虑：
     * - 图片压缩和优化
     * - OCR识别准确度
     * - 相机权限的处理
     * - 图片存储和缓存
     */
    data class Image(
        val imageFile: File,
        val ocrRegion: android.graphics.Rect? = null,
        val maxFileSizeBytes: Long = MAX_IMAGE_SIZE
    ) : TranslationInput() {

        companion object {
            /** 图片文件的最大大小（字节） */
            const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L // 10MB
        }

        /**
         * 验证图片文件是否有效（预留）
         */
        fun validateImage(): String? {
            return when {
                !imageFile.exists() -> "图片文件不存在"
                imageFile.length() == 0L -> "图片文件为空"
                imageFile.length() > maxFileSizeBytes -> "图片文件超过${maxFileSizeBytes/1024/1024}MB限制"
                else -> null
            }
        }
    }

    /**
     * 🎓 学习要点：sealed class的扩展函数
     *
     * 为什么在这里定义扩展函数？
     * 1. 为所有子类提供通用行为
     * 2. 避免在每个子类中重复代码
     * 3. 利用when表达式的穷尽性检查
     * 4. 保持代码的整洁和一致性
     */

    /**
     * 获取输入类型的显示名称
     *
     * 🎯 设计考虑：
     * - 为UI层提供用户友好的类型名称
     * - 支持国际化扩展
     * - 便于日志记录和调试
     *
     * @return 输入类型的显示名称
     */
    fun getTypeName(): String {
        return when (this) {
            is Text -> "文本翻译"
            is Voice -> "语音翻译"
            is Image -> "图片翻译"
        }
    }

    /**
     * 验证输入数据是否有效
     *
     * 🎯 设计考虑：
     * - 统一的验证接口，便于业务层调用
     * - 利用when表达式确保所有类型都被处理
     * - 返回详细的错误信息
     *
     * @return 验证结果，成功返回null，失败返回错误信息
     */
    fun validate(): String? {
        return when (this) {
            is Text -> this.validateText()
            is Voice -> this.validateVoice()
            is Image -> this.validateImage()
        }
    }

    /**
     * 检查输入是否为空或无效
     *
     * @return 如果输入为空或无效返回true
     */
    fun isEmpty(): Boolean {
        return when (this) {
            is Text -> content.isBlank()
            is Voice -> !audioFile.exists() || audioFile.length() == 0L
            is Image -> !imageFile.exists() || imageFile.length() == 0L
        }
    }

    /**
     * 获取输入数据的大小（用于统计和限制）
     *
     * @return 数据大小（字符数或字节数）
     */
    fun getDataSize(): Long {
        return when (this) {
            is Text -> content.length.toLong()
            is Voice -> audioFile.length()
            is Image -> imageFile.length()
        }
    }
}
