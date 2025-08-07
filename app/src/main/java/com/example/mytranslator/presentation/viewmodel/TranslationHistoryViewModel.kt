package com.example.mytranslator.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytranslator.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 翻译历史记录ViewModel
 *
 * 🎯 设计目的：
 * 1. 管理翻译历史记录的UI状态
 * 2. 协调多个Use Cases的执行
 * 3. 处理用户交互和界面逻辑
 * 4. 提供响应式的数据流给UI层
 *
 * 🏗️ MVVM架构设计：
 * - 状态管理：使用StateFlow管理UI状态
 * - 事件处理：使用SharedFlow处理一次性事件
 * - 生命周期：利用viewModelScope管理协程
 * - 数据绑定：提供UI友好的数据格式
 *
 * 📱 功能覆盖：
 * - 历史记录列表显示
 * - 搜索和筛选功能
 * - 收藏管理
 * - 删除和清理操作
 *
 * 🎓 学习要点：
 * ViewModel的核心概念：
 * 1. 状态管理 - 使用StateFlow管理可观察状态
 * 2. 事件处理 - 区分状态和一次性事件
 * 3. 协程使用 - 在viewModelScope中执行异步操作
 * 4. 错误处理 - 统一的错误处理和用户反馈
 */
class TranslationHistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val searchHistoryUseCase: SearchHistoryUseCase,
    private val manageFavoriteUseCase: ManageFavoriteUseCase,
    private val deleteHistoryUseCase: DeleteHistoryUseCase,
    private val saveTranslationUseCase: SaveTranslationUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "TranslationHistoryVM"
    }

    // ===== UI状态管理 =====

    /**
     * 历史记录UI状态
     */
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /**
     * 搜索查询状态
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 一次性事件流
     */
    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    // ===== 数据流 =====

    /**
     * 历史记录数据流
     */
    val historyData: StateFlow<HistoryResult> = combine(
        _uiState.map { it.currentTab },
        _uiState.map { it.sortOption },
        _uiState.map { it.groupOption }
    ) { tab, sortOption, groupOption ->
        Triple(tab, sortOption, groupOption)
    }.flatMapLatest { (tab, sortOption, groupOption) ->
        when (tab) {
            HistoryTab.ALL -> getHistoryUseCase.getAllHistory(sortOption, groupOption)
            HistoryTab.FAVORITES -> getHistoryUseCase.getFavoriteHistory(sortOption)
            HistoryTab.TODAY -> getHistoryUseCase.getTodayHistory()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryResult.Loading
    )

    /**
     * 搜索结果数据流
     */
    val searchResults: StateFlow<SearchResult> = searchHistoryUseCase
        .searchHistory(_searchQuery)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResult.Empty
        )

    init {
        Log.d(TAG, "🚀 TranslationHistoryViewModel 初始化")
        
        // 监听搜索状态变化
        viewModelScope.launch {
            _searchQuery.collect { query ->
                updateUiState { it.copy(isSearchMode = query.isNotBlank()) }
            }
        }
    }

    // ===== 用户交互处理 =====

    /**
     * 切换标签页
     */
    fun switchTab(tab: HistoryTab) {
        Log.d(TAG, "📑 切换标签页: $tab")
        updateUiState { it.copy(currentTab = tab) }
    }

    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        Log.d(TAG, "🔍 更新搜索查询: '$query'")
        _searchQuery.value = query
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        Log.d(TAG, "🔍 清空搜索")
        _searchQuery.value = ""
    }

    /**
     * 更新排序选项
     */
    fun updateSortOption(sortOption: SortOption) {
        Log.d(TAG, "📊 更新排序选项: $sortOption")
        updateUiState { it.copy(sortOption = sortOption) }
    }

    /**
     * 更新分组选项
     */
    fun updateGroupOption(groupOption: GroupOption) {
        Log.d(TAG, "📊 更新分组选项: $groupOption")
        updateUiState { it.copy(groupOption = groupOption) }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(translationId: String) {
        Log.d(TAG, "⭐ 切换收藏状态: $translationId")
        
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true) }
            
            try {
                val result = manageFavoriteUseCase.toggleFavorite(translationId)
                
                when (result) {
                    is FavoriteResult.Success -> {
                        val message = when (result.action) {
                            FavoriteAction.ADDED -> "已添加到收藏"
                            FavoriteAction.REMOVED -> "已取消收藏"
                            FavoriteAction.ALREADY_ADDED -> "已经是收藏状态"
                            FavoriteAction.ALREADY_REMOVED -> "已经是非收藏状态"
                        }
                        emitEvent(HistoryEvent.ShowMessage(message))
                    }
                    
                    else -> {
                        val errorMessage = result.getErrorMessage() ?: "操作失败"
                        emitEvent(HistoryEvent.ShowError(errorMessage))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 切换收藏状态失败", e)
                emitEvent(HistoryEvent.ShowError("操作失败: ${e.message}"))
            } finally {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 删除单个翻译记录
     */
    fun deleteTranslation(translationId: String, forceDelete: Boolean = false) {
        Log.d(TAG, "🗑️ 删除翻译记录: $translationId")
        
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true) }
            
            try {
                val result = deleteHistoryUseCase.deleteTranslation(translationId, forceDelete)
                
                when (result) {
                    is DeleteResult.Success -> {
                        emitEvent(HistoryEvent.ShowMessage("删除成功"))
                    }
                    
                    is DeleteResult.ProtectionError -> {
                        emitEvent(HistoryEvent.ShowConfirmDialog(
                            title = "确认删除",
                            message = result.message + "\n\n是否强制删除？",
                            onConfirm = { deleteTranslation(translationId, true) }
                        ))
                    }
                    
                    else -> {
                        val errorMessage = result.getErrorMessage() ?: "删除失败"
                        emitEvent(HistoryEvent.ShowError(errorMessage))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 删除翻译记录失败", e)
                emitEvent(HistoryEvent.ShowError("删除失败: ${e.message}"))
            } finally {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 批量删除翻译记录
     */
    fun deleteTranslationsBatch(translationIds: List<String>, forceDelete: Boolean = false) {
        Log.d(TAG, "🗑️ 批量删除翻译记录: ${translationIds.size} 条")
        
        if (translationIds.isEmpty()) {
            emitEvent(HistoryEvent.ShowError("请选择要删除的记录"))
            return
        }
        
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true) }
            
            try {
                val result = deleteHistoryUseCase.deleteTranslationsBatch(translationIds, forceDelete)
                
                when (result) {
                    is DeleteResult.Success -> {
                        val message = "成功删除 ${result.deletedCount} 条记录"
                        emitEvent(HistoryEvent.ShowMessage(message))
                        
                        // 清空选择状态
                        updateUiState { it.copy(selectedItems = emptySet()) }
                    }
                    
                    else -> {
                        val errorMessage = result.getErrorMessage() ?: "批量删除失败"
                        emitEvent(HistoryEvent.ShowError(errorMessage))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批量删除失败", e)
                emitEvent(HistoryEvent.ShowError("批量删除失败: ${e.message}"))
            } finally {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearAllHistory(keepFavorites: Boolean = true) {
        Log.d(TAG, "🗑️ 清空所有历史记录 (保留收藏: $keepFavorites)")
        
        val message = if (keepFavorites) {
            "确定要清空所有非收藏记录吗？此操作不可撤销。"
        } else {
            "确定要清空所有历史记录吗？此操作不可撤销。"
        }
        
        emitEvent(HistoryEvent.ShowConfirmDialog(
            title = "确认清空",
            message = message,
            onConfirm = { performClearAllHistory(keepFavorites) }
        ))
    }

    /**
     * 执行清空所有历史记录
     */
    private fun performClearAllHistory(keepFavorites: Boolean) {
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true) }
            
            try {
                val result = deleteHistoryUseCase.clearAllHistory(
                    keepFavorites = keepFavorites,
                    confirmationToken = "CONFIRM_CLEAR_ALL"
                )
                
                when (result) {
                    is DeleteResult.Success -> {
                        val message = if (keepFavorites) "已清空非收藏记录" else "已清空所有记录"
                        emitEvent(HistoryEvent.ShowMessage(message))
                    }
                    
                    else -> {
                        val errorMessage = result.getErrorMessage() ?: "清空失败"
                        emitEvent(HistoryEvent.ShowError(errorMessage))
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 清空历史记录失败", e)
                emitEvent(HistoryEvent.ShowError("清空失败: ${e.message}"))
            } finally {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    // ===== 选择模式管理 =====

    /**
     * 切换选择模式
     */
    fun toggleSelectionMode() {
        val currentState = _uiState.value
        updateUiState { 
            it.copy(
                isSelectionMode = !currentState.isSelectionMode,
                selectedItems = if (!currentState.isSelectionMode) emptySet() else currentState.selectedItems
            )
        }
    }

    /**
     * 选择/取消选择项目
     */
    fun toggleItemSelection(translationId: String) {
        val currentSelected = _uiState.value.selectedItems
        val newSelected = if (currentSelected.contains(translationId)) {
            currentSelected - translationId
        } else {
            currentSelected + translationId
        }
        
        updateUiState { it.copy(selectedItems = newSelected) }
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll(allItemIds: List<String>) {
        val currentSelected = _uiState.value.selectedItems
        val newSelected = if (currentSelected.size == allItemIds.size) {
            emptySet() // 取消全选
        } else {
            allItemIds.toSet() // 全选
        }
        
        updateUiState { it.copy(selectedItems = newSelected) }
    }

    // ===== 辅助方法 =====

    /**
     * 更新UI状态
     */
    private fun updateUiState(update: (HistoryUiState) -> HistoryUiState) {
        _uiState.value = update(_uiState.value)
    }

    /**
     * 发送事件
     */
    private fun emitEvent(event: HistoryEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    /**
     * 处理错误
     */
    private fun handleError(error: Throwable, operation: String) {
        Log.e(TAG, "❌ $operation 失败", error)
        emitEvent(HistoryEvent.ShowError("$operation 失败: ${error.message}"))
    }
}

/**
 * 历史记录标签页枚举
 */
enum class HistoryTab {
    ALL,        // 全部记录
    FAVORITES,  // 收藏记录
    TODAY       // 今日记录
}

/**
 * 历史记录UI状态
 *
 * 🎯 设计说明：
 * 封装历史记录界面的所有状态信息，
 * 使用不可变数据类确保状态的一致性。
 */
data class HistoryUiState(
    // 基本状态
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentTab: HistoryTab = HistoryTab.ALL,

    // 搜索状态
    val isSearchMode: Boolean = false,

    // 排序和分组
    val sortOption: SortOption = SortOption.TIMESTAMP_DESC,
    val groupOption: GroupOption = GroupOption.BY_DATE,

    // 选择模式
    val isSelectionMode: Boolean = false,
    val selectedItems: Set<String> = emptySet(),

    // 错误状态
    val errorMessage: String? = null
) {
    /**
     * 是否有选中的项目
     */
    fun hasSelectedItems(): Boolean = selectedItems.isNotEmpty()

    /**
     * 获取选中项目数量
     */
    fun getSelectedCount(): Int = selectedItems.size

    /**
     * 检查项目是否被选中
     */
    fun isItemSelected(itemId: String): Boolean = selectedItems.contains(itemId)
}

/**
 * 历史记录事件密封类
 *
 * 🎯 设计说明：
 * 定义历史记录界面的一次性事件，
 * 如Toast消息、对话框显示等。
 */
sealed class HistoryEvent {
    /**
     * 显示消息
     */
    data class ShowMessage(val message: String) : HistoryEvent()

    /**
     * 显示错误
     */
    data class ShowError(val message: String) : HistoryEvent()

    /**
     * 显示确认对话框
     */
    data class ShowConfirmDialog(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit
    ) : HistoryEvent()

    /**
     * 导航到详情页面
     */
    data class NavigateToDetail(val translationId: String) : HistoryEvent()

    /**
     * 导航到搜索页面
     */
    object NavigateToSearch : HistoryEvent()

    /**
     * 刷新数据
     */
    object RefreshData : HistoryEvent()
}
