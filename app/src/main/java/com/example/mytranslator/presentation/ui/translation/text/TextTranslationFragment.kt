package com.example.mytranslator.presentation.ui.translation.text

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mytranslator.common.base.BaseFragment
import com.example.mytranslator.common.utils.LanguageLocalizer
import com.example.mytranslator.databinding.FragmentTextTranslationBinding
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.presentation.ui.language.LanguageSelectionBottomSheet
import com.example.mytranslator.presentation.viewmodel.TextTranslationViewModel
import com.example.mytranslator.presentation.viewmodel.TextTranslationViewModel.TranslationUiState

/**
 * 文本翻译Fragment
 *
 * 设计思想：
 * 1. 继承BaseFragment：复用基础功能，专注业务逻辑
 * 2. ViewBinding：类型安全的视图访问
 * 3. MVVM架构：通过ViewModel管理业务逻辑和状态
 * 4. 模板方法模式：按照BaseFragment定义的流程初始化
 *
 * 功能特性：
 * - 文本输入和翻译
 * - 语言选择和切换
 * - 翻译历史记录
 * - 复制和分享功能
 * - 实时状态管理和错误处理
 *
 * 架构集成：
 * - 使用BaseFragment的createMyViewModel统一创建ViewModel
 * - 通过LiveData观察状态变化，实现响应式UI
 * - 集成Clean Architecture的UseCase层
 */
class TextTranslationFragment : BaseFragment<FragmentTextTranslationBinding>() {

    // ==================== ViewModel管理 ====================
    /**
     * 文本翻译ViewModel
     *
     * 🎯 使用BaseFragment的createMyViewModel方法创建
     *
     * 【为什么在initData中创建？】
     * 1. 时机合适：此时Fragment的View已创建，可以安全地进行数据初始化
     * 2. 生命周期：确保ViewModel与Fragment正确绑定
     * 3. 架构清晰：数据层的初始化放在initData中，符合职责分离原则
     * 4. 观察时机：创建后立即可以开始观察LiveData
     */
    private lateinit var viewModel: TextTranslationViewModel

    /**
     * 🎯 BaseFragment模板方法：获取ViewBinding实例
     *
     * 这是BaseFragment要求子类实现的抽象方法
     * 用于创建Fragment的ViewBinding实例
     */
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTextTranslationBinding {
        return FragmentTextTranslationBinding.inflate(inflater, container, false)
    }

    /**
     * 🎯 BaseFragment模板方法：初始化视图
     *
     * 在这里设置UI组件的初始状态
     * 例如：RecyclerView的Adapter、Toolbar配置等
     */
    override fun initView() {
        super.initView()

        // 设置输入框的初始状态
        setupInputArea()

        // 设置语言选择区域
        setupLanguageSelection()

        // 设置翻译结果区域
        setupResultArea()
    }

    /**
     * 🎯 BaseFragment模板方法：初始化数据
     *
     * 在这里加载初始数据
     * 例如：网络请求、数据库查询、SharedPreferences读取等
     *
     * 【ViewModel创建的最佳时机】
     * 在initData中创建ViewModel是最佳实践：
     * 1. View已创建：此时Fragment的View已经创建完成
     * 2. 生命周期安全：确保ViewModel与Fragment正确绑定
     * 3. 数据初始化：符合"数据初始化"的语义
     * 4. 观察准备：创建后可以立即开始观察LiveData
     */
    override fun initData() {
        super.initData()

        // 🏭 创建ViewModel - 使用BaseFragment的统一方法
        viewModel = createMyViewModel(TextTranslationViewModel::class.java)

        // 🔍 开始观察ViewModel的状态变化
        observeViewModel()

        // 加载用户偏好的语言设置
        loadLanguagePreferences()

        // 加载翻译历史（如果需要）
        loadTranslationHistory()
    }

