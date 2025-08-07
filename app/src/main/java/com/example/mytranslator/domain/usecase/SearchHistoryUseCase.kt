package com.example.mytranslator.domain.usecase

import android.util.Log
import com.example.mytranslator.domain.model.TranslationHistory
import com.example.mytranslator.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 搜索翻译历史记录用例
 *
 * 🎯 业务目标：
 * 提供强大的历史记录搜索功能，支持：
 * - 实时搜索和结果更新
 * - 多字段模糊匹配
 * - 搜索结果高亮
 * - 搜索历史管理
 *
 * 🏗️ Use Case设计原则：
 * - 响应式搜索：使用Flow实现实时搜索
 * - 性能优化：防抖动和去重处理
 * - 用户体验：快速响应和智能提示
 * - 搜索算法：多维度匹配和相关性排序
 *
 * 📱 使用场景：
 * - 历史记录搜索页面
 * - 快速查找特定翻译
 * - 智能推荐和联想
 * - 搜索结果筛选
 *
 * 🎓 学习要点：
 * 搜索功能的实现技巧：
 * 1. 防抖动 - 避免频繁的搜索请求
 * 2. 去重 - 避免重复的相同搜索
 * 3. 结果排序 - 按相关性排序搜索结果
 * 4. 高亮显示 - 标记匹配的关键词
 */
class SearchHistoryUseCase(
    private val translationHistoryRepository: TranslationHistoryRepository
) {

    companion object {
        private const val TAG = "SearchHistoryUseCase"
        private const val DEBOUNCE_DELAY = 300L // 防抖延迟300ms
        private const val MIN_SEARCH_LENGTH = 1 // 最小搜索长度
    }

    /**
     * 执行搜索操作
     *
     * 🔧 搜索流程：
     * 1. 输入验证和预处理
     * 2. 防抖动处理
     * 3. 执行搜索查询
     * 4. 结果排序和高亮
     * 5. 返回格式化结果
     *
     * @param queryFlow 搜索关键词流
     * @return Flow<SearchResult> 搜索结果流
     */
    @OptIn(FlowPreview::class)
    fun searchHistory(queryFlow: Flow<String>): Flow<SearchResult> {
        return queryFlow
            .debounce(DEBOUNCE_DELAY) // 防抖动，避免频繁搜索
            .distinctUntilChanged() // 去重，避免重复搜索
            .flatMapLatest { query ->
                performSearch(query)
            }
            .catch { exception ->
                Log.e(TAG, "❌ 搜索过程中发生异常", exception)
                emit(SearchResult.Error(exception.message ?: "搜索失败"))
            }
    }

    /**
     * 执行单次搜索
     *
     * @param query 搜索关键词
     * @return Flow<SearchResult> 搜索结果
     */
    private fun performSearch(query: String): Flow<SearchResult> {
        Log.d(TAG, "🔍 执行搜索: '$query'")
        
        // 空查询返回空结果
        if (query.isBlank()) {
            return flowOf(SearchResult.Empty)
        }
        
        // 查询长度不足
        if (query.length < MIN_SEARCH_LENGTH) {
            return flowOf(SearchResult.Empty)
        }
        
        return translationHistoryRepository.searchHistory(query)
            .map { historyList ->
                Log.d(TAG, "📊 搜索到 ${historyList.size} 条结果")
                
                if (historyList.isEmpty()) {
                    SearchResult.NoResults(query)
                } else {
                    // 计算相关性并排序
                    val rankedResults = rankSearchResults(historyList, query)
                    
                    // 生成搜索结果项
                    val searchItems = rankedResults.map { (history, score) ->
                        createSearchItem(history, query, score)
                    }
                    
                    SearchResult.Success(
                        query = query,
                        items = searchItems,
                        totalCount = searchItems.size
                    )
                }
            }
    }

    /**
     * 对搜索结果进行相关性排序
     *
     * 🔧 排序算法：
     * 1. 完全匹配优先级最高
     * 2. 开头匹配优先级较高
     * 3. 包含匹配优先级中等
     * 4. 考虑匹配字段的重要性
     * 5. 考虑使用频率和时间
     *
     * @param historyList 搜索结果列表
     * @param query 搜索关键词
     * @return 排序后的结果列表，包含相关性分数
     */
    private fun rankSearchResults(
        historyList: List<TranslationHistory>,
        query: String
    ): List<Pair<TranslationHistory, Double>> {
        return historyList.map { history ->
            val score = calculateRelevanceScore(history, query)
            history to score
        }.sortedByDescending { it.second }
    }

    /**
     * 计算相关性分数
     *
     * @param history 翻译历史记录
     * @param query 搜索关键词
     * @return 相关性分数 (0.0 - 1.0)
     */
    private fun calculateRelevanceScore(
        history: TranslationHistory,
        query: String
    ): Double {
        val queryLower = query.lowercase()
        var score = 0.0
        
        // 原文匹配 (权重: 0.4)
        score += calculateFieldScore(history.originalText, queryLower) * 0.4
        
        // 译文匹配 (权重: 0.4)
        score += calculateFieldScore(history.translatedText, queryLower) * 0.4
        
        // 语言名称匹配 (权重: 0.1)
        score += calculateFieldScore(history.sourceLanguageName, queryLower) * 0.05
        score += calculateFieldScore(history.targetLanguageName, queryLower) * 0.05
        
        // 标签匹配 (权重: 0.1)
        val tagScore = history.tags.maxOfOrNull { tag ->
            calculateFieldScore(tag, queryLower)
        } ?: 0.0
        score += tagScore * 0.1
        
        // 使用频率加权 (最多增加20%)
        val usageBonus = minOf(history.usageCount * 0.02, 0.2)
        score += usageBonus
        
        // 时间新鲜度加权 (最多增加10%)
        val daysSinceCreation = (System.currentTimeMillis() - history.timestamp) / (24 * 60 * 60 * 1000)
        val freshnessBonus = maxOf(0.0, 0.1 - daysSinceCreation * 0.001)
        score += freshnessBonus
        
        return minOf(score, 1.0) // 确保分数不超过1.0
    }

    /**
     * 计算单个字段的匹配分数
     *
     * @param field 字段内容
     * @param query 搜索关键词
     * @return 匹配分数 (0.0 - 1.0)
     */
    private fun calculateFieldScore(field: String, query: String): Double {
        val fieldLower = field.lowercase()
        
        return when {
            // 完全匹配
            fieldLower == query -> 1.0
            
            // 开头匹配
            fieldLower.startsWith(query) -> 0.8
            
            // 包含匹配
            fieldLower.contains(query) -> {
                // 根据匹配位置和长度计算分数
                val matchIndex = fieldLower.indexOf(query)
                val positionScore = 1.0 - (matchIndex.toDouble() / fieldLower.length) * 0.3
                val lengthScore = query.length.toDouble() / fieldLower.length
                minOf(0.6, positionScore * lengthScore + 0.3)
            }
            
            // 无匹配
            else -> 0.0
        }
    }

    /**
     * 创建搜索结果项
     *
     * @param history 翻译历史记录
     * @param query 搜索关键词
     * @param relevanceScore 相关性分数
     * @return 搜索结果项
     */
    private fun createSearchItem(
        history: TranslationHistory,
        query: String,
        relevanceScore: Double
    ): SearchResultItem {
        return SearchResultItem(
            history = history,
            highlightedOriginalText = highlightText(history.originalText, query),
            highlightedTranslatedText = highlightText(history.translatedText, query),
            relevanceScore = relevanceScore,
            matchedFields = getMatchedFields(history, query)
        )
    }

    /**
     * 高亮显示匹配的文本
     *
     * @param text 原始文本
     * @param query 搜索关键词
     * @return 高亮后的文本信息
     */
    private fun highlightText(text: String, query: String): HighlightedText {
        val queryLower = query.lowercase()
        val textLower = text.lowercase()
        val matchIndex = textLower.indexOf(queryLower)
        
        return if (matchIndex >= 0) {
            HighlightedText(
                fullText = text,
                highlightStart = matchIndex,
                highlightEnd = matchIndex + query.length,
                hasHighlight = true
            )
        } else {
            HighlightedText(
                fullText = text,
                highlightStart = -1,
                highlightEnd = -1,
                hasHighlight = false
            )
        }
    }

    /**
     * 获取匹配的字段列表
     *
     * @param history 翻译历史记录
     * @param query 搜索关键词
     * @return 匹配的字段列表
     */
    private fun getMatchedFields(history: TranslationHistory, query: String): List<String> {
        val matchedFields = mutableListOf<String>()
        val queryLower = query.lowercase()
        
        if (history.originalText.lowercase().contains(queryLower)) {
            matchedFields.add("原文")
        }
        if (history.translatedText.lowercase().contains(queryLower)) {
            matchedFields.add("译文")
        }
        if (history.sourceLanguageName.lowercase().contains(queryLower)) {
            matchedFields.add("源语言")
        }
        if (history.targetLanguageName.lowercase().contains(queryLower)) {
            matchedFields.add("目标语言")
        }
        if (history.tags.any { it.lowercase().contains(queryLower) }) {
            matchedFields.add("标签")
        }
        
        return matchedFields
    }
}

