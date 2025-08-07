package com.example.mytranslator.di

import android.content.Context
import com.example.mytranslator.data.config.ApiConfig
import com.example.mytranslator.data.local.database.TranslationDatabase
import com.example.mytranslator.data.mapper.TranslationHistoryMapper
import com.example.mytranslator.data.network.api.TranslationApi
import com.example.mytranslator.data.repository.TranslationHistoryRepositoryImpl
import com.example.mytranslator.data.repository.TranslationRepositoryImpl
import com.example.mytranslator.data.repository.LanguageRepositoryImpl
import com.example.mytranslator.domain.usecase.*
import com.example.mytranslator.domain.service.TranslationHistoryIntegrationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 翻译功能依赖注入容器
 *
 * 🎯 设计目的：
 * 1. 统一管理翻译相关的依赖创建
 * 2. 提供单例模式确保对象复用
 * 3. 简化依赖注入和对象组装
 * 4. 支持测试时的Mock替换
 *
 * 🏗️ 容器设计：
 * - 单例模式：确保全局唯一实例
 * - 懒加载：按需创建依赖对象
 * - 层次化：按架构层次组织依赖
 * - 可配置：支持不同环境的配置
 *
 * 📱 使用场景：
 * - Activity/Fragment中获取ViewModel
 * - 测试时创建Mock依赖
 * - 应用启动时的依赖初始化
 * - 功能模块的依赖管理
 *
 * 🎓 学习要点：
 * 依赖注入的核心概念：
 * 1. 控制反转 - 对象创建由容器控制
 * 2. 依赖注入 - 依赖关系由外部注入
 * 3. 单一职责 - 每个类只负责自己的业务
 * 4. 可测试性 - 便于Mock和单元测试
 */
object TranslationDependencyContainer {

    // ===== 数据库相关 =====
    
    private var database: TranslationDatabase? = null
    
    /**
     * 获取翻译数据库实例
     */
    fun getDatabase(context: Context): TranslationDatabase {
        return database ?: synchronized(this) {
            database ?: TranslationDatabase.getDatabase(context).also { database = it }
        }
    }

    // ===== API层 =====

    private var translationApi: TranslationApi? = null

