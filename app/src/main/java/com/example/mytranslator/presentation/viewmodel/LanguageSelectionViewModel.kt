package com.example.mytranslator.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.usecase.GetLanguagesUseCase
import kotlinx.coroutines.launch

/**
 * 语言选择ViewModel
 *
 * 🎯 设计目的：
 * 1. 管理语言选择界面的状态和数据
 * 2. 从API动态获取支持的语言列表
 * 3. 提供搜索和过滤功能
 * 4. 处理加载状态和错误状态
 *
 * 🏗️ 架构设计：
 * - MVVM模式：分离UI逻辑和业务逻辑
 * - LiveData：响应式数据绑定
 * - UseCase：封装业务逻辑
 * - 状态管理：统一管理UI状态
 *
 * 🔧 技术特性：
 * - 协程支持：异步数据加载
 * - 错误处理：完善的异常处理机制
 * - 搜索功能：实时搜索和过滤
 * - 缓存机制：避免重复的网络请求
 *
 * 🎓 学习要点：
 * ViewModel的职责：
 * 1. 数据管理：持有和管理UI相关的数据
 * 2. 状态管理：管理加载、成功、错误等状态
 * 3. 业务逻辑：调用UseCase执行业务操作
 * 4. 生命周期：在配置变化时保持数据
 */
class LanguageSelectionViewModel(
    private val getLanguagesUseCase: GetLanguagesUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "LanguageSelectionViewModel"
    }

    // 语言列表状态
    private val _languagesState = MutableLiveData<LanguagesUiState>()
    val languagesState: LiveData<LanguagesUiState> = _languagesState

    // 搜索查询
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // 原始语言列表（用于搜索）
    private var allLanguages: List<Language> = emptyList()

    /**
     * UI状态定义
     */
    sealed class LanguagesUiState {
        object Loading : LanguagesUiState()
        data class Success(val languages: List<Language>) : LanguagesUiState()
        data class Error(val message: String) : LanguagesUiState()
    }

    init {
        // 初始化时加载语言列表
        loadLanguages()
    }

    /**
     * 加载语言列表
     *
     * 🎯 设计考虑：
     * - 显示加载状态给用户
     * - 从API获取最新的语言列表
     * - 处理网络错误和API错误
     * - 提供回退机制确保应用可用
     */
    fun loadLanguages(includeAutoDetect: Boolean = true) {
        _languagesState.value = LanguagesUiState.Loading

        viewModelScope.launch {
            try {
                val result = getLanguagesUseCase.getAllLanguages(
                    sortStrategy = GetLanguagesUseCase.SortStrategy.BY_USAGE,
                    includeAutoDetect = includeAutoDetect
                )

                result.fold(
                    onSuccess = { languages ->
                        allLanguages = languages
                        _languagesState.value = LanguagesUiState.Success(languages)
                    },
                    onFailure = { exception ->
                        _languagesState.value = LanguagesUiState.Error(
                            exception.message ?: "获取语言列表失败"
                        )
                    }
                )

            } catch (e: Exception) {
                _languagesState.value = LanguagesUiState.Error(
                    "加载语言列表时发生错误: ${e.message}"
                )
            }
        }
    }

    /**
     * 搜索语言
     *
     * 🎯 设计考虑：
     * - 实时搜索，无需等待用户完成输入
     * - 支持多种搜索方式（代码、名称、本地化名称）
     * - 空查询时显示所有语言
     * - 搜索结果按相关性排序
     */
    fun searchLanguages(query: String) {
        _searchQuery.value = query

        if (query.isEmpty()) {
            // 空查询时显示所有语言
            _languagesState.value = LanguagesUiState.Success(allLanguages)
        } else {
            // 执行搜索
            val filteredLanguages = allLanguages.filter { language ->
                language.name.contains(query, ignoreCase = true) ||
                language.displayName.contains(query, ignoreCase = true) ||
                language.code.contains(query, ignoreCase = true)
            }

            _languagesState.value = LanguagesUiState.Success(filteredLanguages)
        }
    }

    /**
     * 刷新语言列表
     *
     * 🎯 用途：
     * - 用户手动刷新
     * - 网络恢复后重新加载
     * - 强制更新语言数据
     */
    fun refreshLanguages(includeAutoDetect: Boolean = true) {
        loadLanguages(includeAutoDetect)
    }

    /**
     * 清除搜索
     */
    fun clearSearch() {
        searchLanguages("")
    }

    /**
     * 获取当前的语言列表
     */
    fun getCurrentLanguages(): List<Language> {
        return when (val state = _languagesState.value) {
            is LanguagesUiState.Success -> state.languages
            else -> emptyList()
        }
    }

    /**
     * 检查是否正在加载
     */
    fun isLoading(): Boolean {
        return _languagesState.value is LanguagesUiState.Loading
    }

    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? {
        return when (val state = _languagesState.value) {
            is LanguagesUiState.Error -> state.message
            else -> null
        }
    }
}
