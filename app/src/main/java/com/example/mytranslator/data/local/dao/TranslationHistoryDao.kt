package com.example.mytranslator.data.local.dao

import androidx.room.*
import com.example.mytranslator.data.local.entity.TranslationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 🏠 translationHistory分支 - 翻译历史记录数据访问对象
 * 
 * DAO (Data Access Object) 定义了与数据库交互的方法。
 * Room会在编译时自动生成这个接口的实现类。
 * 
 * 🎯 设计目标：
 * - 提供完整的CRUD操作
 * - 支持复杂的查询和筛选
 * - 使用Flow实现响应式数据流
 * - 支持批量操作提升性能
 * 
 * 🚀 性能优化：
 * - 使用suspend函数支持协程
 * - 合理使用索引提升查询速度
 * - 支持分页查询避免内存问题
 */
@Dao
interface TranslationHistoryDao {
    
    // ➕ ===== 插入操作 =====
    
    /**
     * 插入单条翻译记录
     * 
     * @Insert注解特点：
     * - onConflict: 处理主键冲突的策略
     * - REPLACE: 如果主键已存在，替换旧记录
     * - suspend: 支持协程，不会阻塞主线程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: TranslationHistoryEntity)
    
    /**
     * 批量插入翻译记录
     * 
     * 批量操作的优势：
     * - 减少数据库事务次数
     * - 提升插入性能
     * - 适用于数据导入场景
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<TranslationHistoryEntity>)
    
    // 🔍 ===== 查询操作 =====
    
    /**
     * 获取所有翻译历史记录（按时间倒序）
     * 
     * Flow的优势：
     * - 响应式数据流，数据变化时自动更新UI
     * - 支持背压处理
     * - 与Compose和LiveData无缝集成
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<TranslationHistoryEntity>>
    
    /**
     * 获取所有翻译历史记录（一次性查询）
     * 
     * 适用场景：
     * - 数据导出
     * - 一次性数据处理
     * - 不需要响应式更新的场景
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<TranslationHistoryEntity>
    
    /**
     * 分页获取翻译历史记录
     * 
     * LIMIT和OFFSET的使用：
     * - LIMIT: 限制返回记录数量
     * - OFFSET: 跳过指定数量的记录
     * - 适用于大数据集的分页加载
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistoryPaged(limit: Int, offset: Int): List<TranslationHistoryEntity>
    
    /**
     * 获取收藏的翻译记录
     * 
     * WHERE子句的使用：
     * - 筛选特定条件的记录
     * - 利用索引提升查询性能
     */
    @Query("SELECT * FROM translation_history WHERE is_favorite = 1 ORDER BY timestamp DESC")
    fun getFavoritesFlow(): Flow<List<TranslationHistoryEntity>>
    
    /**
     * 根据ID获取特定翻译记录
     * 
     * 返回类型说明：
     * - TranslationHistoryEntity?: 可能为null，表示记录不存在
     * - suspend: 异步查询，不阻塞主线程
     */
    @Query("SELECT * FROM translation_history WHERE id = :id")
    suspend fun getTranslationById(id: String): TranslationHistoryEntity?
    
    /**
     * 搜索翻译记录（支持原文和译文搜索）
     * 
     * LIKE操作符的使用：
     * - %keyword%: 包含关键词的模糊搜索
     * - OR: 逻辑或操作，搜索原文或译文
     * - LOWER(): 不区分大小写搜索
     */
    @Query("""
        SELECT * FROM translation_history 
        WHERE LOWER(original_text) LIKE LOWER('%' || :keyword || '%') 
           OR LOWER(translated_text) LIKE LOWER('%' || :keyword || '%')
        ORDER BY timestamp DESC
    """)
    fun searchTranslations(keyword: String): Flow<List<TranslationHistoryEntity>>
    