    /**
     * 获取翻译API实例
     */
    fun getTranslationApi(): TranslationApi {
        return translationApi ?: synchronized(this) {
            translationApi ?: createTranslationApi().also { translationApi = it }
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

    // ===== Repository层 =====

    private var translationRepository: TranslationRepositoryImpl? = null
    private var languageRepository: LanguageRepositoryImpl? = null
    private var translationHistoryRepository: TranslationHistoryRepositoryImpl? = null

    /**
     * 获取翻译Repository
     */
    fun getTranslationRepository(context: Context): TranslationRepositoryImpl {
        return translationRepository ?: synchronized(this) {
            translationRepository ?: run {
                val api = getTranslationApi()
                TranslationRepositoryImpl.create(
                    api = api,
                    appId = ApiConfig.BaiduTranslation.APP_ID,
                    secretKey = ApiConfig.BaiduTranslation.SECRET_KEY
                ).also { translationRepository = it }
            }
        }
    }

    /**
     * 获取语言Repository
     */
    fun getLanguageRepository(context: Context): LanguageRepositoryImpl {
        return languageRepository ?: synchronized(this) {
            languageRepository ?: run {
                val api = getTranslationApi()
                LanguageRepositoryImpl(api, context).also {
                    languageRepository = it
                }
            }
        }
    }
    
    /**
     * 获取翻译历史Repository
     */
    fun getTranslationHistoryRepository(context: Context): TranslationHistoryRepositoryImpl {
        return translationHistoryRepository ?: synchronized(this) {
            translationHistoryRepository ?: run {
                val db = getDatabase(context)
                val dao = db.translationHistoryDao()
                val mapper = TranslationHistoryMapper
                TranslationHistoryRepositoryImpl(dao, mapper).also { 
                    translationHistoryRepository = it 
                }
            }
        }
    }

    // ===== Use Case层 =====
    
    private var translateUseCase: TranslateUseCase? = null
    private var getLanguagesUseCase: GetLanguagesUseCase? = null
    private var saveTranslationUseCase: SaveTranslationUseCase? = null
    
    /**
     * 获取翻译Use Case（带历史记录集成）
     */
    fun getTranslateUseCase(context: Context): TranslateUseCase {
        return translateUseCase ?: synchronized(this) {
            translateUseCase ?: run {
                val translationRepo = getTranslationRepository(context)
                val languageRepo = getLanguageRepository(context)
                val historyIntegrationService = getTranslationHistoryIntegrationService(context)
                
                TranslateUseCase(
                    translationRepository = translationRepo,
                    languageRepository = languageRepo,
                    historyIntegrationService = historyIntegrationService
                ).also { translateUseCase = it }
            }
        }
    }
    
    /**
     * 获取语言列表Use Case
     */
    fun getGetLanguagesUseCase(context: Context): GetLanguagesUseCase {
        return getLanguagesUseCase ?: synchronized(this) {
            getLanguagesUseCase ?: run {
                val languageRepo = getLanguageRepository(context)
                GetLanguagesUseCase(languageRepo).also { getLanguagesUseCase = it }
            }
        }
    }
    
    /**
     * 获取保存翻译Use Case
     */
    fun getSaveTranslationUseCase(context: Context): SaveTranslationUseCase {
        return saveTranslationUseCase ?: synchronized(this) {
            saveTranslationUseCase ?: run {
                val historyRepo = getTranslationHistoryRepository(context)
                SaveTranslationUseCase(historyRepo).also { saveTranslationUseCase = it }
            }
        }
    }

    // ===== 服务层 =====
    
    private var translationHistoryIntegrationService: TranslationHistoryIntegrationService? = null
    
    /**
     * 获取翻译历史集成服务
     */
    fun getTranslationHistoryIntegrationService(context: Context): TranslationHistoryIntegrationService {
        return translationHistoryIntegrationService ?: synchronized(this) {
            translationHistoryIntegrationService ?: run {
                val saveTranslationUseCase = getSaveTranslationUseCase(context)
                TranslationHistoryIntegrationService(saveTranslationUseCase).also { 
                    translationHistoryIntegrationService = it 
                }
            }
        }
    }

    // ===== 历史记录相关Use Cases =====
    
    private var getHistoryUseCase: GetHistoryUseCase? = null
    private var searchHistoryUseCase: SearchHistoryUseCase? = null
    private var manageFavoriteUseCase: ManageFavoriteUseCase? = null
    private var deleteHistoryUseCase: DeleteHistoryUseCase? = null
    
    /**
     * 获取历史记录Use Case
     */
    fun getGetHistoryUseCase(context: Context): GetHistoryUseCase {
        return getHistoryUseCase ?: synchronized(this) {
            getHistoryUseCase ?: run {
                val historyRepo = getTranslationHistoryRepository(context)
                GetHistoryUseCase(historyRepo).also { getHistoryUseCase = it }
            }
        }
    }
    
    /**
     * 获取搜索历史Use Case
     */
    fun getSearchHistoryUseCase(context: Context): SearchHistoryUseCase {
        return searchHistoryUseCase ?: synchronized(this) {
            searchHistoryUseCase ?: run {
                val historyRepo = getTranslationHistoryRepository(context)
                SearchHistoryUseCase(historyRepo).also { searchHistoryUseCase = it }
            }
        }
    }
    
    /**
     * 获取收藏管理Use Case
     */
    fun getManageFavoriteUseCase(context: Context): ManageFavoriteUseCase {
        return manageFavoriteUseCase ?: synchronized(this) {
            manageFavoriteUseCase ?: run {
                val historyRepo = getTranslationHistoryRepository(context)
                ManageFavoriteUseCase(historyRepo).also { manageFavoriteUseCase = it }
            }
        }
    }
    
    /**
     * 获取删除历史Use Case
     */
    fun getDeleteHistoryUseCase(context: Context): DeleteHistoryUseCase {
        return deleteHistoryUseCase ?: synchronized(this) {
            deleteHistoryUseCase ?: run {
                val historyRepo = getTranslationHistoryRepository(context)
                DeleteHistoryUseCase(historyRepo).also { deleteHistoryUseCase = it }
            }
        }
    }

    // ===== 清理方法 =====
    
    /**
     * 清理所有缓存的实例（主要用于测试）
     */
    fun clearAll() {
        database = null
        translationRepository = null
        languageRepository = null
        translationHistoryRepository = null
        translateUseCase = null
        getLanguagesUseCase = null
        saveTranslationUseCase = null
        translationHistoryIntegrationService = null
        getHistoryUseCase = null
        searchHistoryUseCase = null
        manageFavoriteUseCase = null
        deleteHistoryUseCase = null
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return database != null
    }
}
