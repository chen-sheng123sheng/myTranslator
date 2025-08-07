package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.model.TranslationHistory
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * 获取翻译历史记录用例
 *
 * 🎯 业务目标：
 * 获取和管理翻译历史记录的显示，支持：
 * - 全部历史记录获取
 * - 收藏记录筛选
 * - 数据排序和分组
 * - 错误处理和重试
 *
 * 🏗️ Use Case设计原则：
 * - 响应式数据流：使用Flow提供实时数据更新
 * - 数据转换：将Repository数据转换为UI友好的格式
 * - 错误恢复：提供优雅的错误处理和降级策略
 * - 性能优化：支持分页和懒加载
 *
 * 📱 使用场景：
 * - 历史记录列表页面
 * - 收藏记录页面
 * - 搜索结果展示
 * - 统计数据显示
 *
 * 🎓 学习要点：
 * Flow在Use Case中的应用：
 * 1. 响应式编程 - 数据变化时自动更新UI
 * 2. 错误处理 - 使用catch操作符处理异常
 * 3. 数据转换 - 使用map操作符转换数据格式
 * 4. 背压处理 - 合理控制数据流速度
 */
class GetHistoryUseCase(
    private val translationHistoryRepository: TranslationHistoryRepository
) {

    companion object {
        private const val TAG = "GetHistoryUseCase"
    }

    /**
     * 获取所有历史记录
     *
     * 🔧 业务逻辑：
     * 1. 从Repository获取数据流
     * 2. 应用业务规则和排序
     * 3. 转换为UI展示格式
     * 4. 处理错误和异常情况
     *
     * @param sortBy 排序方式
     * @param groupBy 分组方式
     * @return Flow<HistoryResult> 历史记录结果流
     */
    fun getAllHistory(
        sortBy: SortOption = SortOption.TIMESTAMP_DESC,
        groupBy: GroupOption = GroupOption.NONE
    ): Flow<HistoryResult> {
        Log.d(TAG, "📋 获取所有历史记录 - 排序: $sortBy, 分组: $groupBy")
        
        return translationHistoryRepository.getAllHistory()
            .map<List<TranslationHistory>, HistoryResult> { historyList ->
                Log.d(TAG, "📊 获取到 ${historyList.size} 条历史记录")

                // 应用排序
                val sortedList = applySorting(historyList, sortBy)

                // 应用分组
                val groupedData = applyGrouping(sortedList, groupBy)

                HistoryResult.Success(groupedData)
            }
            .catch { exception ->
                Log.e(TAG, "❌ 获取历史记录失败", exception)
                emit(HistoryResult.Error(exception.message ?: "获取历史记录失败"))
            }
    }

    /**
     * 获取收藏的历史记录
     *
     * @param sortBy 排序方式
     * @return Flow<HistoryResult> 收藏记录结果流
     */
    fun getFavoriteHistory(
        sortBy: SortOption = SortOption.TIMESTAMP_DESC
    ): Flow<HistoryResult> {
        Log.d(TAG, "⭐ 获取收藏历史记录 - 排序: $sortBy")
        
        return translationHistoryRepository.getFavorites()
            .map<List<TranslationHistory>, HistoryResult> { favoriteList ->
                Log.d(TAG, "📊 获取到 ${favoriteList.size} 条收藏记录")

                val sortedList = applySorting(favoriteList, sortBy)
                val groupedData = HistoryGroupData(
                    groups = listOf(
                        HistoryGroup(
                            title = "收藏记录",
                            items = sortedList,
                            count = sortedList.size
                        )
                    ),
                    totalCount = sortedList.size
                )

                HistoryResult.Success(groupedData)
            }
            .catch { exception ->
                Log.e(TAG, "❌ 获取收藏记录失败", exception)
                emit(HistoryResult.Error(exception.message ?: "获取收藏记录失败"))
            }
    }

    /**
     * 获取今日翻译记录
     *
     * @return Flow<HistoryResult> 今日记录结果流
     */
    fun getTodayHistory(): Flow<HistoryResult> {
        Log.d(TAG, "📅 获取今日历史记录")
        
        return translationHistoryRepository.getAllHistory()
            .map<List<TranslationHistory>, HistoryResult> { historyList ->
                val todayList = historyList.filter { it.isToday() }
                Log.d(TAG, "📊 获取到 ${todayList.size} 条今日记录")

                val groupedData = HistoryGroupData(
                    groups = listOf(
                        HistoryGroup(
                            title = "今日翻译",
                            items = todayList,
                            count = todayList.size
                        )
                    ),
                    totalCount = todayList.size
                )

                HistoryResult.Success(groupedData)
            }
            .catch { exception ->
                Log.e(TAG, "❌ 获取今日记录失败", exception)
                emit(HistoryResult.Error(exception.message ?: "获取今日记录失败"))
            }
    }

    /**
     * 应用排序规则
     *
     * @param historyList 原始历史记录列表
     * @param sortBy 排序选项
     * @return 排序后的列表
     */
    private fun applySorting(
        historyList: List<TranslationHistory>,
        sortBy: SortOption
    ): List<TranslationHistory> {
        return when (sortBy) {
            SortOption.TIMESTAMP_DESC -> historyList.sortedByDescending { it.timestamp }
            SortOption.TIMESTAMP_ASC -> historyList.sortedBy { it.timestamp }
            SortOption.USAGE_COUNT_DESC -> historyList.sortedByDescending { it.usageCount }
            SortOption.ALPHABETICAL -> historyList.sortedBy { it.originalText.lowercase() }
            SortOption.LANGUAGE_PAIR -> historyList.sortedBy { it.getLanguagePairCode() }
        }
    }

    /**
     * 应用分组规则
     *
     * @param historyList 排序后的历史记录列表
     * @param groupBy 分组选项
     * @return 分组后的数据
     */
    private fun applyGrouping(
        historyList: List<TranslationHistory>,
        groupBy: GroupOption
    ): HistoryGroupData {
        return when (groupBy) {
            GroupOption.NONE -> {
                HistoryGroupData(
                    groups = listOf(
                        HistoryGroup(
                            title = "全部记录",
                            items = historyList,
                            count = historyList.size
                        )
                    ),
                    totalCount = historyList.size
                )
            }
            
            GroupOption.BY_DATE -> {
                groupByDate(historyList)
            }
            
            GroupOption.BY_LANGUAGE_PAIR -> {
                groupByLanguagePair(historyList)
            }
            
            GroupOption.BY_PROVIDER -> {
                groupByProvider(historyList)
            }
        }
    }

    /**
     * 按日期分组
     */
    private fun groupByDate(historyList: List<TranslationHistory>): HistoryGroupData {
        val groups = historyList.groupBy { history ->
            when {
                history.isToday() -> "今天"
                history.isThisWeek() -> "本周"
                history.isThisMonth() -> "本月"
                else -> "更早"
            }
        }.map { (title, items) ->
            HistoryGroup(
                title = title,
                items = items,
                count = items.size
            )
        }.sortedBy { group ->
            when (group.title) {
                "今天" -> 0
                "本周" -> 1
                "本月" -> 2
                "更早" -> 3
                else -> 4
            }
        }
        
        return HistoryGroupData(
            groups = groups,
            totalCount = historyList.size
        )
    }

    /**
     * 按语言对分组
     */
    private fun groupByLanguagePair(historyList: List<TranslationHistory>): HistoryGroupData {
        val groups = historyList.groupBy { it.getLanguagePairDescription() }
            .map { (title, items) ->
                HistoryGroup(
                    title = title,
                    items = items,
                    count = items.size
                )
            }
            .sortedByDescending { it.count }
        
        return HistoryGroupData(
            groups = groups,
            totalCount = historyList.size
        )
    }

    /**
     * 按翻译服务提供商分组
     */
    private fun groupByProvider(historyList: List<TranslationHistory>): HistoryGroupData {
        val groups = historyList.groupBy { it.translationProvider }
            .map { (provider, items) ->
                HistoryGroup(
                    title = provider.replaceFirstChar { it.uppercase() },
                    items = items,
                    count = items.size
                )
            }
            .sortedByDescending { it.count }
        
        return HistoryGroupData(
            groups = groups,
            totalCount = historyList.size
        )
    }
}

