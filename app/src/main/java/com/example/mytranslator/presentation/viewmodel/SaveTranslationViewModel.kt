package com.example.mytranslator.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytranslator.domain.usecase.SaveTranslationRequest
import com.example.mytranslator.domain.usecase.SaveTranslationUseCase
import com.example.mytranslator.domain.usecase.SaveResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 保存翻译记录ViewModel
 *
 * 🎯 设计目的：
 * 1. 专门处理翻译结果的保存逻辑
 * 2. 在翻译完成后自动保存到历史记录
 * 3. 提供手动保存和批量保存功能
 * 4. 管理保存状态和用户反馈
 *
 * 🏗️ 设计特点：
 * - 自动保存：翻译完成后自动保存
 * - 状态管理：跟踪保存状态和结果
 * - 错误处理：提供详细的错误信息
 * - 批量操作：支持批量保存多个翻译
 *
 * 📱 使用场景：
 * - 翻译界面的自动保存
 * - 用户手动保存翻译结果
 * - 批量导入翻译记录
 * - 离线翻译结果同步
 *
 * 🎓 学习要点：
 * 保存操作的ViewModel设计：
 * 1. 状态跟踪 - 跟踪保存操作的状态
 * 2. 自动化 - 提供自动保存机制
 * 3. 用户反馈 - 及时反馈保存结果
 * 4. 错误恢复 - 处理保存失败的情况
 */