/**
 * 搜索结果密封类
 */
sealed class SearchResult {
    /**
     * 搜索成功
     */
    data class Success(
        val query: String,
        val items: List<SearchResultItem>,
        val totalCount: Int
    ) : SearchResult()
    
    /**
     * 无搜索结果
     */
    data class NoResults(val query: String) : SearchResult()
    
    /**
     * 空搜索
     */
    object Empty : SearchResult()
    
    /**
     * 搜索错误
     */
    data class Error(val message: String) : SearchResult()
    
    /**
     * 检查是否有结果
     */
    fun hasResults(): Boolean = this is Success && items.isNotEmpty()
    
    /**
     * 获取结果数量
     */
    fun getResultCount(): Int = (this as? Success)?.totalCount ?: 0
}

/**
 * 搜索结果项
 */
data class SearchResultItem(
    val history: TranslationHistory,
    val highlightedOriginalText: HighlightedText,
    val highlightedTranslatedText: HighlightedText,
    val relevanceScore: Double,
    val matchedFields: List<String>
)

/**
 * 高亮文本信息
 */
data class HighlightedText(
    val fullText: String,
    val highlightStart: Int,
    val highlightEnd: Int,
    val hasHighlight: Boolean
) {
    /**
     * 获取高亮前的文本
     */
    fun getTextBefore(): String = if (hasHighlight && highlightStart > 0) {
        fullText.substring(0, highlightStart)
    } else ""
    
    /**
     * 获取高亮的文本
     */
    fun getHighlightedText(): String = if (hasHighlight) {
        fullText.substring(highlightStart, highlightEnd)
    } else ""
    
    /**
     * 获取高亮后的文本
     */
    fun getTextAfter(): String = if (hasHighlight && highlightEnd < fullText.length) {
        fullText.substring(highlightEnd)
    } else if (!hasHighlight) fullText else ""
}
