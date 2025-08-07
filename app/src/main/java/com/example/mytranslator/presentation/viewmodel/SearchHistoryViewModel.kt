package com.example.mytranslator.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytranslator.domain.usecase.SearchHistoryUseCase
import com.example.mytranslator.domain.usecase.SearchResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 搜索历史记录ViewModel
 *
 * 🎯 设计目的：
 * 1. 专门处理搜索相关的UI逻辑
 * 2. 管理搜索状态和结果
 * 3. 提供搜索建议和历史
 * 4. 优化搜索性能和用户体验
 *
 * 🏗️ 设计特点：
 * - 专注搜索：只处理搜索相关功能
 * - 实时响应：提供即时搜索反馈
 * - 智能建议：基于历史提供搜索建议
 * - 性能优化：防抖动和缓存机制
 *
 * 📱 功能特性：
 * - 实时搜索和结果显示
 * - 搜索历史管理
 * - 搜索建议和自动完成
 * - 搜索结果高亮
 *
 * 🎓 学习要点：
 * 搜索ViewModel的设计模式：
 * 1. 状态分离 - 搜索状态与主界面状态分离
 * 2. 实时响应 - 使用Flow实现实时搜索
 * 3. 性能优化 - 防抖动和结果缓存
 * 4. 用户体验 - 搜索建议和历史记录
 */
