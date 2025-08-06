package com.example.mytranslator.presentation.ui.translation.text

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.mytranslator.common.base.BaseFragment
import com.example.mytranslator.databinding.FragmentTextTranslationBinding

/**
 * 文本翻译Fragment
 *
 * 设计思想：
 * 1. 继承BaseFragment：复用基础功能，专注业务逻辑
 * 2. ViewBinding：类型安全的视图访问
 * 3. 模板方法模式：按照BaseFragment定义的流程初始化
 *
 * 功能特性：
 * - 文本输入和翻译
 * - 语言选择和切换
 * - 翻译历史记录
 * - 复制和分享功能
 */
class TextTranslationFragment : BaseFragment<FragmentTextTranslationBinding>() {

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
     */
    override fun initData() {
        super.initData()

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
     */
    private fun setupTranslateButton() {
        binding.btnTranslate.setOnClickListener {
            val inputText = binding.etInputText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                performTranslation(inputText)
            } else {
                // 显示提示信息
                showToast("请输入要翻译的文本")
            }
        }
    }

    /**
     * 设置语言切换监听器
     */
    private fun setupLanguageSwitchListeners() {
        // 源语言选择
        binding.layoutSourceLanguage.setOnClickListener {
            showLanguageSelectionDialog(true)
        }

        // 目标语言选择
        binding.layoutTargetLanguage.setOnClickListener {
            showLanguageSelectionDialog(false)
        }

        // 语言交换按钮
        binding.btnSwapLanguages.setOnClickListener {
            swapLanguages()
        }
    }

    /**
     * 设置输入框文本监听
     */
    private fun setupInputTextListener() {
        binding.etInputText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount(s?.length ?: 0)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * 设置结果区域操作按钮
     */
    private fun setupResultActions() {
        // 复制按钮
        binding.btnCopyResult.setOnClickListener {
            copyResultToClipboard()
        }

        // 分享按钮
        binding.btnShareResult.setOnClickListener {
            shareTranslationResult()
        }
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 执行翻译
     */
    private fun performTranslation(text: String) {
        // 显示加载状态
        showLoadingState()

        // TODO: 调用翻译API
        // 这里应该调用翻译服务
        // 示例：
        // translationViewModel.translate(text, sourceLanguage, targetLanguage)

        // 模拟翻译结果（实际应该通过ViewModel和LiveData）
        simulateTranslation(text)
    }

    /**
     * 模拟翻译过程（仅用于演示）
     */
    private fun simulateTranslation(text: String) {
        // 使用safeExecute确保Fragment状态有效
        safeExecute {
            // 模拟网络延迟
            binding.root.postDelayed({
                safeExecute {
                    // 模拟翻译结果
                    val result = "Translation result for: $text"
                    showTranslationResult(result)
                }
            }, 2000)
        }
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
        binding.progressBar.visibility = android.view.View.GONE
        binding.tvResult.visibility = android.view.View.VISIBLE
        binding.tvResult.text = result
        binding.layoutError.visibility = android.view.View.GONE
    }

    /**
     * 显示错误状态
     */
    private fun showErrorState(error: String) {
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
     * 显示语言选择对话框
     */
    private fun showLanguageSelectionDialog(isSourceLanguage: Boolean) {
        // TODO: 实现语言选择对话框
        showToast(if (isSourceLanguage) "选择源语言" else "选择目标语言")
    }

    /**
     * 交换源语言和目标语言
     */
    private fun swapLanguages() {
        val sourceText = binding.tvSourceLanguage.text.toString()
        val targetText = binding.tvTargetLanguage.text.toString()

        if (sourceText != "自动检测") {
            binding.tvSourceLanguage.text = targetText
            binding.tvTargetLanguage.text = sourceText
            showToast("语言已交换")
        } else {
            showToast("自动检测模式无法交换")
        }
    }

    /**
     * 复制结果到剪贴板
     */
    private fun copyResultToClipboard() {
        val result = binding.tvResult.text.toString()
        if (result.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("翻译结果", result)
            clipboard.setPrimaryClip(clip)
            showToast("已复制到剪贴板")
        }
    }

    /**
     * 分享翻译结果
     */
    private fun shareTranslationResult() {
        val result = binding.tvResult.text.toString()
        if (result.isNotEmpty()) {
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, result)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "分享翻译结果"))
        }
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}