    /**
     * 🎯 BaseFragment模板方法：初始化监听器
     *
     * 在这里设置各种监听器
     * 例如：点击事件、文本变化监听、LiveData观察等
     */
    override fun initListener() {
        super.initListener()

        // 设置翻译按钮点击事件
        setupTranslateButton()

        // 设置语言切换点击事件
        setupLanguageSwitchListeners()

        // 设置输入框文本变化监听
        setupInputTextListener()

        // 设置结果区域的操作按钮
        setupResultActions()

        // 设置输入区域的操作按钮
        setupInputActions()
    }

    // ==================== ViewModel观察 ====================

    /**
     * 观察ViewModel的状态变化
     *
     * 🎯 响应式UI的核心方法
     *
     * 【为什么要观察ViewModel？】
     * 1. 响应式编程：UI自动响应数据变化，无需手动更新
     * 2. 状态管理：统一管理加载、成功、错误等状态
     * 3. 生命周期安全：LiveData自动处理生命周期，避免内存泄漏
     * 4. 数据一致性：确保UI始终显示最新的数据状态
     */
    private fun observeViewModel() {
        // 观察翻译状态变化
        viewModel.translationState.observe(viewLifecycleOwner) { state ->
            handleTranslationState(state)
        }

        // 观察输入文本变化
        viewModel.inputText.observe(viewLifecycleOwner) { text ->
            if (binding.etInputText.text.toString() != text) {
                binding.etInputText.setText(text)
            }
        }

        // 观察字符计数变化
        viewModel.characterCount.observe(viewLifecycleOwner) { count ->
            updateCharCount(count)
        }

        // 观察源语言变化
        viewModel.sourceLanguage.observe(viewLifecycleOwner) { language ->
            updateSourceLanguageDisplay(language)
        }

        // 观察目标语言变化
        viewModel.targetLanguage.observe(viewLifecycleOwner) { language ->
            updateTargetLanguageDisplay(language)
        }

        // 观察语言交换按钮状态
        viewModel.isSwapEnabled.observe(viewLifecycleOwner) { canSwap ->
            binding.btnSwapLanguages.isEnabled = canSwap
            binding.btnSwapLanguages.alpha = if (canSwap) 1.0f else 0.5f
        }
    }

    /**
     * 处理翻译状态变化
     *
     * 🎯 统一的状态处理方法
     *
     * 【状态驱动UI的好处】
     * 1. 一致性：所有状态变化都通过这个方法处理，确保UI一致
     * 2. 可维护性：状态处理逻辑集中，易于维护和调试
     * 3. 扩展性：新增状态时只需在这里添加处理逻辑
     * 4. 错误预防：避免UI状态不一致的问题
     */
    private fun handleTranslationState(state: TranslationUiState) {
        when (state) {
            is TranslationUiState.Idle -> {
                hideResultArea()
            }

            is TranslationUiState.Loading -> {
                showLoadingState()
            }

            is TranslationUiState.Success -> {
                showTranslationResult(state.result.translatedText)
            }

            is TranslationUiState.Error -> {
                showErrorState(state.message)
            }
        }
    }

    // ==================== 私有方法：具体实现 ====================

    /**
     * 设置输入区域
     */
    private fun setupInputArea() {
        // 设置输入框的最大字符数提示
        updateCharCount(0)
    }

    /**
     * 设置语言选择区域
     */
    private fun setupLanguageSelection() {
        // 设置默认语言显示
        binding.tvSourceLanguage.text = "自动检测"
        binding.tvTargetLanguage.text = "英文"
    }

    /**
     * 设置翻译结果区域
     */
    private fun setupResultArea() {
        // 初始状态隐藏结果区域
        binding.cardResult.visibility = android.view.View.GONE
    }

    /**
     * 加载语言偏好设置
     */
    private fun loadLanguagePreferences() {
        // TODO: 从SharedPreferences或数据库加载用户的语言偏好
        // 示例：
        // val prefs = requireContext().getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        // val sourceLanguage = prefs.getString("source_language", "auto")
        // val targetLanguage = prefs.getString("target_language", "en")
    }

    /**
     * 加载翻译历史
     */
    private fun loadTranslationHistory() {
        // TODO: 加载翻译历史记录
        // 可以从数据库或本地文件加载
    }

