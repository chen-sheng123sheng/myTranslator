package com.example.mytranslator.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mytranslator.data.config.ApiConfig
import com.example.mytranslator.data.network.api.TranslationApi
import com.example.mytranslator.data.repository.LanguageRepositoryImpl
import com.example.mytranslator.data.repository.TranslationRepositoryImpl
import com.example.mytranslator.domain.repository.LanguageRepository
import com.example.mytranslator.domain.repository.TranslationRepository
import com.example.mytranslator.domain.usecase.GetLanguagesUseCase
import com.example.mytranslator.domain.usecase.TranslateUseCase
import com.example.mytranslator.domain.model.Language
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ViewModel工厂类
 *
 * 🎯 设计思想：
 * 1. 依赖注入 - 为ViewModel提供所需的依赖
 * 2. 工厂模式 - 统一创建ViewModel实例
 * 3. 单例管理 - 避免重复创建相同的依赖
 * 4. 配置集中 - 所有依赖配置都在这里
 *
 * 🎓 学习要点：
 * 为什么需要ViewModelFactory？
 * 1. ViewModel构造函数需要参数时必须使用Factory
 * 2. 依赖注入的手动实现方式
 * 3. 在没有Hilt等DI框架时的解决方案
 */
class ViewModelFactory private constructor(
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    // 单例依赖，避免重复创建
    private val translationApi: TranslationApi by lazy {
        println("🔧 创建TranslationApi")
        createTranslationApi()
    }

    private val translationRepository: TranslationRepository by lazy {
        println("🔧 创建TranslationRepository")
        TranslationRepositoryImpl.create(
            api = translationApi,
            appId = ApiConfig.BaiduTranslation.APP_ID,
            secretKey = ApiConfig.BaiduTranslation.SECRET_KEY
        )
    }

    private val languageRepository: LanguageRepository by lazy {
        println("🔧 创建LanguageRepository (真实实现)")
        LanguageRepositoryImpl(translationApi, applicationContext)
    }

    private val translateUseCase: TranslateUseCase by lazy {
        println("🔧 创建TranslateUseCase")
        TranslateUseCase(translationRepository, languageRepository)
    }

    private val getLanguagesUseCase: GetLanguagesUseCase by lazy {
        println("🔧 创建GetLanguagesUseCase")
        GetLanguagesUseCase(languageRepository)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TextTranslationViewModel::class.java) -> {
                try {
                    TextTranslationViewModel(translateUseCase, getLanguagesUseCase) as T
                } catch (e: Exception) {
                    println("❌ 创建TextTranslationViewModel失败: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }
            modelClass.isAssignableFrom(LanguageSelectionViewModel::class.java) -> {
                try {
                    LanguageSelectionViewModel(getLanguagesUseCase) as T
                } catch (e: Exception) {
                    println("❌ 创建LanguageSelectionViewModel失败: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    /**
     * 创建翻译API实例
     */
    private fun createTranslationApi(): TranslationApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(TranslationApi.BAIDU_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(TranslationApi::class.java)
    }

    // Mock实现已移除，现在使用真实的LanguageRepositoryImpl

    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        /**
         * 获取ViewModelFactory单例
         *
         * 🎯 设计考虑：
         * - 使用Application Context避免内存泄漏
         * - 双重检查锁定确保线程安全
         * - 延迟初始化提高性能
         *
         * @param context 应用上下文，会自动转换为Application Context
         */
        fun getInstance(context: Context): ViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViewModelFactory(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * 为了向后兼容，提供无参数版本
         * 但需要先调用有参数版本进行初始化
         */
        fun getInstance(): ViewModelFactory {
            return INSTANCE ?: throw IllegalStateException(
                "ViewModelFactory未初始化，请先调用getInstance(context)"
            )
        }
    }
}
