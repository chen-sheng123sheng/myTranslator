package com.example.mytranslator.presentation.ui.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mytranslator.common.utils.LanguageLocalizer
import com.example.mytranslator.databinding.BottomSheetLanguageSelectionBinding
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.presentation.viewmodel.LanguageSelectionViewModel
import com.example.mytranslator.presentation.viewmodel.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 语言选择底部弹窗
 *
 * 🎯 设计目的：
 * 1. 提供优雅的语言选择界面
 * 2. 支持搜索和快速定位
 * 3. 符合 Material Design 规范
 * 4. 良好的用户体验和交互
 *
 * 🎨 UI特性：
 * - 底部滑入动画
 * - 搜索功能
 * - 分组显示（常用语言 + 全部语言）
 * - 当前选中状态指示
 * - 手势拖拽关闭
 *
 * 📱 使用场景：
 * - 源语言选择
 * - 目标语言选择
 * - 语言设置修改
 *
 * 🎓 学习要点：
 * BottomSheetDialogFragment 的使用：
 * 1. 继承 BottomSheetDialogFragment
 * 2. 使用 ViewBinding 管理视图
 * 3. 实现搜索和筛选功能
 * 4. 回调接口传递选择结果
 */
class LanguageSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var languageAdapter: LanguageAdapter
    private var currentSelectedLanguage: Language? = null
    private var onLanguageSelected: ((Language) -> Unit)? = null

    // ViewModel for managing language data
    private val viewModel: LanguageSelectionViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }

    companion object {
        private const val TAG = "LanguageSelectionBottomSheet"
        private const val ARG_CURRENT_LANGUAGE = "current_language"
        private const val ARG_SELECTION_TYPE = "selection_type"

        /**
         * 创建语言选择底部弹窗实例
         *
         * @param currentLanguage 当前选中的语言
         * @param selectionType 选择类型（源语言/目标语言）
         * @param onLanguageSelected 语言选择回调
         */
        fun newInstance(
            currentLanguage: Language? = null,
            selectionType: SelectionType = SelectionType.SOURCE,
            onLanguageSelected: (Language) -> Unit
        ): LanguageSelectionBottomSheet {
            return LanguageSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    // TODO: 实现Language的Parcelable或使用其他方式传递参数
                    // currentLanguage?.let { putParcelable(ARG_CURRENT_LANGUAGE, it) }
                    putSerializable(ARG_SELECTION_TYPE, selectionType)
                }
                this.onLanguageSelected = onLanguageSelected
                this.currentSelectedLanguage = currentLanguage
            }
        }
    }

    /**
     * 语言选择类型
     */
    enum class SelectionType {
        SOURCE,  // 源语言选择
        TARGET   // 目标语言选择
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLanguageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取参数 - Language不是Parcelable，暂时注释
        // currentSelectedLanguage = arguments?.getParcelable(ARG_CURRENT_LANGUAGE)
        val selectionType = arguments?.getSerializable(ARG_SELECTION_TYPE) as? SelectionType
            ?: SelectionType.SOURCE

        setupUI(selectionType)
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
        loadLanguages(selectionType)
    }

    /**
     * 设置UI
     *
     * 🎯 国际化改进：
     * - 使用LanguageLocalizer获取本地化文本
     * - 根据用户系统语言自动显示对应文本
     * - 支持动态语言切换
     */
    private fun setupUI(selectionType: SelectionType) {
        binding.apply {
            // 设置本地化标题
            tvTitle.text = when (selectionType) {
                SelectionType.SOURCE -> LanguageLocalizer.LanguageSelection.getSourceLanguageTitle(requireContext())
                SelectionType.TARGET -> LanguageLocalizer.LanguageSelection.getTargetLanguageTitle(requireContext())
            }

            // 设置搜索提示文本
            searchView.queryHint = LanguageLocalizer.LanguageSelection.getSearchHint(requireContext())

            // 关闭按钮
            btnClose.setOnClickListener { dismiss() }
            btnClose.contentDescription = LanguageLocalizer.LanguageSelection.getCloseText(requireContext())
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(
            currentSelectedLanguage = currentSelectedLanguage,
            onLanguageClick = { language ->
                onLanguageSelected?.invoke(language)
                dismiss()
            }
        )

        binding.recyclerViewLanguages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = languageAdapter
        }
    }

    /**
     * 设置搜索功能
     */
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 在UI层处理本地化搜索
                searchLanguagesWithLocalization(newText.orEmpty())
                return true
            }
        })
    }

    /**
     * 观察ViewModel状态变化
     *
     * 🎯 响应式UI的核心：
     * - 观察语言列表的加载状态
     * - 自动更新UI显示
     * - 处理错误状态
     */
    private fun observeViewModel() {
        viewModel.languagesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LanguageSelectionViewModel.LanguagesUiState.Loading -> {
                    // 显示加载状态
                    showLoadingState()
                }
                is LanguageSelectionViewModel.LanguagesUiState.Success -> {
                    // 显示语言列表
                    hideLoadingState()
                    languageAdapter.updateLanguages(state.languages)
                }
                is LanguageSelectionViewModel.LanguagesUiState.Error -> {
                    // 显示错误状态
                    hideLoadingState()
                    showErrorState(state.message)
                }
            }
        }
    }

    /**
     * 加载语言列表
     *
     * 🎯 从API动态获取：
     * - 根据选择类型决定是否包含自动检测
     * - 使用ViewModel管理加载状态
     * - 支持错误处理和重试
     */
    private fun loadLanguages(selectionType: SelectionType) {
        val includeAutoDetect = selectionType == SelectionType.SOURCE
        viewModel.loadLanguages(includeAutoDetect)
    }

    /**
     * 显示加载状态
     */
    private fun showLoadingState() {
        // TODO: 添加加载指示器
        // 可以在布局中添加ProgressBar
    }

    /**
     * 隐藏加载状态
     */
    private fun hideLoadingState() {
        // TODO: 隐藏加载指示器
    }

    /**
     * 显示错误状态
     */
    private fun showErrorState(message: String) {
        // TODO: 显示错误信息
        // 可以使用Snackbar或Toast显示错误
    }

    /**
     * 支持本地化的语言搜索
     *
     * 🎯 本地化搜索改进：
     * - 在UI层处理本地化，避免ViewModel依赖Context
     * - 支持搜索本地化后的语言名称
     * - 支持搜索英文名称、原生名称、语言代码
     * - 例如：中文用户可以搜索"法语"找到French
     *
     * 🔧 实现原理：
     * 1. 获取当前所有语言列表
     * 2. 对每个语言获取本地化名称
     * 3. 在多个维度进行匹配搜索
     * 4. 直接更新适配器显示结果
     */
    private fun searchLanguagesWithLocalization(query: String) {
        val currentLanguages = viewModel.getCurrentLanguages()

        val filteredLanguages = if (query.isEmpty()) {
            currentLanguages
        } else {
            currentLanguages.filter { language ->
                // 获取本地化的语言名称
                val localizedName = LanguageLocalizer.getLocalizedLanguageName(requireContext(), language)

                // 支持多种搜索方式
                language.name.contains(query, ignoreCase = true) ||           // 英文名称
                language.displayName.contains(query, ignoreCase = true) ||    // 原生名称
                language.code.contains(query, ignoreCase = true) ||           // 语言代码
                localizedName.contains(query, ignoreCase = true)              // 本地化名称
            }
        }

        // 直接更新适配器，不通过ViewModel
        languageAdapter.updateLanguages(filteredLanguages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
