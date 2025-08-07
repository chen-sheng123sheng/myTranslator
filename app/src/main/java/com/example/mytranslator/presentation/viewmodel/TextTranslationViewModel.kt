package com.example.mytranslator.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytranslator.domain.model.Language
import com.example.mytranslator.domain.model.TranslationInput
import com.example.mytranslator.domain.model.TranslationResult
import com.example.mytranslator.domain.usecase.GetLanguagesUseCase
import com.example.mytranslator.domain.usecase.TranslateUseCase
import kotlinx.coroutines.launch

/**
 * 文本翻译ViewModel
 *
 * 🎯 设计思想：
 * 1. MVVM架构核心 - 连接View和Model的桥梁
 * 2. 状态管理中心 - 管理UI的所有状态变化
 * 3. 业务逻辑协调 - 调用UseCase执行业务操作
 * 4. 生命周期感知 - 自动处理配置变化和内存管理
 *
 * 🔧 技术特性：
 * - LiveData响应式编程，UI自动更新
 * - ViewModelScope协程管理，自动取消
 * - 状态封装，UI只需观察状态变化
 * - 错误处理统一，提供用户友好的反馈
 *
 * 📱 使用场景：
 * - TextTranslationFragment的数据提供者
 * - 翻译操作的状态管理
 * - 语言选择的数据源
 * - 用户交互的响应处理
 *
 * 🎓 学习要点：
 * ViewModel的核心价值：
 * 1. 生命周期感知 - 配置变化时数据不丢失
 * 2. UI逻辑分离 - View只负责显示，ViewModel负责逻辑
 * 3. 可测试性 - 独立于Android框架，便于单元测试
 * 4. 状态管理 - 统一管理UI状态，避免状态不一致
 */
