package com.example.mytranslator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.mytranslator.data.local.database.TranslationDatabase
import com.example.mytranslator.di.TranslationDependencyContainer
import com.example.mytranslator.domain.usecase.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 历史记录ViewModel工厂类
 *
 * 🎯 设计目的：
 * 1. 创建和管理ViewModel实例
 * 2. 处理依赖注入和对象创建
 * 3. 提供统一的ViewModel创建入口
 * 4. 管理ViewModel的生命周期
 *
 * 🏗️ 工厂模式设计：
 * - 封装创建逻辑：隐藏复杂的对象创建过程
 * - 依赖管理：统一管理ViewModel的依赖关系
 * - 类型安全：提供类型安全的ViewModel创建
 * - 扩展性：便于添加新的ViewModel类型
 *
 * 📱 支持的ViewModel：
 * - TranslationHistoryViewModel：主历史记录界面
 * - SearchHistoryViewModel：搜索功能界面
 * - 未来可扩展更多ViewModel
 *
 * 🎓 学习要点：
 * ViewModelProvider.Factory的使用：
 * 1. 工厂模式 - 封装对象创建逻辑
 * 2. 依赖注入 - 管理ViewModel的依赖
 * 3. 生命周期 - 配合ViewModelProvider管理生命周期
 * 4. 类型安全 - 使用泛型确保类型安全
 */
class HistoryViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {

    companion object {
        private const val TAG = "HistoryViewModelFactory"

        /**
         * 创建工厂实例的便捷方法
         */
        fun create(context: android.content.Context): HistoryViewModelFactory {
            return HistoryViewModelFactory(context)
        }
    }

    /**
     * 创建ViewModel实例
     *
     * 🔧 创建流程：
     * 1. 检查ViewModel类型
     * 2. 创建必要的依赖对象
     * 3. 实例化ViewModel
     * 4. 返回类型安全的实例
     *
     * @param modelClass ViewModel类
     * @return ViewModel实例
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TranslationHistoryViewModel::class.java) -> {
                createTranslationHistoryViewModel() as T
            }
            
            modelClass.isAssignableFrom(SearchHistoryViewModel::class.java) -> {
                createSearchHistoryViewModel() as T
            }
            
            else -> {
                throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
            }
        }
    }

    /**
     * 创建TranslationHistoryViewModel
     *
     * 🔧 依赖创建：
     * 1. 使用依赖容器获取Use Cases
     * 2. 组装ViewModel
     */
    private fun createTranslationHistoryViewModel(): TranslationHistoryViewModel {
        // 使用依赖容器获取Use Cases
        val getHistoryUseCase = TranslationDependencyContainer.getGetHistoryUseCase(context)
        val searchHistoryUseCase = TranslationDependencyContainer.getSearchHistoryUseCase(context)
        val manageFavoriteUseCase = TranslationDependencyContainer.getManageFavoriteUseCase(context)
        val deleteHistoryUseCase = TranslationDependencyContainer.getDeleteHistoryUseCase(context)
        val saveTranslationUseCase = TranslationDependencyContainer.getSaveTranslationUseCase(context)

        // 创建ViewModel
        return TranslationHistoryViewModel(
            getHistoryUseCase = getHistoryUseCase,
            searchHistoryUseCase = searchHistoryUseCase,
            manageFavoriteUseCase = manageFavoriteUseCase,
            deleteHistoryUseCase = deleteHistoryUseCase,
            saveTranslationUseCase = saveTranslationUseCase
        )
    }

    /**
     * 创建SearchHistoryViewModel
     */
    private fun createSearchHistoryViewModel(): SearchHistoryViewModel {
        // 使用依赖容器获取Use Case
        val searchHistoryUseCase = TranslationDependencyContainer.getSearchHistoryUseCase(context)

        // 创建ViewModel
        return SearchHistoryViewModel(searchHistoryUseCase)
    }
}

/**
 * ViewModel创建扩展函数
 *
 * 🎯 便捷方法：
 * 为Activity和Fragment提供便捷的ViewModel创建方法
 */

/**
 * 为Activity创建TranslationHistoryViewModel
 */
fun androidx.activity.ComponentActivity.createTranslationHistoryViewModel(): TranslationHistoryViewModel {
    val factory = HistoryViewModelFactory.create(this)
    return ViewModelProvider(this, factory)[TranslationHistoryViewModel::class.java]
}

/**
 * 为Fragment创建TranslationHistoryViewModel
 */
fun androidx.fragment.app.Fragment.createTranslationHistoryViewModel(): TranslationHistoryViewModel {
    val factory = HistoryViewModelFactory.create(requireContext())
    return ViewModelProvider(this, factory)[TranslationHistoryViewModel::class.java]
}

/**
 * 为Activity创建SearchHistoryViewModel
 */
fun androidx.activity.ComponentActivity.createSearchHistoryViewModel(): SearchHistoryViewModel {
    val factory = HistoryViewModelFactory.create(this)
    return ViewModelProvider(this, factory)[SearchHistoryViewModel::class.java]
}

/**
 * 为Fragment创建SearchHistoryViewModel
 */
fun androidx.fragment.app.Fragment.createSearchHistoryViewModel(): SearchHistoryViewModel {
    val factory = HistoryViewModelFactory.create(requireContext())
    return ViewModelProvider(this, factory)[SearchHistoryViewModel::class.java]
}

/**
 * 历史记录Use Cases容器（已废弃）
 *
 * 🎯 设计说明：
 * 此容器已被TranslationDependencyContainer替代，
 * 保留此处仅为向后兼容。
 */
@Deprecated("使用TranslationDependencyContainer替代")
object HistoryDependencyContainer {

    /**
     * 获取TranslationDatabase实例
     */
    fun getDatabase(context: android.content.Context): TranslationDatabase {
        return TranslationDatabase.getDatabase(context)
    }

    /**
     * 创建所有Use Cases
     */
    fun createUseCases(context: android.content.Context): HistoryUseCases {
        return HistoryUseCases(
            getHistoryUseCase = TranslationDependencyContainer.getGetHistoryUseCase(context),
            searchHistoryUseCase = TranslationDependencyContainer.getSearchHistoryUseCase(context),
            manageFavoriteUseCase = TranslationDependencyContainer.getManageFavoriteUseCase(context),
            deleteHistoryUseCase = TranslationDependencyContainer.getDeleteHistoryUseCase(context),
            saveTranslationUseCase = TranslationDependencyContainer.getSaveTranslationUseCase(context)
        )
    }
}

/**
 * Use Cases容器
 *
 * 🎯 设计说明：
 * 将所有相关的Use Cases组织在一起，
 * 便于管理和传递。
 */
data class HistoryUseCases(
    val getHistoryUseCase: GetHistoryUseCase,
    val searchHistoryUseCase: SearchHistoryUseCase,
    val manageFavoriteUseCase: ManageFavoriteUseCase,
    val deleteHistoryUseCase: DeleteHistoryUseCase,
    val saveTranslationUseCase: SaveTranslationUseCase
)

/**
 * ViewModel状态管理扩展
 *
 * 🎯 便捷方法：
 * 为ViewModel提供状态管理的便捷方法
 *
 * 注意：这些扩展函数将在UI层实现时提供具体实现
 */
