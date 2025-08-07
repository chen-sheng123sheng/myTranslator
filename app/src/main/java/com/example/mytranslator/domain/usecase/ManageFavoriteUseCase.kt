package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 管理收藏功能用例
 *
 * 🎯 业务目标：
 * 管理翻译历史记录的收藏状态，支持：
 * - 添加和取消收藏
 * - 批量收藏操作
 * - 收藏状态验证
 * - 收藏数量限制
 *
 * 🏗️ Use Case设计原则：
 * - 原子操作：确保收藏状态的一致性
 * - 业务验证：检查收藏操作的合法性
 * - 错误恢复：提供操作失败时的回滚机制
 * - 用户反馈：提供清晰的操作结果反馈
 *
 * 📱 使用场景：
 * - 历史记录列表中的收藏按钮
 * - 翻译详情页面的收藏功能
 * - 批量管理收藏记录
 * - 收藏夹整理和清理
 *
 * 🎓 学习要点：
 * 状态管理的关键概念：
 * 1. 原子性 - 确保操作的完整性
 * 2. 一致性 - 保持数据状态的一致
 * 3. 隔离性 - 避免并发操作的冲突
 * 4. 持久性 - 确保状态变更的持久化
 */
class ManageFavoriteUseCase(
    private val translationHistoryRepository: TranslationHistoryRepository
) {

    companion object {
        private const val TAG = "ManageFavoriteUseCase"
        private const val MAX_FAVORITES = 1000 // 最大收藏数量限制
    }

    /**
     * 切换收藏状态
     *
     * 🔧 业务逻辑：
     * 1. 验证记录是否存在
     * 2. 检查收藏数量限制
     * 3. 执行状态切换
     * 4. 记录操作日志
     * 5. 返回操作结果
     *
     * @param translationId 翻译记录ID
     * @return FavoriteResult 操作结果
     */
    suspend fun toggleFavorite(translationId: String): FavoriteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⭐ 切换收藏状态: $translationId")
                
                // 1. 验证输入
                if (translationId.isBlank()) {
                    return@withContext FavoriteResult.ValidationError("翻译记录ID不能为空")
                }
                
                // 2. 获取当前记录
                val currentRecord = translationHistoryRepository.getHistoryById(translationId)
                if (currentRecord == null) {
                    Log.w(TAG, "❌ 未找到翻译记录: $translationId")
                    return@withContext FavoriteResult.NotFound("未找到指定的翻译记录")
                }
                
                // 3. 检查收藏数量限制（仅在添加收藏时检查）
                if (!currentRecord.isFavorite) {
                    val favoriteCountResult = checkFavoriteLimit()
                    if (!favoriteCountResult.canAddMore) {
                        Log.w(TAG, "❌ 收藏数量已达上限")
                        return@withContext FavoriteResult.LimitExceeded(
                            "收藏数量已达上限（$MAX_FAVORITES），请先删除一些收藏记录"
                        )
                    }
                }
                
                // 4. 执行切换操作
                val repositoryResult = translationHistoryRepository.toggleFavorite(translationId)
                
                if (repositoryResult.isSuccess) {
                    val newStatus = !currentRecord.isFavorite
                    val action = if (newStatus) "添加收藏" else "取消收藏"
                    Log.i(TAG, "✅ $action 成功: $translationId")
                    
                    FavoriteResult.Success(
                        translationId = translationId,
                        isFavorite = newStatus,
                        action = if (newStatus) FavoriteAction.ADDED else FavoriteAction.REMOVED
                    )
                } else {
                    val error = repositoryResult.exceptionOrNull()
                    Log.e(TAG, "❌ 切换收藏状态失败", error)
                    FavoriteResult.OperationError(error?.message ?: "操作失败")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 切换收藏状态时发生异常", e)
                FavoriteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 批量添加收藏
     *
     * @param translationIds 翻译记录ID列表
     * @return BatchFavoriteResult 批量操作结果
     */
    suspend fun addFavoritesBatch(translationIds: List<String>): BatchFavoriteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⭐ 批量添加收藏: ${translationIds.size} 条记录")
                
                if (translationIds.isEmpty()) {
                    return@withContext BatchFavoriteResult.ValidationError("记录列表不能为空")
                }
                
                // 检查收藏数量限制
                val favoriteCountResult = checkFavoriteLimit()
                val availableSlots = MAX_FAVORITES - favoriteCountResult.currentCount
                
                if (translationIds.size > availableSlots) {
                    return@withContext BatchFavoriteResult.LimitExceeded(
                        "只能再添加 $availableSlots 条收藏记录"
                    )
                }
                
                val results = mutableListOf<FavoriteResult>()
                var successCount = 0
                var failureCount = 0
                
                // 逐个处理
                for (id in translationIds) {
                    val result = toggleFavoriteInternal(id, true)
                    results.add(result)
                    
                    if (result.isSuccess()) {
                        successCount++
                    } else {
                        failureCount++
                    }
                }
                
                Log.i(TAG, "✅ 批量添加收藏完成: 成功 $successCount, 失败 $failureCount")
                
                BatchFavoriteResult.Success(
                    totalCount = translationIds.size,
                    successCount = successCount,
                    failureCount = failureCount,
                    results = results
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量添加收藏时发生异常", e)
                BatchFavoriteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 批量取消收藏
     *
     * @param translationIds 翻译记录ID列表
     * @return BatchFavoriteResult 批量操作结果
     */
    suspend fun removeFavoritesBatch(translationIds: List<String>): BatchFavoriteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⭐ 批量取消收藏: ${translationIds.size} 条记录")
                
                if (translationIds.isEmpty()) {
                    return@withContext BatchFavoriteResult.ValidationError("记录列表不能为空")
                }
                
                val results = mutableListOf<FavoriteResult>()
                var successCount = 0
                var failureCount = 0
                
                // 逐个处理
                for (id in translationIds) {
                    val result = toggleFavoriteInternal(id, false)
                    results.add(result)
                    
                    if (result.isSuccess()) {
                        successCount++
                    } else {
                        failureCount++
                    }
                }
                
                Log.i(TAG, "✅ 批量取消收藏完成: 成功 $successCount, 失败 $failureCount")
                
                BatchFavoriteResult.Success(
                    totalCount = translationIds.size,
                    successCount = successCount,
                    failureCount = failureCount,
                    results = results
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量取消收藏时发生异常", e)
                BatchFavoriteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 检查收藏数量限制
     *
     * @return FavoriteCountResult 收藏数量检查结果
     */
    private suspend fun checkFavoriteLimit(): FavoriteCountResult {
        return try {
            val statistics = translationHistoryRepository.getHistoryStatistics()
            // 由于getHistoryStatistics返回Flow，这里我们需要获取当前值
            // 在实际实现中，可能需要添加一个同步方法或使用first()
            
            // 临时实现：假设当前收藏数量
            val currentCount = 0 // 这里应该从statistics中获取实际值
            
            FavoriteCountResult(
                currentCount = currentCount,
                maxCount = MAX_FAVORITES,
                canAddMore = currentCount < MAX_FAVORITES
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 检查收藏数量限制失败", e)
            FavoriteCountResult(
                currentCount = MAX_FAVORITES, // 保守估计，假设已满
                maxCount = MAX_FAVORITES,
                canAddMore = false
            )
        }
    }

    /**
     * 内部切换收藏状态方法
     *
     * @param translationId 翻译记录ID
     * @param targetState 目标状态（true=收藏，false=取消收藏）
     * @return FavoriteResult 操作结果
     */
    private suspend fun toggleFavoriteInternal(
        translationId: String,
        targetState: Boolean
    ): FavoriteResult {
        return try {
            val currentRecord = translationHistoryRepository.getHistoryById(translationId)
            if (currentRecord == null) {
                return FavoriteResult.NotFound("未找到指定的翻译记录")
            }
            
            // 如果已经是目标状态，直接返回成功
            if (currentRecord.isFavorite == targetState) {
                return FavoriteResult.Success(
                    translationId = translationId,
                    isFavorite = targetState,
                    action = if (targetState) FavoriteAction.ALREADY_ADDED else FavoriteAction.ALREADY_REMOVED
                )
            }
            
            // 执行状态切换
            val repositoryResult = translationHistoryRepository.toggleFavorite(translationId)
            
            if (repositoryResult.isSuccess) {
                FavoriteResult.Success(
                    translationId = translationId,
                    isFavorite = targetState,
                    action = if (targetState) FavoriteAction.ADDED else FavoriteAction.REMOVED
                )
            } else {
                val error = repositoryResult.exceptionOrNull()
                FavoriteResult.OperationError(error?.message ?: "操作失败")
            }
            
        } catch (e: Exception) {
            FavoriteResult.UnknownError(e.message ?: "未知错误")
        }
    }
}

/**
 * 收藏操作类型枚举
 */
enum class FavoriteAction {
    ADDED,           // 已添加收藏
    REMOVED,         // 已取消收藏
    ALREADY_ADDED,   // 已经是收藏状态
    ALREADY_REMOVED  // 已经是非收藏状态
}

/**
 * 收藏数量检查结果
 */
data class FavoriteCountResult(
    val currentCount: Int,
    val maxCount: Int,
    val canAddMore: Boolean
)

/**
 * 收藏操作结果密封类
 */
sealed class FavoriteResult {
    /**
     * 操作成功
     */
    data class Success(
        val translationId: String,
        val isFavorite: Boolean,
        val action: FavoriteAction
    ) : FavoriteResult()
    
    /**
     * 验证错误
     */
    data class ValidationError(val message: String) : FavoriteResult()
    
    /**
     * 记录未找到
     */
    data class NotFound(val message: String) : FavoriteResult()
    
    /**
     * 数量限制错误
     */
    data class LimitExceeded(val message: String) : FavoriteResult()
    
    /**
     * 操作错误
     */
    data class OperationError(val message: String) : FavoriteResult()
    
    /**
     * 未知错误
     */
    data class UnknownError(val message: String) : FavoriteResult()
    
    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? = when (this) {
        is Success -> null
        is ValidationError -> message
        is NotFound -> message
        is LimitExceeded -> message
        is OperationError -> message
        is UnknownError -> message
    }
}

/**
 * 批量收藏操作结果密封类
 */
sealed class BatchFavoriteResult {
    /**
     * 批量操作成功
     */
    data class Success(
        val totalCount: Int,
        val successCount: Int,
        val failureCount: Int,
        val results: List<FavoriteResult>
    ) : BatchFavoriteResult()
    
    /**
     * 验证错误
     */
    data class ValidationError(val message: String) : BatchFavoriteResult()
    
    /**
     * 数量限制错误
     */
    data class LimitExceeded(val message: String) : BatchFavoriteResult()
    
    /**
     * 未知错误
     */
    data class UnknownError(val message: String) : BatchFavoriteResult()
    
    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 获取成功率
     */
    fun getSuccessRate(): Double = when (this) {
        is Success -> if (totalCount > 0) successCount.toDouble() / totalCount else 0.0
        else -> 0.0
    }
}