/**
 * 排序选项枚举
 */
enum class SortOption {
    TIMESTAMP_DESC,    // 按时间倒序
    TIMESTAMP_ASC,     // 按时间正序
    USAGE_COUNT_DESC,  // 按使用次数倒序
    ALPHABETICAL,      // 按字母顺序
    LANGUAGE_PAIR      // 按语言对
}

/**
 * 分组选项枚举
 */
enum class GroupOption {
    NONE,              // 不分组
    BY_DATE,           // 按日期分组
    BY_LANGUAGE_PAIR,  // 按语言对分组
    BY_PROVIDER        // 按翻译服务商分组
}

/**
 * 历史记录分组数据
 */
data class HistoryGroupData(
    val groups: List<HistoryGroup>,
    val totalCount: Int
)

/**
 * 历史记录分组
 */
data class HistoryGroup(
    val title: String,
    val items: List<TranslationHistory>,
    val count: Int
)

/**
 * 历史记录结果密封类
 */
sealed class HistoryResult {
    /**
     * 获取成功
     */
    data class Success(val data: HistoryGroupData) : HistoryResult()
    
    /**
     * 获取失败
     */
    data class Error(val message: String) : HistoryResult()
    
    /**
     * 加载中
     */
    object Loading : HistoryResult()
    
    /**
     * 检查是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 获取数据
     */
    fun getDataOrNull(): HistoryGroupData? = (this as? Success)?.data
    
    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? = (this as? Error)?.message
}
