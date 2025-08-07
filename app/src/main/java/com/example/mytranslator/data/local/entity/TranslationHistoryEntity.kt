package com.example.mytranslator.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 🏠 translationHistory分支 - 翻译历史记录实体类
 * 
 * 这个Entity类定义了翻译历史记录在Room数据库中的表结构。
 * 每个Entity对应数据库中的一张表，Entity的属性对应表的列。
 * 
 * 🎯 设计目标：
 * - 存储完整的翻译历史信息
 * - 支持高效的查询和排序
 * - 提供收藏功能
 * - 记录翻译元数据
 * 
 * 📊 表结构设计：
 * - 主键：使用UUID确保全局唯一性
 * - 索引：为常用查询字段添加索引提升性能
 * - 字段：涵盖翻译的完整信息
 */
@Entity(
    tableName = "translation_history",  // 📋 指定数据库表名
    indices = [
        // 🚀 性能优化：为常用查询字段添加索引
        Index(value = ["timestamp"]),           // 按时间排序查询
        Index(value = ["is_favorite"]),         // 查询收藏记录
        Index(value = ["source_language_code"]), // 按源语言筛选
        Index(value = ["target_language_code"])  // 按目标语言筛选
    ]
)
data class TranslationHistoryEntity(
    
    // 🔑 主键字段
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,  // 使用UUID作为主键，确保全局唯一性
    
    // 📝 翻译内容字段
    @ColumnInfo(name = "original_text")
    val originalText: String,  // 原始文本
    
    @ColumnInfo(name = "translated_text") 
    val translatedText: String,  // 翻译结果
    
    // 🌍 语言信息字段
    @ColumnInfo(name = "source_language_code")
    val sourceLanguageCode: String,  // 源语言代码（如：en, zh, ja）
    
    @ColumnInfo(name = "target_language_code")
    val targetLanguageCode: String,  // 目标语言代码
    
    @ColumnInfo(name = "source_language_name")
    val sourceLanguageName: String,  // 源语言显示名称（如：英语, 中文, 日语）
    
    @ColumnInfo(name = "target_language_name")
    val targetLanguageName: String,  // 目标语言显示名称
    
    // ⏰ 时间戳字段
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // 翻译时间戳（毫秒），用于排序和筛选
    
    // ⭐ 用户操作字段
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,  // 是否收藏，默认为false
    
    // 🔧 元数据字段
    @ColumnInfo(name = "translation_provider")
    val translationProvider: String = "baidu",  // 翻译服务提供商，默认百度
    
    // 📏 文本长度字段（用于统计分析）
    @ColumnInfo(name = "original_text_length")
    val originalTextLength: Int = originalText.length,  // 原文长度
    
    @ColumnInfo(name = "translated_text_length")
    val translatedTextLength: Int = translatedText.length  // 译文长度
)

/**
 * 🎓 Entity设计学习要点：
 * 
 * 1. @Entity注解：
 *    - tableName: 指定数据库表名
 *    - indices: 定义索引提升查询性能
 * 
 * 2. @PrimaryKey注解：
 *    - 标记主键字段
 *    - 确保记录的唯一性
 * 
 * 3. @ColumnInfo注解：
 *    - name: 指定数据库列名（遵循snake_case命名规范）
 *    - 可以指定其他属性如类型、默认值等
 * 
 * 4. 索引设计原则：
 *    - 为WHERE子句中常用的字段添加索引
 *    - 为ORDER BY中使用的字段添加索引
 *    - 平衡查询性能和存储空间
 * 
 * 5. 字段设计考虑：
 *    - 数据类型选择：String vs Int vs Long
 *    - 默认值设置：减少空值处理
 *    - 字段命名：清晰、一致的命名规范
 * 
 * 6. 性能优化：
 *    - 合理的索引设计
 *    - 避免过大的TEXT字段
 *    - 考虑数据压缩和归档策略
 */
