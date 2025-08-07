package com.example.mytranslator.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * 🏠 translationHistory分支 - Room数据库类型转换器
 * 
 * TypeConverter用于处理SQLite不直接支持的Kotlin数据类型。
 * SQLite只支持：INTEGER, REAL, TEXT, BLOB
 * 我们需要将复杂的Kotlin类型转换为这些基础类型进行存储。
 * 
 * 🎯 设计目标：
 * - 支持常用复杂数据类型的存储
 * - 提供高效的序列化/反序列化
 * - 确保数据完整性和类型安全
 * - 为未来功能扩展做准备
 * 
 * 💡 转换原理：
 * - @TypeConverter注解标记转换方法
 * - Room在编译时自动生成调用代码
 * - 存储时：Kotlin类型 → SQLite类型
 * - 读取时：SQLite类型 → Kotlin类型
 */
class DatabaseConverters {
    
    // 📅 ===== 时间类型转换器 =====
    
    /**
     * Date转换为Long时间戳（存储到数据库）
     * 
     * 为什么使用Long存储时间？
     * - SQLite原生支持INTEGER类型
     * - 时间戳便于比较和排序
     * - 跨平台兼容性好
     * - 节省存储空间
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time  // 获取毫秒时间戳
    }
    
    /**
     * Long时间戳转换为Date（从数据库读取）
     * 
     * null处理：
     * - 支持可空类型，避免NPE
     * - 数据库中的NULL值正确映射为Kotlin的null
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }  // 从时间戳创建Date对象
    }
    
    // 📝 ===== 字符串列表转换器 =====
    
    /**
     * 字符串列表转换为JSON字符串（存储到数据库）
     * 
     * 使用JSON的优势：
     * - 人类可读，便于调试
     * - 支持嵌套结构
     * - 跨语言兼容
     * - Gson库成熟稳定
     * 
     * 使用场景示例：
     * - 存储翻译的多个候选结果
     * - 保存用户的搜索历史关键词
     * - 记录翻译过程中的中间步骤
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list == null) {
            null
        } else {
            Gson().toJson(list)  // 序列化为JSON字符串
        }
    }
    
    /**
     * JSON字符串转换为字符串列表（从数据库读取）
     * 
     * 异常处理：
     * - 捕获JSON解析异常
     * - 返回空列表而不是崩溃
     * - 记录错误日志便于调试
     */
    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        return if (json == null) {
            null
        } else {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson(json, type)  // 反序列化JSON
            } catch (e: Exception) {
                // 🚨 JSON解析失败时返回空列表，避免崩溃
                emptyList()
            }
        }
    }
    
    // 🗺️ ===== Map类型转换器 =====
    
    /**
     * Map转换为JSON字符串（存储到数据库）
     * 
     * Map的使用场景：
     * - 存储翻译的元数据信息
     * - 保存API响应的额外字段
     * - 记录用户的个性化设置
     */
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return if (map == null) {
            null
        } else {
            Gson().toJson(map)
        }
    }
    
    /**
     * JSON字符串转换为Map（从数据库读取）
     */
    @TypeConverter
    fun toStringMap(json: String?): Map<String, String>? {
        return if (json == null) {
            null
        } else {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptyMap()  // 解析失败返回空Map
            }
        }
    }
    
    // 🎯 ===== 枚举类型转换器示例 =====
    
    /**
     * 翻译状态枚举（示例）
     * 
     * 枚举的优势：
     * - 类型安全，避免无效值
     * - 代码可读性好
     * - IDE支持自动补全
     */
    enum class TranslationStatus {
        PENDING,    // 待翻译
        SUCCESS,    // 翻译成功
        FAILED,     // 翻译失败
        CACHED      // 缓存结果
    }
    
    /**
     * 枚举转换为字符串（存储到数据库）
     * 
     * 为什么存储枚举名称而不是序号？
     * - 序号可能因为枚举顺序变化而改变
     * - 名称更稳定，便于数据库迁移
     * - 调试时更容易理解
     */
    @TypeConverter
    fun fromTranslationStatus(status: TranslationStatus?): String? {
        return status?.name  // 存储枚举名称
    }
    
    /**
     * 字符串转换为枚举（从数据库读取）
     * 
     * 异常处理：
     * - 处理数据库中的无效枚举值
     * - 提供默认值避免崩溃
     */
    @TypeConverter
    fun toTranslationStatus(name: String?): TranslationStatus? {
        return if (name == null) {
            null
        } else {
            try {
                TranslationStatus.valueOf(name)
            } catch (e: IllegalArgumentException) {
                // 🚨 无效枚举值时返回默认状态
                TranslationStatus.PENDING
            }
        }
    }
    
    // 🔢 ===== 数值列表转换器 =====
    
    /**
     * 整数列表转换为逗号分隔的字符串
     * 
     * 轻量级存储方案：
     * - 相比JSON更节省空间
     * - 适用于简单的数值列表
     * - 人类可读，便于SQL查询
     * 
     * 使用场景：
     * - 存储翻译的置信度分数列表
     * - 记录用户操作的时间间隔
     */
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return list?.joinToString(",")  // 用逗号连接
    }
    
    /**
     * 逗号分隔字符串转换为整数列表
     */
    @TypeConverter
    fun toIntList(data: String?): List<Int>? {
        return if (data.isNullOrEmpty()) {
            null
        } else {
            try {
                data.split(",").map { it.trim().toInt() }
            } catch (e: NumberFormatException) {
                emptyList()  // 解析失败返回空列表
            }
        }
    }
}

/**
 * 🎓 TypeConverter设计学习要点：
 * 
 * 1. 转换器的工作原理：
 *    - @TypeConverter注解标记转换方法
 *    - Room自动识别并调用对应的转换器
 *    - 存储：Kotlin类型 → SQLite支持的类型
 *    - 读取：SQLite类型 → Kotlin类型
 * 
 * 2. 常见转换策略：
 *    - 时间类型：Date ↔ Long时间戳
 *    - 集合类型：List/Map ↔ JSON字符串
 *    - 枚举类型：Enum ↔ 枚举名称字符串
 *    - 简单列表：List<Int> ↔ 逗号分隔字符串
 * 
 * 3. 异常处理原则：
 *    - 永远不要让转换器抛出异常
 *    - 提供合理的默认值
 *    - 记录错误日志便于调试
 *    - 优雅降级而不是崩溃
 * 
 * 4. 性能考虑：
 *    - JSON序列化有一定开销，适合复杂对象
 *    - 简单分隔符适合基础类型列表
 *    - 避免在转换器中进行复杂计算
 *    - 考虑缓存频繁转换的结果
 * 
 * 5. 数据完整性：
 *    - 确保转换是可逆的
 *    - 处理null值的情况
 *    - 验证转换后的数据格式
 *    - 考虑数据库迁移时的兼容性
 * 
 * 6. 最佳实践：
 *    - 为每种转换提供完整的双向方法
 *    - 使用成熟的序列化库（如Gson）
 *    - 编写单元测试验证转换正确性
 *    - 文档化转换器的使用场景
 */