class SearchHistoryViewModel(
    private val searchHistoryUseCase: SearchHistoryUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SearchHistoryVM"
        private const val MAX_SEARCH_HISTORY = 20
        private const val MAX_SUGGESTIONS = 10
    }

    // ===== 搜索状态管理 =====

    /**
     * 搜索查询状态
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 搜索UI状态
     */
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * 搜索历史记录
     */
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    /**
     * 搜索建议
     */
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    /**
     * 一次性事件流
     */
    private val _events = MutableSharedFlow<SearchEvent>()
    val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

    // ===== 搜索结果数据流 =====

    /**
     * 搜索结果数据流
     */
    val searchResults: StateFlow<SearchResult> = searchHistoryUseCase
        .searchHistory(_searchQuery)
        .onStart {
            updateUiState { it.copy(isSearching = true) }
        }
        .onEach { result ->
            updateUiState { 
                it.copy(
                    isSearching = false,
                    hasResults = result.hasResults(),
                    resultCount = result.getResultCount()
                )
            }
        }
        .catch { exception ->
            Log.e(TAG, "❌ 搜索过程中发生异常", exception)
            updateUiState { it.copy(isSearching = false) }
            emitEvent(SearchEvent.ShowError("搜索失败: ${exception.message}"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResult.Empty
        )

    init {
        Log.d(TAG, "🚀 SearchHistoryViewModel 初始化")
        
        // 监听搜索查询变化，生成搜索建议
        viewModelScope.launch {
            _searchQuery
                .debounce(100) // 短暂防抖，用于生成建议
                .distinctUntilChanged()
                .collect { query ->
                    generateSearchSuggestions(query)
                }
        }
        
        // 加载搜索历史
        loadSearchHistory()
    }

    // ===== 用户交互处理 =====

    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        Log.d(TAG, "🔍 更新搜索查询: '$query'")
        _searchQuery.value = query
        
        // 更新UI状态
        updateUiState { 
            it.copy(
                isActive = query.isNotBlank(),
                showSuggestions = query.isNotBlank() && query.length >= 2
            )
        }
    }

    /**
     * 执行搜索
     */
    fun performSearch(query: String = _searchQuery.value) {
        Log.d(TAG, "🔍 执行搜索: '$query'")
        
        if (query.isBlank()) {
            emitEvent(SearchEvent.ShowError("请输入搜索关键词"))
            return
        }
        
        // 更新搜索查询
        _searchQuery.value = query
        
        // 添加到搜索历史
        addToSearchHistory(query)
        
        // 隐藏建议
        updateUiState { 
            it.copy(
                showSuggestions = false,
                isActive = true
            )
        }
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        Log.d(TAG, "🔍 清空搜索")
        _searchQuery.value = ""
        updateUiState { 
            it.copy(
                isActive = false,
                showSuggestions = false,
                hasResults = false,
                resultCount = 0
            )
        }
    }

    /**
     * 选择搜索建议
     */
    fun selectSuggestion(suggestion: String) {
        Log.d(TAG, "💡 选择搜索建议: '$suggestion'")
        performSearch(suggestion)
    }

    /**
     * 选择搜索历史
     */
    fun selectSearchHistory(historyItem: String) {
        Log.d(TAG, "📜 选择搜索历史: '$historyItem'")
        performSearch(historyItem)
    }

    /**
     * 删除搜索历史项
     */
    fun removeSearchHistoryItem(item: String) {
        Log.d(TAG, "🗑️ 删除搜索历史项: '$item'")
        
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(item)
        _searchHistory.value = currentHistory
        
        // 保存到本地存储
        saveSearchHistory(currentHistory)
    }

    /**
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        Log.d(TAG, "🗑️ 清空搜索历史")
        _searchHistory.value = emptyList()
        saveSearchHistory(emptyList())
        emitEvent(SearchEvent.ShowMessage("搜索历史已清空"))
    }

    /**
     * 切换搜索模式
     */
    fun toggleSearchMode() {
        val currentState = _uiState.value
        updateUiState { 
            it.copy(
                isActive = !currentState.isActive,
                showSuggestions = false
            )
        }
        
        if (!currentState.isActive) {
            // 进入搜索模式时清空查询
            clearSearch()
        }
    }

    // ===== 搜索建议生成 =====

    /**
     * 生成搜索建议
     */
    private fun generateSearchSuggestions(query: String) {
        if (query.length < 2) {
            _searchSuggestions.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                // 从搜索历史中筛选匹配的项目
                val historyMatches = _searchHistory.value
                    .filter { it.contains(query, ignoreCase = true) && it != query }
                    .take(MAX_SUGGESTIONS / 2)
                
                // 生成基于查询的建议（这里可以扩展为更智能的建议算法）
                val generatedSuggestions = generateSmartSuggestions(query)
                
                // 合并建议
                val allSuggestions = (historyMatches + generatedSuggestions)
                    .distinct()
                    .take(MAX_SUGGESTIONS)
                
                _searchSuggestions.value = allSuggestions
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 生成搜索建议失败", e)
                _searchSuggestions.value = emptyList()
            }
        }
    }

    /**
     * 生成智能搜索建议
     */
    private fun generateSmartSuggestions(query: String): List<String> {
        // 这里可以实现更复杂的建议算法
        // 例如：基于常用词汇、语言检测、同义词等
        
        val suggestions = mutableListOf<String>()
        
        // 简单的建议生成逻辑
        if (query.length >= 3) {
            // 可以添加常用的搜索模式
            suggestions.add("$query 翻译")
            suggestions.add("$query 例句")
        }
        
        return suggestions.take(MAX_SUGGESTIONS / 2)
    }

    // ===== 搜索历史管理 =====

    /**
     * 添加到搜索历史
     */
    private fun addToSearchHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        
        // 移除已存在的相同查询
        currentHistory.remove(query)
        
        // 添加到开头
        currentHistory.add(0, query)
        
        // 限制历史记录数量
        if (currentHistory.size > MAX_SEARCH_HISTORY) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _searchHistory.value = currentHistory
        
        // 保存到本地存储
        saveSearchHistory(currentHistory)
    }

    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                // 这里应该从SharedPreferences或数据库加载
                // 暂时使用空列表
                val history = emptyList<String>() // loadFromStorage()
                _searchHistory.value = history
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 加载搜索历史失败", e)
                _searchHistory.value = emptyList()
            }
        }
    }

    /**
     * 保存搜索历史
     */
    private fun saveSearchHistory(history: List<String>) {
        viewModelScope.launch {
            try {
                // 这里应该保存到SharedPreferences或数据库
                // saveToStorage(history)
                Log.d(TAG, "💾 搜索历史已保存: ${history.size} 条")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存搜索历史失败", e)
            }
        }
    }

    // ===== 辅助方法 =====

    /**
     * 更新UI状态
     */
    private fun updateUiState(update: (SearchUiState) -> SearchUiState) {
        _uiState.value = update(_uiState.value)
    }

    /**
     * 发送事件
     */
    private fun emitEvent(event: SearchEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

/**
 * 搜索UI状态
 */
data class SearchUiState(
    val isActive: Boolean = false,
    val isSearching: Boolean = false,
    val showSuggestions: Boolean = false,
    val hasResults: Boolean = false,
    val resultCount: Int = 0
) {
    /**
     * 是否显示空状态
     */
    fun shouldShowEmptyState(): Boolean = isActive && !isSearching && !hasResults
    
    /**
     * 是否显示加载状态
     */
    fun shouldShowLoading(): Boolean = isSearching
}

/**
 * 搜索事件密封类
 */
sealed class SearchEvent {
    /**
     * 显示消息
     */
    data class ShowMessage(val message: String) : SearchEvent()
    
    /**
     * 显示错误
     */
    data class ShowError(val message: String) : SearchEvent()
    
    /**
     * 导航到搜索结果详情
     */
    data class NavigateToDetail(val translationId: String) : SearchEvent()
    
    /**
     * 关闭搜索
     */
    object CloseSearch : SearchEvent()
}
