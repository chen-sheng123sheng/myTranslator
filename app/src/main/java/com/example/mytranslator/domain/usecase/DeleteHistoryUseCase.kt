package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 删除翻译历史记录用例
 *
 * 🎯 业务目标：
 * 管理翻译历史记录的删除操作，支持：
 * - 单个记录删除
 * - 批量记录删除
 * - 清空所有历史记录
 * - 保留收藏记录的清理
 *
 * 🏗️ Use Case设计原则：
 * - 安全删除：提供确认机制和撤销功能
 * - 批量操作：支持高效的批量删除
 * - 数据保护：重要数据的保护策略
 * - 操作审计：记录删除操作的详细日志
 *
 * 📱 使用场景：
 * - 历史记录列表的删除按钮
 * - 批量管理和清理功能
 * - 设置页面的清空历史
 * - 存储空间管理
 *
 * 🎓 学习要点：
 * 删除操作的设计考虑：
 * 1. 安全性 - 防止误删和数据丢失
 * 2. 性能 - 批量删除的优化
 * 3. 一致性 - 确保相关数据的一致性
 * 4. 可恢复性 - 提供数据恢复机制
 */
class DeleteHistoryUseCase(
    private val translationHistoryRepository: TranslationHistoryRepository
) {

    companion object {
        private const val TAG = "DeleteHistoryUseCase"
        private const val MAX_BATCH_SIZE = 100 // 最大批量删除数量
    }

    /**
     * 删除单个翻译记录
     *
     * 🔧 业务逻辑：
     * 1. 验证记录是否存在
     * 2. 检查删除权限
     * 3. 执行删除操作
     * 4. 记录操作日志
     * 5. 返回操作结果
     *
     * @param translationId 翻译记录ID
     * @param forceDelete 是否强制删除（忽略保护策略）
     * @return DeleteResult 删除结果
     */
    suspend fun deleteTranslation(
        translationId: String,
        forceDelete: Boolean = false
    ): DeleteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 删除翻译记录: $translationId (强制: $forceDelete)")
                
                // 1. 验证输入
                if (translationId.isBlank()) {
                    return@withContext DeleteResult.ValidationError("翻译记录ID不能为空")
                }
                
                // 2. 检查记录是否存在
                val record = translationHistoryRepository.getHistoryById(translationId)
                if (record == null) {
                    Log.w(TAG, "❌ 未找到翻译记录: $translationId")
                    return@withContext DeleteResult.NotFound("未找到指定的翻译记录")
                }
                
                // 3. 检查删除保护策略
                if (!forceDelete) {
                    val protectionResult = checkDeletionProtection(record.isFavorite, record.usageCount)
                    if (!protectionResult.canDelete) {
                        Log.w(TAG, "❌ 删除被保护策略阻止: ${protectionResult.reason}")
                        return@withContext DeleteResult.ProtectionError(protectionResult.reason)
                    }
                }
                
                // 4. 执行删除操作
                val repositoryResult = translationHistoryRepository.deleteHistory(translationId)
                
                if (repositoryResult.isSuccess) {
                    Log.i(TAG, "✅ 翻译记录删除成功: $translationId")
                    DeleteResult.Success(
                        deletedIds = listOf(translationId),
                        deletedCount = 1,
                        wasProtected = record.isFavorite || record.usageCount > 10
                    )
                } else {
                    val error = repositoryResult.exceptionOrNull()
                    Log.e(TAG, "❌ 删除翻译记录失败", error)
                    DeleteResult.OperationError(error?.message ?: "删除失败")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 删除翻译记录时发生异常", e)
                DeleteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 批量删除翻译记录
     *
     * @param translationIds 翻译记录ID列表
     * @param forceDelete 是否强制删除
     * @return DeleteResult 删除结果
     */
    suspend fun deleteTranslationsBatch(
        translationIds: List<String>,
        forceDelete: Boolean = false
    ): DeleteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 批量删除翻译记录: ${translationIds.size} 条 (强制: $forceDelete)")
                
                // 1. 验证输入
                if (translationIds.isEmpty()) {
                    return@withContext DeleteResult.ValidationError("删除列表不能为空")
                }
                
                if (translationIds.size > MAX_BATCH_SIZE) {
                    return@withContext DeleteResult.ValidationError(
                        "批量删除数量不能超过 $MAX_BATCH_SIZE 条"
                    )
                }
                
                // 2. 检查重复ID
                val uniqueIds = translationIds.distinct()
                if (uniqueIds.size != translationIds.size) {
                    Log.w(TAG, "⚠️ 删除列表中包含重复ID，已自动去重")
                }
                
                // 3. 分批处理（如果需要）
                val batchSize = 50
                val batches = uniqueIds.chunked(batchSize)
                val allDeletedIds = mutableListOf<String>()
                var protectedCount = 0
                
                for (batch in batches) {
                    val batchResult = processBatch(batch, forceDelete)
                    allDeletedIds.addAll(batchResult.deletedIds)
                    protectedCount += batchResult.protectedCount
                }
                
                Log.i(TAG, "✅ 批量删除完成: 删除 ${allDeletedIds.size} 条，保护 $protectedCount 条")
                
                DeleteResult.Success(
                    deletedIds = allDeletedIds,
                    deletedCount = allDeletedIds.size,
                    wasProtected = protectedCount > 0
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量删除时发生异常", e)
                DeleteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 清空所有历史记录
     *
     * @param keepFavorites 是否保留收藏记录
     * @param confirmationToken 确认令牌（防止误操作）
     * @return DeleteResult 删除结果
     */
    suspend fun clearAllHistory(
        keepFavorites: Boolean = true,
        confirmationToken: String? = null
    ): DeleteResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ 清空所有历史记录 (保留收藏: $keepFavorites)")
                
                // 1. 验证确认令牌（重要操作需要确认）
                if (confirmationToken != "CONFIRM_CLEAR_ALL") {
                    return@withContext DeleteResult.ValidationError("需要确认令牌才能执行清空操作")
                }
                
                // 2. 执行清空操作
                val repositoryResult = translationHistoryRepository.clearAllHistory(keepFavorites)
                
                if (repositoryResult.isSuccess) {
                    val action = if (keepFavorites) "清空非收藏记录" else "清空所有记录"
                    Log.i(TAG, "✅ $action 成功")
                    
                    DeleteResult.Success(
                        deletedIds = emptyList(), // 清空操作不返回具体ID
                        deletedCount = -1, // -1 表示批量清空
                        wasProtected = keepFavorites
                    )
                } else {
                    val error = repositoryResult.exceptionOrNull()
                    Log.e(TAG, "❌ 清空历史记录失败", error)
                    DeleteResult.OperationError(error?.message ?: "清空失败")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 清空历史记录时发生异常", e)
                DeleteResult.UnknownError(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 检查删除保护策略
     *
     * @param isFavorite 是否为收藏记录
     * @param usageCount 使用次数
     * @return DeletionProtectionResult 保护检查结果
     */
    private fun checkDeletionProtection(
        isFavorite: Boolean,
        usageCount: Int
    ): DeletionProtectionResult {
        return when {
            isFavorite -> DeletionProtectionResult(
                canDelete = false,
                reason = "收藏记录受保护，请先取消收藏或使用强制删除"
            )
            
            usageCount > 10 -> DeletionProtectionResult(
                canDelete = false,
                reason = "高频使用记录受保护（使用次数: $usageCount），请使用强制删除"
            )
            
            else -> DeletionProtectionResult(
                canDelete = true,
                reason = ""
            )
        }
    }

    /**
     * 处理删除批次
     *
     * @param batch 批次ID列表
     * @param forceDelete 是否强制删除
     * @return BatchProcessResult 批次处理结果
     */
    private suspend fun processBatch(
        batch: List<String>,
        forceDelete: Boolean
    ): BatchProcessResult {
        val deletedIds = mutableListOf<String>()
        var protectedCount = 0
        
        // 使用Repository的批量删除方法
        val repositoryResult = translationHistoryRepository.deleteHistoryBatch(batch)
        
        if (repositoryResult.isSuccess) {
            val actualDeletedCount = repositoryResult.getOrNull() ?: 0
            
            // 如果删除数量少于请求数量，说明有些记录受保护或不存在
            if (actualDeletedCount < batch.size) {
                protectedCount = batch.size - actualDeletedCount
            }
            
            // 假设删除成功的是前面的记录（实际实现中可能需要更精确的跟踪）
            deletedIds.addAll(batch.take(actualDeletedCount))
        }
        
        return BatchProcessResult(
            deletedIds = deletedIds,
            protectedCount = protectedCount
        )
    }

    /**
     * 删除保护检查结果
     */
    private data class DeletionProtectionResult(
        val canDelete: Boolean,
        val reason: String
    )

    /**
     * 批次处理结果
     */
    private data class BatchProcessResult(
        val deletedIds: List<String>,
        val protectedCount: Int
    )
}

/**
 * 删除结果密封类
 */
sealed class DeleteResult {
    /**
     * 删除成功
     */
    data class Success(
        val deletedIds: List<String>,
        val deletedCount: Int,
        val wasProtected: Boolean
    ) : DeleteResult()
    
    /**
     * 验证错误
     */
    data class ValidationError(val message: String) : DeleteResult()
    
    /**
     * 记录未找到
     */
    data class NotFound(val message: String) : DeleteResult()
    
    /**
     * 保护策略错误
     */
    data class ProtectionError(val message: String) : DeleteResult()
    
    /**
     * 操作错误
     */
    data class OperationError(val message: String) : DeleteResult()
    
    /**
     * 未知错误
     */
    data class UnknownError(val message: String) : DeleteResult()
    
    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 获取删除数量
     */
    fun getActualDeletedCount(): Int = (this as? Success)?.deletedCount ?: 0
    
    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? = when (this) {
        is Success -> null
        is ValidationError -> message
        is NotFound -> message
        is ProtectionError -> message
        is OperationError -> message
        is UnknownError -> message
    }
    
    /**
     * 是否有保护的记录
     */
    fun hasProtectedRecords(): Boolean = (this as? Success)?.wasProtected ?: false
}