    /**
     * 设置翻译按钮
     *
     * 🎯 通过ViewModel执行翻译
     *
     * 【为什么通过ViewModel执行翻译？】
     * 1. 业务逻辑分离：翻译逻辑在ViewModel中，Fragment只负责UI
     * 2. 状态管理：ViewModel统一管理翻译状态
     * 3. 生命周期安全：ViewModel处理异步操作，避免Fragment销毁时的问题
     * 4. 可测试性：业务逻辑在ViewModel中，更容易进行单元测试
     */
    private fun setupTranslateButton() {
        binding.btnTranslate.setOnClickListener {
            // 直接调用ViewModel的翻译方法
            // ViewModel会自动验证输入并处理各种状态
            viewModel.translate()
        }
    }

    /**
     * 设置语言切换监听器
     */
    private fun setupLanguageSwitchListeners() {
        // 源语言选择
        binding.layoutSourceLanguage.setOnClickListener {
            showSourceLanguageSelection()
        }

        // 目标语言选择
        binding.layoutTargetLanguage.setOnClickListener {
            showTargetLanguageSelection()
        }

        // 语言交换按钮 - 通过ViewModel执行
        binding.btnSwapLanguages.setOnClickListener {
            viewModel.swapLanguages()
        }
    }

    /**
     * 设置输入框文本监听
     *
     * 🎯 与ViewModel双向绑定
     *
     * 【双向数据绑定的实现】
     * 1. UI -> ViewModel：用户输入时更新ViewModel
     * 2. ViewModel -> UI：ViewModel变化时更新UI（在observeViewModel中处理）
     * 3. 防止循环更新：通过文本比较避免无限循环
     */
    private fun setupInputTextListener() {
        binding.etInputText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                // 更新ViewModel中的输入文本（会自动更新字符计数）
                viewModel.updateInputText(text)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * 设置输入区域操作按钮
     *
     * 🎯 提供便捷的输入操作
     */
    private fun setupInputActions() {
        // 清除输入按钮
        binding.btnClearInput.setOnClickListener {
            viewModel.updateInputText("")
            showToast("已清除输入")
        }
    }

    /**
     * 设置结果区域操作按钮
     *
     * 🎯 集成ViewModel的结果操作
     */
    private fun setupResultActions() {
        // 复制按钮 - 通过ViewModel获取结果
        binding.btnCopyResult.setOnClickListener {
            val result = viewModel.copyResult()
            if (result != null) {
                copyToClipboard(result)
                showToast("已复制到剪贴板")
            } else {
                showToast("没有可复制的内容")
            }
        }

        // 分享按钮 - 通过ViewModel获取结果
        binding.btnShareResult.setOnClickListener {
            val result = viewModel.copyResult()
            if (result != null) {
                shareText(result)
            } else {
                showToast("没有可分享的内容")
            }
        }
    }

    // ==================== UI状态管理方法 ====================

    /**
     * 隐藏结果区域
     */
    private fun hideResultArea() {
        binding.cardResult.visibility = android.view.View.GONE
    }

    /**
     * 显示加载状态
     */
    private fun showLoadingState() {
        binding.cardResult.visibility = android.view.View.VISIBLE
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tvResult.visibility = android.view.View.GONE
        binding.layoutError.visibility = android.view.View.GONE
    }

    /**
     * 显示翻译结果
     */
    private fun showTranslationResult(result: String) {
        binding.cardResult.visibility = android.view.View.VISIBLE
        binding.progressBar.visibility = android.view.View.GONE
        binding.tvResult.visibility = android.view.View.VISIBLE
        binding.tvResult.text = result
        binding.layoutError.visibility = android.view.View.GONE
    }

    /**
     * 显示错误状态
     */
    private fun showErrorState(error: String) {
        binding.cardResult.visibility = android.view.View.VISIBLE
        binding.progressBar.visibility = android.view.View.GONE
        binding.tvResult.visibility = android.view.View.GONE
        binding.layoutError.visibility = android.view.View.VISIBLE
        binding.tvError.text = error
    }

    /**
     * 更新字符计数
     */
    @SuppressLint("SetTextI18n")
    private fun updateCharCount(count: Int) {
        binding.tvCharCount.text = "$count/5000"
    }

    /**
     * 更新源语言显示
     *
     * 🎯 国际化改进：
     * - 使用LanguageLocalizer获取本地化的语言名称
     * - 根据用户系统语言自动显示对应文本
     * - 为自动检测提供特殊的本地化处理
     */
    private fun updateSourceLanguageDisplay(language: Language?) {
        binding.tvSourceLanguage.text = when {
            language == null -> LanguageLocalizer.getLocalizedLanguageName(requireContext(), Language.AUTO_DETECT)
            language.isAutoDetect() -> LanguageLocalizer.getLocalizedLanguageName(requireContext(), language)
            else -> LanguageLocalizer.getLocalizedLanguageName(requireContext(), language)
        }
    }

    /**
     * 更新目标语言显示
     *
     * 🎯 国际化改进：
     * - 使用LanguageLocalizer获取本地化的语言名称
     * - 提供本地化的默认值
     * - 保持与源语言显示的一致性
     */
    private fun updateTargetLanguageDisplay(language: Language?) {
        binding.tvTargetLanguage.text = if (language != null) {
            LanguageLocalizer.getLocalizedLanguageName(requireContext(), language)
        } else {
            LanguageLocalizer.getLocalizedLanguageName(requireContext(), Language.ENGLISH)
        }
    }

    /**
     * 显示源语言选择底部弹窗
     *
     * 🎯 设计考虑：
     * - 使用 BottomSheetDialogFragment 提供现代化的用户体验
     * - 传递当前选中的语言，便于用户识别
     * - 通过 ViewModel 更新语言选择，保持状态一致性
     */
    private fun showSourceLanguageSelection() {
        val currentLanguage = viewModel.sourceLanguage.value

        val bottomSheet = LanguageSelectionBottomSheet.newInstance(
            currentLanguage = currentLanguage,
            selectionType = LanguageSelectionBottomSheet.SelectionType.SOURCE,
            onLanguageSelected = { selectedLanguage ->
                // 通过 ViewModel 更新源语言
                viewModel.selectSourceLanguage(selectedLanguage)

                // 显示本地化的用户反馈
                val localizedName = LanguageLocalizer.getLocalizedLanguageName(requireContext(), selectedLanguage)
                val message = LanguageLocalizer.LanguageSelection.getSourceLanguageSelectedMessage(requireContext(), localizedName)
                showToast(message)
            }
        )

        bottomSheet.show(parentFragmentManager, "SourceLanguageSelection")
    }

    /**
     * 显示目标语言选择底部弹窗
     *
     * 🎯 设计考虑：
     * - 与源语言选择保持一致的交互体验
     * - 自动过滤掉"自动检测"选项（目标语言不能是自动检测）
     * - 提供即时的用户反馈
     */
    private fun showTargetLanguageSelection() {
        val currentLanguage = viewModel.targetLanguage.value

        val bottomSheet = LanguageSelectionBottomSheet.newInstance(
            currentLanguage = currentLanguage,
            selectionType = LanguageSelectionBottomSheet.SelectionType.TARGET,
            onLanguageSelected = { selectedLanguage ->
                // 通过 ViewModel 更新目标语言
                viewModel.selectTargetLanguage(selectedLanguage)

                // 显示本地化的用户反馈
                val localizedName = LanguageLocalizer.getLocalizedLanguageName(requireContext(), selectedLanguage)
                val message = LanguageLocalizer.LanguageSelection.getTargetLanguageSelectedMessage(requireContext(), localizedName)
                showToast(message)
            }
        )

        bottomSheet.show(parentFragmentManager, "TargetLanguageSelection")
    }

    // ==================== 辅助工具方法 ====================

    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("翻译结果", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * 分享文本
     */
    private fun shareText(text: String) {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "分享翻译结果"))
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}