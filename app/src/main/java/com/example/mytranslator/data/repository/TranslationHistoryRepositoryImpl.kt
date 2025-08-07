package com.example.mytranslator.data.repository

import android.util.Log
import com.example.mytranslator.data.local.dao.TranslationHistoryDao
import com.example.mytranslator.data.mapper.TranslationHistoryMapper
import com.example.mytranslator.domain.model.TranslationHistory
import com.example.mytranslator.domain.repository.HistoryStatistics
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 翻译历史记录Repository实现类
 *
 * 🎯 设计目的：
 * 1. 实现TranslationHistoryRepository接口
 * 2. 协调数据层和领域层之间的数据转换
 * 3. 处理数据访问的具体逻辑和异常
 * 4. 提供高性能的数据操作实现
 *
 * 🏗️ 架构设计：
 * - 使用Mapper进行Entity和Domain模型转换
 * - 使用协程确保异步操作不阻塞UI
 * - 使用Flow提供响应式数据流
 * - 使用Result包装操作结果，提供统一的错误处理
 *
 * 📱 技术特性：
 * - 依赖注入支持（Dagger/Hilt）
 * - 单例模式确保数据一致性
 * - 异常处理和日志记录
 * - 性能优化的数据库操作
 *
 * 🎓 学习要点：
 * Repository实现的关键要素：
 * 1. 数据转换 - Entity与Domain模型的映射
 * 2. 异常处理 - 将数据库异常转换为业务异常
 * 3. 性能优化 - 合理使用协程和数据库操作
 * 4. 测试友好 - 便于Mock和单元测试
 */
class TranslationHistoryRepositoryImpl(
    private val translationHistoryDao: TranslationHistoryDao,
    private val mapper: TranslationHistoryMapper
) : TranslationHistoryRepository {

    companion object {
        private const val TAG = "TranslationHistoryRepo"
    }

    /**
     * 保存翻译记录
     *
     * 🔧 实现细节：
     * 1. 将Domain模型转换为Entity
     * 2. 执行数据库插入操作
     * 3. 处理可能的异常情况
     * 4. 返回操作结果
     */
    override suspend fun saveTranslation(translation: TranslationHistory): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💾 保存翻译记录: ${translation.getOriginalTextPreview()}")
                
                // 验证数据有效性
                if (!translation.isValid()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("翻译记录数据无效")
                    )
                }
                
                // 转换为Entity并保存
                val entity = mapper.toEntity(translation)
                translationHistoryDao.insertTranslation(entity)
                
                Log.d(TAG, "✅ 翻译记录保存成功: ${translation.id}")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存翻译记录失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取所有历史记录
     *
     * 🔧 实现细节：
     * 1. 从数据库获取Entity列表
     * 2. 使用Flow进行响应式数据流
     * 3. 转换为Domain模型
     * 4. 按时间倒序排列
     */
    override fun getAllHistory(): Flow<List<TranslationHistory>> {
        Log.d(TAG, "📋 获取所有历史记录")
        
        return translationHistoryDao.getAllHistoryFlow()
            .map { entities ->
                entities.map { entity ->
                    mapper.toDomain(entity)
                }
            }
    }

    /**
     * 搜索历史记录
     *
     * 🔧 实现细节：
     * 1. 使用数据库的模糊搜索功能
     * 2. 搜索原文和译文内容
     * 3. 实时返回搜索结果
     */
    override fun searchHistory(query: String): Flow<List<TranslationHistory>> {
        Log.d(TAG, "🔍 搜索历史记录: $query")
        
        return if (query.isBlank()) {
            getAllHistory()
        } else {
            translationHistoryDao.searchHistory(query)
                .map { entities ->
                    entities.map { entity ->
                        mapper.toDomain(entity)
                    }
                }
        }
    }

    /**
     * 获取收藏的翻译记录
     */
    override fun getFavorites(): Flow<List<TranslationHistory>> {
        Log.d(TAG, "⭐ 获取收藏记录")
        
        return translationHistoryDao.getFavoritesFlow()
            .map { entities ->
                entities.map { entity ->
                    mapper.toDomain(entity)
                }
            }
    }

    /**
     * 切换收藏状态
     *
     * 🔧 实现细节：
     * 1. 更新数据库中的收藏状态
     * 2. 使用数据库的原子操作
     * 3. 返回操作结果
     */
    override suspend fun toggleFavorite(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⭐ 切换收藏状态: $id")
                
                // 获取当前记录
                val entity = translationHistoryDao.getTranslationById(id)
                    ?: return@withContext Result.failure(
                        NoSuchElementException("未找到ID为 $id 的翻译记录")
                    )
                
                // 切换收藏状态
                val updatedCount = translationHistoryDao.updateFavoriteStatus(id, !entity.isFavorite)
                
                if (updatedCount > 0) {
                    Log.d(TAG, "✅ 收藏状态切换成功: $id")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("收藏状态更新失败"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 切换收藏状态失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 删除指定的历史记录
     */
    override suspend fun deleteHistory(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 删除历史记录: $id")
                
                val deletedCount = translationHistoryDao.deleteTranslationById(id)
                
                if (deletedCount > 0) {
                    Log.d(TAG, "✅ 历史记录删除成功: $id")
                    Result.success(Unit)
                } else {
                    Result.failure(NoSuchElementException("未找到要删除的记录"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 删除历史记录失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 清空所有历史记录
     */
    override suspend fun clearAllHistory(keepFavorites: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 清空历史记录 (保留收藏: $keepFavorites)")
                
                val deletedCount = if (keepFavorites) {
                    translationHistoryDao.deleteNonFavorites()
                } else {
                    translationHistoryDao.clearAllHistory()
                }
                
                Log.d(TAG, "✅ 清空完成，删除了 $deletedCount 条记录")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 清空历史记录失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取历史记录统计信息
     */
    override fun getHistoryStatistics(): Flow<HistoryStatistics> {
        Log.d(TAG, "📊 获取历史记录统计")

        return translationHistoryDao.getHistoryStatistics()
            .map { stats ->
                HistoryStatistics(
                    totalCount = stats.totalCount,
                    favoriteCount = stats.favoriteCount,
                    todayCount = stats.todayCount,
                    thisWeekCount = stats.thisWeekCount,
                    thisMonthCount = stats.thisMonthCount,
                    // 这些字段暂时使用默认值，后续可以通过额外查询获取
                    mostUsedSourceLanguage = null,
                    mostUsedTargetLanguage = null,
                    averageTranslationsPerDay = if (stats.totalCount > 0) stats.totalCount.toDouble() / 30 else 0.0
                )
            }
    }

    /**
     * 根据ID获取单个历史记录
     */
    override suspend fun getHistoryById(id: String): TranslationHistory? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 获取历史记录: $id")
                
                val entity = translationHistoryDao.getTranslationById(id)
                entity?.let { mapper.toDomain(it) }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 获取历史记录失败", e)
                null
            }
        }
    }

    /**
     * 批量删除历史记录
     */
    override suspend fun deleteHistoryBatch(ids: List<String>): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 批量删除历史记录: ${ids.size} 条")
                
                val deletedCount = translationHistoryDao.deleteTranslationsByIds(ids)
                
                Log.d(TAG, "✅ 批量删除完成，删除了 $deletedCount 条记录")
                Result.success(deletedCount)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量删除失败", e)
                Result.failure(e)
            }
        }
    }
}
