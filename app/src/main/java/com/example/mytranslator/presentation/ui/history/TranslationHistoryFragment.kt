package com.example.mytranslator.presentation.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mytranslator.R
import com.example.mytranslator.data.local.database.TranslationDatabase
import com.example.mytranslator.databinding.FragmentTranslationHistoryBinding
import com.example.mytranslator.domain.usecase.HistoryResult
import com.example.mytranslator.domain.usecase.HistoryGroupData
import com.example.mytranslator.domain.usecase.SortOption
import com.example.mytranslator.domain.model.TranslationHistory
import com.example.mytranslator.presentation.viewmodel.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 翻译历史记录Fragment
 *
 * 🎯 设计目的：
 * 1. 展示翻译历史记录列表
 * 2. 提供搜索、筛选、排序功能
 * 3. 支持收藏、删除等操作
 * 4. 实现流畅的用户交互体验
 *
 * 🏗️ UI架构设计：
 * - MVVM模式：使用ViewModel管理状态
 * - ViewBinding：类型安全的视图绑定
 * - RecyclerView：高效的列表展示
 * - Material Design：现代化的UI设计
 *
 * 📱 功能特性：
 * - 多标签页展示（全部、收藏、今日）
 * - 实时搜索和筛选
 * - 批量选择和操作
 * - 下拉刷新和加载更多
 *
 * 🎓 学习要点：
 * Fragment的现代化开发模式：
 * 1. ViewBinding - 替代findViewById
 * 2. ViewModel - 状态管理和业务逻辑
 * 3. Flow - 响应式数据绑定
 * 4. Lifecycle - 生命周期感知
 */
class TranslationHistoryFragment : Fragment() {

    companion object {
        private const val TAG = "TranslationHistoryFragment"
        
        /**
         * 创建Fragment实例的工厂方法
         */
        fun newInstance(): TranslationHistoryFragment {
            return TranslationHistoryFragment()
        }
    }

    // ===== 视图绑定 =====
    
    private var _binding: FragmentTranslationHistoryBinding? = null
    private val binding get() = _binding!!

    // ===== ViewModel =====
    
    private val viewModel: TranslationHistoryViewModel by viewModels {
        HistoryViewModelFactory.create(requireContext())
    }

    // ===== 适配器 =====
    
    private lateinit var historyAdapter: TranslationHistoryAdapter

    // ===== 生命周期方法 =====

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ===== UI设置 =====

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置标题
        binding.toolbarHistory.title = getString(R.string.history_title)

