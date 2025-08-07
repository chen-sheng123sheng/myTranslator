package com.example.mytranslator.domain.repository

import com.example.mytranslator.domain.model.TranslationHistory
import kotlinx.coroutines.flow.Flow

/**
 * 翻译历史记录Repository接口
 *
 * 🎯 设计目的：
 * 1. 定义翻译历史记录的数据访问契约
 * 2. 为上层业务逻辑提供统一的数据接口
 * 3. 隔离数据访问细节，提高可测试性
 * 4. 支持多种数据源的扩展（本地数据库、云端同步等）
 *
 * 🏗️ 架构设计：
 * - 使用Flow进行响应式数据流
 * - 使用Result包装操作结果，提供错误处理
 * - 支持异步操作，不阻塞UI线程
 * - 遵循单一职责原则，只关注数据访问
 *
 * 📱 使用场景：
 * - 保存翻译结果到历史记录
 * - 获取和搜索历史记录
 * - 管理收藏功能
 * - 清理和删除操作
 *
 * 🎓 学习要点：
 * Repository模式的核心概念：
 * 1. 抽象数据访问 - 隐藏具体的数据存储实现
 * 2. 统一接口 - 为不同的数据源提供一致的访问方式
 * 3. 业务导向 - 接口设计以业务需求为导向
 * 4. 可测试性 - 便于Mock和单元测试
 */
interface TranslationHistoryRepository {

    /**
     * 保存翻译记录
     *
     * 🎯 功能说明：
     * 将新的翻译结果保存到历史记录中，支持：
     * - 自动生成唯一ID
     * - 时间戳记录
     * - 重复检测（可选）
     * - 数据验证
     *
     * @param translation 要保存的翻译记录
     * @return Result<Unit> 保存结果，成功返回Unit，失败返回异常信息
     */
    suspend fun saveTranslation(translation: TranslationHistory): Result<Unit>

    /**
     * 获取所有历史记录
     *
     * 🎯 功能说明：
     * 获取所有翻译历史记录，按时间倒序排列：
     * - 响应式数据流，自动更新UI
     * - 分页支持（未来扩展）
     * - 性能优化的查询
     *
     * @return Flow<List<TranslationHistory>> 历史记录流
     */
    fun getAllHistory(): Flow<List<TranslationHistory>>

    /**
     * 搜索历史记录
     *
     * 🎯 功能说明：
     * 根据关键词搜索历史记录：
     * - 支持原文和译文搜索
     * - 模糊匹配
     * - 实时搜索结果更新
     * - 搜索结果高亮（UI层实现）
     *
     * @param query 搜索关键词
     * @return Flow<List<TranslationHistory>> 搜索结果流
     */
    fun searchHistory(query: String): Flow<List<TranslationHistory>>

    /**
     * 获取收藏的翻译记录
     *
     * 🎯 功能说明：
     * 获取所有标记为收藏的翻译记录：
     * - 按收藏时间或使用频率排序
     * - 响应式更新
     * - 支持收藏状态变化的实时反映
     *
     * @return Flow<List<TranslationHistory>> 收藏记录流
     */
    fun getFavorites(): Flow<List<TranslationHistory>>

    /**
     * 切换收藏状态
     *
     * 🎯 功能说明：
     * 切换指定翻译记录的收藏状态：
     * - 如果已收藏则取消收藏
     * - 如果未收藏则添加到收藏
     * - 更新收藏时间戳
     *
     * @param id 翻译记录ID
     * @return Result<Unit> 操作结果
     */
    suspend fun toggleFavorite(id: String): Result<Unit>

    /**
     * 删除指定的历史记录
     *
     * 🎯 功能说明：
     * 删除指定ID的翻译历史记录：
     * - 软删除或硬删除（根据业务需求）
     * - 关联数据清理
     * - 操作确认机制（UI层实现）
     *
     * @param id 要删除的记录ID
     * @return Result<Unit> 删除结果
     */
    suspend fun deleteHistory(id: String): Result<Unit>

    /**
     * 清空所有历史记录
     *
     * 🎯 功能说明：
     * 清空所有翻译历史记录：
     * - 批量删除操作
     * - 保留收藏记录（可选）
     * - 操作不可逆，需要确认
     * - 性能优化的批量操作
     *
     * @param keepFavorites 是否保留收藏记录，默认false
     * @return Result<Unit> 清空结果
     */
    suspend fun clearAllHistory(keepFavorites: Boolean = false): Result<Unit>

    /**
     * 获取历史记录统计信息
     *
     * 🎯 功能说明：
     * 获取历史记录的统计数据：
     * - 总记录数
     * - 收藏记录数
     * - 今日翻译次数
     * - 常用语言对统计
     *
     * @return Flow<HistoryStatistics> 统计信息流
     */
    fun getHistoryStatistics(): Flow<HistoryStatistics>

    /**
     * 根据ID获取单个历史记录
     *
     * 🎯 功能说明：
     * 获取指定ID的翻译历史记录：
     * - 用于详情页面显示
     * - 编辑功能支持
     * - 快速访问
     *
     * @param id 记录ID
     * @return TranslationHistory? 翻译记录，不存在时返回null
     */
    suspend fun getHistoryById(id: String): TranslationHistory?

    /**
     * 批量删除历史记录
     *
     * 🎯 功能说明：
     * 批量删除指定的历史记录：
     * - 多选删除功能支持
     * - 性能优化的批量操作
     * - 事务处理确保数据一致性
     *
     * @param ids 要删除的记录ID列表
     * @return Result<Int> 删除结果，返回实际删除的记录数
     */
    suspend fun deleteHistoryBatch(ids: List<String>): Result<Int>
}

/**
 * 历史记录统计信息数据类
 *
 * 🎯 设计说明：
 * 封装历史记录的各种统计数据，用于：
 * - 仪表板显示
 * - 用户使用分析
 * - 功能优化参考
 */
data class HistoryStatistics(
    val totalCount: Int = 0,
    val favoriteCount: Int = 0,
    val todayCount: Int = 0,
    val thisWeekCount: Int = 0,
    val thisMonthCount: Int = 0,
    val mostUsedSourceLanguage: String? = null,
    val mostUsedTargetLanguage: String? = null,
    val averageTranslationsPerDay: Double = 0.0
)