class TextTranslationViewModel(
    private val translateUseCase: TranslateUseCase,
    private val getLanguagesUseCase: GetLanguagesUseCase
) : ViewModel() {



    // ==================== UI状态管理 ====================

    /**
     * 翻译UI状态
     */
    sealed class TranslationUiState {
        /** 空闲状态 */
        object Idle : TranslationUiState()
        
        /** 加载中状态 */
        object Loading : TranslationUiState()
        
        /** 成功状态 */
        data class Success(val result: TranslationResult) : TranslationUiState()
        
        /** 错误状态 */
        data class Error(val message: String) : TranslationUiState()
    }

    /**
     * 语言选择UI状态
     */
    sealed class LanguageUiState {
        /** 加载中 */
        object Loading : LanguageUiState()
        
        /** 成功加载 */
        data class Success(val languages: List<Language>) : LanguageUiState()
        
        /** 加载失败 */
        data class Error(val message: String) : LanguageUiState()
    }

    // ==================== LiveData状态 ====================

    private val _translationState = MutableLiveData<TranslationUiState>(TranslationUiState.Idle)
    val translationState: LiveData<TranslationUiState> = _translationState

    private val _languageState = MutableLiveData<LanguageUiState>()
    val languageState: LiveData<LanguageUiState> = _languageState

    private val _sourceLanguage = MutableLiveData<Language>()
    val sourceLanguage: LiveData<Language> = _sourceLanguage

    private val _targetLanguage = MutableLiveData<Language>()
    val targetLanguage: LiveData<Language> = _targetLanguage

    private val _inputText = MutableLiveData<String>("")
    val inputText: LiveData<String> = _inputText

    private val _characterCount = MutableLiveData<Int>(0)
    val characterCount: LiveData<Int> = _characterCount

    private val _isSwapEnabled = MutableLiveData<Boolean>(false)
    val isSwapEnabled: LiveData<Boolean> = _isSwapEnabled

    // ==================== 私有状态 ====================

    private var supportedLanguages: List<Language> = emptyList()
    private var currentTranslationResult: TranslationResult? = null

    // ==================== 初始化 ====================

    init {
        loadDefaultLanguages()
        loadSupportedLanguages()
    }

    // ==================== 公共方法 ====================

    /**
     * 更新输入文本
     *
     * 🎯 设计考虑：
     * - 实时更新字符计数
     * - 验证文本长度限制
     * - 更新交换按钮状态
     */
    fun updateInputText(text: String) {
        _inputText.value = text
        _characterCount.value = text.length
        updateSwapButtonState()
        
        // 如果文本为空，重置翻译状态
        if (text.isBlank() && _translationState.value is TranslationUiState.Success) {
            _translationState.value = TranslationUiState.Idle
        }
    }

    /**
     * 执行翻译
     *
     * 🎯 设计考虑：
     * - 参数验证和错误处理
     * - 加载状态管理
     * - 协程异步执行
     * - 结果状态更新
     */
    fun translate() {
        val text = _inputText.value?.trim()
        if (text.isNullOrBlank()) {
            _translationState.value = TranslationUiState.Error("请输入要翻译的文本")
            return
        }

        val source = _sourceLanguage.value
        val target = _targetLanguage.value
        
        if (source == null || target == null) {
            _translationState.value = TranslationUiState.Error("请选择翻译语言")
            return
        }

        if (source == target && !source.isAutoDetect()) {
            _translationState.value = TranslationUiState.Error("源语言和目标语言不能相同")
            return
        }

        // 开始翻译
        Log.d(TAG, "🎯 ViewModel开始翻译")
        Log.d(TAG, "  文本: $text")
        Log.d(TAG, "  ${source.code}(${source.name}) -> ${target.code}(${target.name})")

        _translationState.value = TranslationUiState.Loading

        viewModelScope.launch {
            try {
                val input = TranslationInput.Text(text)
                val params = TranslateUseCase.Params(
                    input = input,
                    sourceLanguage = source,
                    targetLanguage = target,
                    enableCache = true,
                    saveToHistory = true,
                    updateStatistics = true
                )

                Log.d(TAG, "📞 调用TranslateUseCase...")
                translateUseCase.execute(params)
                    .onSuccess { result ->
                        Log.d(TAG, "✅ 翻译成功: ${result.translatedText}")
                        currentTranslationResult = result
                        _translationState.value = TranslationUiState.Success(result)
                        updateSwapButtonState()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "❌ 翻译失败: ${exception.message}", exception)
                        _translationState.value = TranslationUiState.Error(
                            "翻译失败：${exception.message ?: "未知错误"}"
                        )
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 翻译过程异常", e)
                _translationState.value = TranslationUiState.Error("翻译失败: ${e.message}")
            }
        }
    }

    /**
     * 选择源语言
     */
    fun selectSourceLanguage(language: Language) {
        _sourceLanguage.value = language
        updateSwapButtonState()
        
        // 如果已有翻译结果且语言改变，清除结果
        if (_translationState.value is TranslationUiState.Success) {
            _translationState.value = TranslationUiState.Idle
        }
    }

    /**
     * 选择目标语言
     */
    fun selectTargetLanguage(language: Language) {
        _targetLanguage.value = language
        updateSwapButtonState()
        
        // 如果已有翻译结果且语言改变，清除结果
        if (_translationState.value is TranslationUiState.Success) {
            _translationState.value = TranslationUiState.Idle
        }
    }

    /**
     * 交换源语言和目标语言
     *
     * 🎯 设计考虑：
     * - 检查交换的有效性
     * - 同时交换输入文本和翻译结果
     * - 更新UI状态
     */
    fun swapLanguages() {
        val source = _sourceLanguage.value
        val target = _targetLanguage.value
        
        if (source == null || target == null) return
        if (source.isAutoDetect()) return // 自动检测不能作为目标语言
        
        // 交换语言
        _sourceLanguage.value = target
        _targetLanguage.value = source
        
        // 如果有翻译结果，交换输入文本和翻译结果
        currentTranslationResult?.let { result ->
            _inputText.value = result.translatedText
            _characterCount.value = result.translatedText.length
            
            // 创建新的翻译结果（交换后的）
            val swappedResult = result.copy(
                input = TranslationInput.Text(result.translatedText),
                translatedText = result.getOriginalText(),
                sourceLanguage = target,
                targetLanguage = source,
                timestamp = System.currentTimeMillis()
            )
            
            currentTranslationResult = swappedResult
            _translationState.value = TranslationUiState.Success(swappedResult)
        }
        
        updateSwapButtonState()
    }

    /**
     * 复制翻译结果
     */
    fun copyResult(): String? {
        return when (val state = _translationState.value) {
            is TranslationUiState.Success -> state.result.translatedText
            else -> null
        }
    }

    /**
     * 分享翻译结果
     */
    fun shareResult(): String? {
        return when (val state = _translationState.value) {
            is TranslationUiState.Success -> state.result.toShareText()
            else -> null
        }
    }

    /**
     * 清除翻译结果
     */
    fun clearTranslation() {
        _inputText.value = ""
        _characterCount.value = 0
        _translationState.value = TranslationUiState.Idle
        currentTranslationResult = null
        updateSwapButtonState()
    }

    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): List<Language> {
        return supportedLanguages
    }

    /**
     * 搜索语言
     */
    fun searchLanguages(query: String): List<Language> {
        if (query.isBlank()) return supportedLanguages
        
        return supportedLanguages.filter { language ->
            language.name.contains(query, ignoreCase = true) ||
            language.displayName.contains(query, ignoreCase = true) ||
            language.code.contains(query, ignoreCase = true)
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 加载默认语言设置
     */
    private fun loadDefaultLanguages() {
        viewModelScope.launch {
            try {
                val settings = getLanguagesUseCase.getDefaultLanguageSettings().getOrThrow()
                _sourceLanguage.value = settings.sourceLanguage
                _targetLanguage.value = settings.targetLanguage
                updateSwapButtonState()
            } catch (e: Exception) {
                // 使用预定义的默认语言
                _sourceLanguage.value = Language.AUTO_DETECT
                _targetLanguage.value = Language.ENGLISH
                updateSwapButtonState()
            }
        }
    }

    /**
     * 加载支持的语言列表
     */
    private fun loadSupportedLanguages() {
        _languageState.value = LanguageUiState.Loading
        
        viewModelScope.launch {
            try {
                val languages = getLanguagesUseCase.getAllLanguages(
                    sortStrategy = GetLanguagesUseCase.SortStrategy.SMART,
                    includeAutoDetect = true
                ).getOrThrow()
                
                supportedLanguages = languages
                _languageState.value = LanguageUiState.Success(languages)
                
            } catch (e: Exception) {
                // 使用预定义的语言列表作为回退
                supportedLanguages = Language.getSupportedLanguages()
                _languageState.value = LanguageUiState.Error("加载语言列表失败，使用默认列表")
            }
        }
    }

    /**
     * 更新交换按钮状态
     *
     * 🎯 设计考虑：
     * - 源语言不能是自动检测
     * - 源语言和目标语言不能相同
     * - 必须有有效的语言选择
     */
    private fun updateSwapButtonState() {
        val source = _sourceLanguage.value
        val target = _targetLanguage.value
        
        val canSwap = source != null && 
                     target != null && 
                     !source.isAutoDetect() && 
                     source != target
        
        _isSwapEnabled.value = canSwap
    }

    /**
     * 🎓 学习要点：ViewModel的生命周期管理
     */
    override fun onCleared() {
        super.onCleared()
        // ViewModelScope会自动取消所有协程
        // 这里可以进行额外的清理工作
    }

    /**
     * 🎓 学习要点：伴生对象的工厂方法
     */
    companion object {
        private const val TAG = "TextTranslationVM"

        /** 最大输入字符数 */
        const val MAX_INPUT_LENGTH = 5000

        /** 字符计数警告阈值 */
        const val CHARACTER_WARNING_THRESHOLD = 4500

        /**
         * 检查字符数是否接近限制
         */
        fun isNearCharacterLimit(count: Int): Boolean {
            return count >= CHARACTER_WARNING_THRESHOLD
        }

        /**
         * 检查字符数是否超过限制
         */
        fun isOverCharacterLimit(count: Int): Boolean {
            return count > MAX_INPUT_LENGTH
        }
    }
}