    /**
     * 按语言对筛选翻译记录
     * 
     * 多条件查询：
     * - AND: 逻辑与操作
     * - 利用语言代码索引提升性能
     */
    @Query("""
        SELECT * FROM translation_history 
        WHERE source_language_code = :sourceCode 
          AND target_language_code = :targetCode 
        ORDER BY timestamp DESC
    """)
    fun getTranslationsByLanguagePair(
        sourceCode: String, 
        targetCode: String
    ): Flow<List<TranslationHistoryEntity>>
    
    // ✏️ ===== 更新操作 =====
    
    /**
     * 更新整个翻译记录
     * 
     * @Update注解特点：
     * - 根据主键更新记录
     * - 返回受影响的行数
     */
    @Update
    suspend fun updateTranslation(translation: TranslationHistoryEntity): Int
    
    /**
     * 切换收藏状态
     * 
     * 自定义UPDATE语句：
     * - 只更新特定字段
     * - 性能优于更新整个记录
     */
    @Query("UPDATE translation_history SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int
    
    // 🗑️ ===== 删除操作 =====
    
    /**
     * 删除特定翻译记录
     * 
     * @Delete注解特点：
     * - 根据主键删除记录
     * - 返回删除的行数
     */
    @Delete
    suspend fun deleteTranslation(translation: TranslationHistoryEntity): Int
    
    /**
     * 根据ID删除翻译记录
     * 
     * 自定义DELETE语句：
     * - 只需要ID即可删除
     * - 避免查询完整记录
     */
    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteTranslationById(id: String): Int
    
    /**
     * 批量删除翻译记录
     * 
     * IN操作符的使用：
     * - 删除ID列表中的所有记录
     * - 支持批量操作
     */
    @Query("DELETE FROM translation_history WHERE id IN (:ids)")
    suspend fun deleteTranslationsByIds(ids: List<String>): Int
    
    /**
     * 清空所有翻译历史记录
     * 
     * 危险操作：
     * - 删除表中所有数据
     * - 通常需要用户确认
     */
    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory(): Int
    
    /**
     * 删除指定时间之前的记录
     * 
     * 数据清理策略：
     * - 定期清理过期数据
     * - 控制数据库大小
     */
    @Query("DELETE FROM translation_history WHERE timestamp < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long): Int
    
    // 📊 ===== 统计查询 =====
    
    /**
     * 获取翻译记录总数
     * 
     * COUNT()函数：
     * - 统计记录数量
     * - 用于分页计算
     */
    @Query("SELECT COUNT(*) FROM translation_history")
    suspend fun getHistoryCount(): Int
    
    /**
     * 获取收藏记录数量
     */
    @Query("SELECT COUNT(*) FROM translation_history WHERE is_favorite = 1")
    suspend fun getFavoriteCount(): Int
    
    /**
     * 获取最近的翻译记录
     * 
     * LIMIT 1的使用：
     * - 只返回一条记录
     * - 获取最新/最旧记录
     */
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTranslation(): TranslationHistoryEntity?
}

/**
 * 🎓 DAO设计学习要点：
 * 
 * 1. 注解类型：
 *    - @Insert: 插入操作，支持冲突策略
 *    - @Update: 更新操作，根据主键更新
 *    - @Delete: 删除操作，根据主键删除
 *    - @Query: 自定义SQL查询
 * 
 * 2. 返回类型选择：
 *    - Flow<T>: 响应式数据流，数据变化时自动更新
 *    - suspend T: 一次性异步查询
 *    - T?: 可能为null的单个结果
 *    - List<T>: 多个结果的列表
 * 
 * 3. SQL语句技巧：
 *    - 参数绑定: :paramName
 *    - 模糊搜索: LIKE '%keyword%'
 *    - 大小写不敏感: LOWER()
 *    - 多条件查询: AND, OR
 *    - 排序: ORDER BY
 *    - 分页: LIMIT, OFFSET
 * 
 * 4. 性能优化：
 *    - 利用索引字段进行WHERE查询
 *    - 使用批量操作减少事务次数
 *    - 合理使用Flow避免不必要的查询
 *    - 分页加载大数据集
 */