        // 设置返回按钮
        binding.toolbarHistory.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // 设置标签页
        setupTabLayout()
    }

    /**
     * 设置标签页
     */
    private fun setupTabLayout() {
        with(binding.tabLayoutHistory) {
            addTab(newTab().setText(R.string.history_tab_all))
            addTab(newTab().setText(R.string.history_tab_favorites))
            addTab(newTab().setText(R.string.history_tab_today))
            
            addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    tab?.let {
                        val historyTab = when (it.position) {
                            0 -> HistoryTab.ALL
                            1 -> HistoryTab.FAVORITES
                            2 -> HistoryTab.TODAY
                            else -> HistoryTab.ALL
                        }
                        viewModel.switchTab(historyTab)
                    }
                }
                
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            })
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        historyAdapter = TranslationHistoryAdapter(
            onItemClick = { translation ->
                // 导航到详情页面
                // TODO: 实现详情页面导航
                showTranslationDetail(translation.id)
            },
            onFavoriteClick = { translation ->
                viewModel.toggleFavorite(translation.id)
            },
            onDeleteClick = { translation ->
                viewModel.deleteTranslation(translation.id)
            },
            onItemLongClick = { translation ->
                // 进入选择模式
                viewModel.toggleSelectionMode()
                viewModel.toggleItemSelection(translation.id)
                true
            }
        )
        
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            
            // 添加分隔线
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    requireContext(),
                    androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察UI状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
        
        // 观察历史记录数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyData.collect { result ->
                    handleHistoryResult(result)
                }
            }
        }
        
        // 观察事件
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        // 搜索按钮
        binding.fabSearch.setOnClickListener {
            openSearchDialog()
        }
        
        // 菜单按钮
        binding.toolbarHistory.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                R.id.action_clear -> {
                    showClearDialog()
                    true
                }
                R.id.action_select_all -> {
                    selectAllItems()
                    true
                }
                else -> false
            }
        }
    }

    // ===== 数据处理 =====

    /**
     * 更新UI状态
     */
    private fun updateUI(state: HistoryUiState) {
        // 更新加载状态
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
        
        // 更新选择模式
        updateSelectionMode(state)
        
        // 更新错误状态
        state.errorMessage?.let { message ->
            showError(message)
        }
    }

    /**
     * 处理历史记录结果
     */
    private fun handleHistoryResult(result: HistoryResult) {
        when (result) {
            is HistoryResult.Success -> {
                showHistoryData(result.data)
                hideEmptyState()
            }
            
            is HistoryResult.Error -> {
                showError(result.message)
                showEmptyState("加载失败")
            }
            
            is HistoryResult.Loading -> {
                // 加载状态已通过SwipeRefreshLayout显示
            }
        }
    }

    /**
     * 处理事件
     */
    private fun handleEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.ShowMessage -> {
                showMessage(event.message)
            }
            
            is HistoryEvent.ShowError -> {
                showError(event.message)
            }
            
            is HistoryEvent.ShowConfirmDialog -> {
                showConfirmDialog(event.title, event.message, event.onConfirm)
            }
            
            is HistoryEvent.NavigateToDetail -> {
                showTranslationDetail(event.translationId)
            }
            
            is HistoryEvent.NavigateToSearch -> {
                openSearchDialog()
            }
            
            is HistoryEvent.RefreshData -> {
                refreshData()
            }
        }
    }

    // ===== UI操作方法 =====

    /**
     * 显示历史记录数据
     */
    private fun showHistoryData(data: HistoryGroupData) {
        // 将分组数据转换为平铺列表
        val flatList = data.groups.flatMap { group ->
            listOf(HistoryListItem.Header(group.title, group.count)) + 
            group.items.map { HistoryListItem.Translation(it) }
        }
        
        historyAdapter.submitList(flatList)
        
        // 更新统计信息
        updateStatistics(data.totalCount)
    }

    /**
     * 显示空状态
     */
    private fun showEmptyState(message: String = "暂无翻译记录") {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.textEmptyMessage.text = message
    }

    /**
     * 隐藏空状态
     */
    private fun hideEmptyState() {
        binding.layoutEmpty.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.VISIBLE
    }

    /**
     * 更新选择模式
     */
    private fun updateSelectionMode(state: HistoryUiState) {
        if (state.isSelectionMode) {
            // 进入选择模式
            binding.toolbarHistory.title = "已选择 ${state.getSelectedCount()} 项"
            binding.fabSearch.hide()
            // 显示批量操作按钮
            showBatchActionButtons()
        } else {
            // 退出选择模式
            binding.toolbarHistory.title = getString(R.string.history_title)
            binding.fabSearch.show()
            // 隐藏批量操作按钮
            hideBatchActionButtons()
        }
        
        // 更新适配器的选择状态
        historyAdapter.updateSelectionMode(state.isSelectionMode, state.selectedItems)
    }

    /**
     * 显示批量操作按钮
     */
    private fun showBatchActionButtons() {
        binding.layoutBatchActions.visibility = View.VISIBLE
        
        binding.buttonBatchDelete.setOnClickListener {
            val selectedIds = viewModel.uiState.value.selectedItems.toList()
            viewModel.deleteTranslationsBatch(selectedIds)
        }
        
        binding.buttonBatchFavorite.setOnClickListener {
            // TODO: 实现批量收藏
            showMessage("批量收藏功能开发中")
        }
    }

    /**
     * 隐藏批量操作按钮
     */
    private fun hideBatchActionButtons() {
        binding.layoutBatchActions.visibility = View.GONE
    }

    /**
     * 更新统计信息
     */
    private fun updateStatistics(totalCount: Int) {
        binding.textStatistics.text = "共 $totalCount 条记录"
    }

    // ===== 对话框和交互 =====

    /**
     * 显示排序对话框
     */
    private fun showSortDialog() {
        val options = arrayOf("按时间排序", "按使用频率", "按字母顺序", "按语言对")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("排序方式")
            .setItems(options) { _, which ->
                val sortOption = when (which) {
                    0 -> SortOption.TIMESTAMP_DESC
                    1 -> SortOption.USAGE_COUNT_DESC
                    2 -> SortOption.ALPHABETICAL
                    3 -> SortOption.LANGUAGE_PAIR
                    else -> SortOption.TIMESTAMP_DESC
                }
                viewModel.updateSortOption(sortOption)
            }
            .show()
    }

    /**
     * 显示清空对话框
     */
    private fun showClearDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空历史记录")
            .setMessage("确定要清空所有历史记录吗？此操作不可撤销。")
            .setPositiveButton("清空") { _, _ ->
                viewModel.clearAllHistory(keepFavorites = true)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 打开搜索对话框
     */
    private fun openSearchDialog() {
        // TODO: 实现搜索对话框或导航到搜索页面
        showMessage("搜索功能开发中")
    }

    /**
     * 显示翻译详情
     */
    private fun showTranslationDetail(translationId: String) {
        // TODO: 实现详情页面导航
        showMessage("详情页面开发中")
    }

    /**
     * 全选项目
     */
    private fun selectAllItems() {
        // 获取当前显示的所有项目ID
        val allItemIds = historyAdapter.getAllTranslationIds()
        viewModel.toggleSelectAll(allItemIds)
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        // 数据会通过ViewModel自动刷新
        binding.swipeRefreshLayout.isRefreshing = false
    }

    // ===== 辅助方法 =====

    /**
     * 显示消息
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * 显示错误
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("重试") { refreshData() }
            .show()
    }

    /**
     * 显示确认对话框
     */
    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }
}