class SaveTranslationViewModel(
    private val saveTranslationUseCase: SaveTranslationUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "SaveTranslationVM"
    }

    // ===== 保存状态管理 =====

    /**
     * 保存UI状态
     */
    private val _uiState = MutableStateFlow(SaveUiState())
    val uiState: StateFlow<SaveUiState> = _uiState.asStateFlow()

    /**
     * 一次性事件流
     */
    private val _events = MutableSharedFlow<SaveEvent>()
    val events: SharedFlow<SaveEvent> = _events.asSharedFlow()

    /**
     * 保存队列
     */
    private val _saveQueue = MutableStateFlow<List<SaveTranslationRequest>>(emptyList())

    init {
        Log.d(TAG, "🚀 SaveTranslationViewModel 初始化")
        
        // 监听保存队列，自动处理保存任务
        viewModelScope.launch {
            _saveQueue
                .filter { it.isNotEmpty() }
                .collect { queue ->
                    processSaveQueue(queue)
                }
        }
    }

    // ===== 保存操作 =====

    /**
     * 保存单个翻译记录
     *
     * @param request 保存请求
     * @param autoSave 是否为自动保存
     */
    fun saveTranslation(request: SaveTranslationRequest, autoSave: Boolean = false) {
        Log.d(TAG, "💾 保存翻译记录 (自动: $autoSave)")
        
        viewModelScope.launch {
            updateUiState { it.copy(isSaving = true, lastSaveAttempt = System.currentTimeMillis()) }
            
            try {
                val result = saveTranslationUseCase(request)
                
                when (result) {
                    is SaveResult.Success -> {
                        Log.i(TAG, "✅ 翻译记录保存成功")
                        
                        updateUiState { 
                            it.copy(
                                isSaving = false,
                                lastSaveSuccess = true,
                                lastSavedTranslationId = result.translationHistory.id,
                                totalSaved = it.totalSaved + 1
                            )
                        }
                        
                        if (!autoSave) {
                            emitEvent(SaveEvent.SaveSuccess("翻译记录已保存"))
                        }
                    }
                    
                    is SaveResult.ValidationError -> {
                        Log.w(TAG, "❌ 保存验证失败: ${result.message}")
                        handleSaveError(result.message, autoSave)
                    }
                    
                    is SaveResult.BusinessRuleError -> {
                        Log.w(TAG, "❌ 业务规则错误: ${result.message}")
                        handleSaveError(result.message, autoSave)
                    }
                    
                    is SaveResult.RepositoryError -> {
                        Log.e(TAG, "❌ Repository错误: ${result.message}")
                        handleSaveError("保存失败: ${result.message}", autoSave)
                    }
                    
                    is SaveResult.UnknownError -> {
                        Log.e(TAG, "❌ 未知错误: ${result.message}")
                        handleSaveError("保存失败: ${result.message}", autoSave)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 保存翻译记录时发生异常", e)
                handleSaveError("保存失败: ${e.message}", autoSave)
            }
        }
    }

    /**
     * 批量保存翻译记录
     *
     * @param requests 保存请求列表
     */
    fun saveTranslationsBatch(requests: List<SaveTranslationRequest>) {
        Log.d(TAG, "💾 批量保存翻译记录: ${requests.size} 条")
        
        if (requests.isEmpty()) {
            emitEvent(SaveEvent.SaveError("没有要保存的翻译记录"))
            return
        }
        
        // 添加到保存队列
        _saveQueue.value = requests
    }

    /**
     * 自动保存翻译结果
     *
     * 🎯 使用场景：
     * 在翻译完成后自动调用此方法保存结果
     */
    fun autoSaveTranslation(
        originalText: String,
        translatedText: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        sourceLanguageName: String,
        targetLanguageName: String,
        translationProvider: String,
        qualityScore: Double? = null
    ) {
        Log.d(TAG, "🤖 自动保存翻译结果")
        
        val request = SaveTranslationRequest(
            originalText = originalText,
            translatedText = translatedText,
            sourceLanguageCode = sourceLanguageCode,
            targetLanguageCode = targetLanguageCode,
            sourceLanguageName = sourceLanguageName,
            targetLanguageName = targetLanguageName,
            translationProvider = translationProvider,
            qualityScore = qualityScore
        )
        
        saveTranslation(request, autoSave = true)
    }

    /**
     * 重试保存
     */
    fun retrySave() {
        Log.d(TAG, "🔄 重试保存")
        
        val lastRequest = _uiState.value.lastFailedRequest
        if (lastRequest != null) {
            saveTranslation(lastRequest)
            updateUiState { it.copy(lastFailedRequest = null) }
        } else {
            emitEvent(SaveEvent.SaveError("没有可重试的保存操作"))
        }
    }

    /**
     * 清除保存状态
     */
    fun clearSaveState() {
        Log.d(TAG, "🧹 清除保存状态")
        updateUiState { SaveUiState() }
    }

    // ===== 私有方法 =====

    /**
     * 处理保存队列
     */
    private suspend fun processSaveQueue(queue: List<SaveTranslationRequest>) {
        Log.d(TAG, "⚙️ 处理保存队列: ${queue.size} 个任务")
        
        updateUiState { 
            it.copy(
                isSaving = true,
                batchSaveProgress = 0,
                batchSaveTotal = queue.size
            )
        }
        
        var successCount = 0
        var failureCount = 0
        
        queue.forEachIndexed { index, request ->
            try {
                val result = saveTranslationUseCase(request)
                
                if (result.isSuccess()) {
                    successCount++
                } else {
                    failureCount++
                    Log.w(TAG, "❌ 批量保存项目失败: ${result.getErrorMessage()}")
                }
                
                // 更新进度
                updateUiState { 
                    it.copy(batchSaveProgress = index + 1)
                }
                
            } catch (e: Exception) {
                failureCount++
                Log.e(TAG, "❌ 批量保存项目异常", e)
            }
        }
        
        // 完成批量保存
        updateUiState { 
            it.copy(
                isSaving = false,
                totalSaved = it.totalSaved + successCount,
                batchSaveProgress = 0,
                batchSaveTotal = 0
            )
        }
        
        // 清空队列
        _saveQueue.value = emptyList()
        
        // 发送完成事件
        val message = "批量保存完成: 成功 $successCount 条，失败 $failureCount 条"
        if (failureCount == 0) {
            emitEvent(SaveEvent.BatchSaveSuccess(message, successCount))
        } else {
            emitEvent(SaveEvent.BatchSavePartialSuccess(message, successCount, failureCount))
        }
    }

    /**
     * 处理保存错误
     */
    private fun handleSaveError(errorMessage: String, autoSave: Boolean) {
        updateUiState { 
            it.copy(
                isSaving = false,
                lastSaveSuccess = false,
                lastErrorMessage = errorMessage
            )
        }
        
        if (!autoSave) {
            emitEvent(SaveEvent.SaveError(errorMessage))
        }
    }

    /**
     * 更新UI状态
     */
    private fun updateUiState(update: (SaveUiState) -> SaveUiState) {
        _uiState.value = update(_uiState.value)
    }

    /**
     * 发送事件
     */
    private fun emitEvent(event: SaveEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

/**
 * 保存UI状态
 */
data class SaveUiState(
    val isSaving: Boolean = false,
    val lastSaveSuccess: Boolean = false,
    val lastSaveAttempt: Long = 0,
    val lastSavedTranslationId: String? = null,
    val lastErrorMessage: String? = null,
    val lastFailedRequest: SaveTranslationRequest? = null,
    val totalSaved: Int = 0,
    
    // 批量保存状态
    val batchSaveProgress: Int = 0,
    val batchSaveTotal: Int = 0
) {
    /**
     * 是否正在进行批量保存
     */
    fun isBatchSaving(): Boolean = batchSaveTotal > 0
    
    /**
     * 获取批量保存进度百分比
     */
    fun getBatchSaveProgress(): Float = if (batchSaveTotal > 0) {
        batchSaveProgress.toFloat() / batchSaveTotal
    } else 0f
    
    /**
     * 是否有保存错误
     */
    fun hasError(): Boolean = !lastSaveSuccess && lastErrorMessage != null
}

/**
 * 保存事件密封类
 */
sealed class SaveEvent {
    /**
     * 保存成功
     */
    data class SaveSuccess(val message: String) : SaveEvent()
    
    /**
     * 保存失败
     */
    data class SaveError(val message: String) : SaveEvent()
    
    /**
     * 批量保存成功
     */
    data class BatchSaveSuccess(val message: String, val count: Int) : SaveEvent()
    
    /**
     * 批量保存部分成功
     */
    data class BatchSavePartialSuccess(
        val message: String,
        val successCount: Int,
        val failureCount: Int
    ) : SaveEvent()
    
    /**
     * 显示保存进度
     */
    data class ShowProgress(val progress: Float, val message: String) : SaveEvent()
}
