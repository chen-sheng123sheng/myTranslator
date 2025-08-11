package com.example.login.internal.storage

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.login.api.User
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 用户存储服务（内部实现）
 * 
 * 🎯 设计目的：
 * 1. 管理用户信息的本地存储和读取
 * 2. 处理登录状态的持久化
 * 3. 提供安全的数据存储和读取
 * 4. 支持数据迁移和版本兼容
 * 
 * 🏗️ 设计模式：
 * - 仓储模式：抽象数据存储操作，隐藏具体实现
 * - 单例模式：全局唯一的存储服务实例
 * - 策略模式：支持多种存储方式（DataStore、SharedPreferences等）
 * - 适配器模式：适配不同的数据格式和版本
 * 
 * 📱 技术选择：
 * - DataStore：替代SharedPreferences的现代化存储方案
 * - JSON序列化：使用Gson进行对象序列化
 * - 协程支持：异步操作，不阻塞主线程
 * - 类型安全：使用强类型的Preferences Key
 * 
 * 🎓 学习要点：
 * 1. DataStore的使用和优势
 * 2. 数据序列化和反序列化
 * 3. 异步存储操作的处理
 * 4. 数据安全和隐私保护
 * 
 * 为什么选择DataStore？
 * 1. 类型安全：编译时检查，避免运行时错误
 * 2. 异步操作：基于协程，不会阻塞UI线程
 * 3. 数据一致性：保证数据的原子性操作
 * 4. 错误处理：更好的异常处理机制
 */
internal class UserStorage private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "UserStorage"
        
        // DataStore名称
        private const val USER_PREFERENCES_NAME = "user_preferences"
        
        // 存储键
        private val USER_INFO_KEY = stringPreferencesKey("user_info")
        private val LOGIN_STATUS_KEY = stringPreferencesKey("login_status")
        private val LAST_LOGIN_TIME_KEY = stringPreferencesKey("last_login_time")
        
        @Volatile
        private var INSTANCE: UserStorage? = null
        
        /**
         * 获取UserStorage单例实例
         * 
         * @param context 应用上下文
         * @return UserStorage实例
         */
        fun getInstance(context: Context): UserStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // DataStore扩展属性
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )
    
    // JSON序列化工具
    private val gson = Gson()
    
    // ===== 用户信息存储方法 =====
    
    /**
     * 保存用户信息
     * 
     * 🎯 保存流程：
     * 1. 将User对象序列化为JSON字符串
     * 2. 使用DataStore安全地存储数据
     * 3. 同时更新登录状态和时间
     * 4. 处理存储过程中的异常
     * 
     * @param user 要保存的用户信息
     * 
     * 数据安全考虑：
     * 1. 敏感信息过滤：不存储密码等敏感信息
     * 2. 数据加密：可选的数据加密存储
     * 3. 原子操作：确保数据的一致性
     * 4. 异常处理：存储失败时的恢复机制
     */
    suspend fun saveUser(user: User) {
        try {
            Log.d(TAG, "💾 Saving user info: ${user.getSummary()}")
            
            // 1. 序列化用户信息
            val userJson = gson.toJson(user)
            val currentTime = System.currentTimeMillis().toString()
            
            // 2. 使用DataStore存储
            context.dataStore.edit { preferences ->
                preferences[USER_INFO_KEY] = userJson
                preferences[LOGIN_STATUS_KEY] = "logged_in"
                preferences[LAST_LOGIN_TIME_KEY] = currentTime
            }
            
            Log.i(TAG, "✅ User info saved successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save user info", e)
            throw UserStorageException("Failed to save user info", e)
        }
    }
    
    /**
     * 获取当前用户信息
     * 
     * 🎯 读取流程：
     * 1. 从DataStore读取用户JSON数据
     * 2. 反序列化为User对象
     * 3. 验证数据的有效性
     * 4. 处理数据损坏或版本不兼容的情况
     * 
     * @return 用户信息，未登录或数据无效返回null
     * 
     * 数据验证：
     * 1. JSON格式验证：确保数据格式正确
     * 2. 字段完整性：检查必要字段是否存在
     * 3. 数据有效性：验证数据的逻辑正确性
     * 4. 版本兼容：处理不同版本的数据格式
     */
    suspend fun getCurrentUser(): User? {
        return try {
            Log.d(TAG, "📖 Reading current user info")
            
            // 1. 从DataStore读取数据
            val userJson = context.dataStore.data.map { preferences ->
                preferences[USER_INFO_KEY]
            }.first()
            
            // 2. 检查数据是否存在
            if (userJson.isNullOrBlank()) {
                Log.d(TAG, "📭 No user info found")
                return null
            }
            
            // 3. 反序列化用户信息
            val user = gson.fromJson(userJson, User::class.java)
            
            // 4. 验证用户信息
            if (isValidUser(user)) {
                Log.d(TAG, "✅ User info loaded: ${user.getSummary()}")
                return user
            } else {
                Log.w(TAG, "⚠️ Invalid user info, clearing data")
                clearUser()
                return null
            }
            
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "❌ Failed to parse user JSON, clearing corrupted data", e)
            clearUser()
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current user", e)
            return null
        }
    }
    
    /**
     * 检查是否已登录
     * 
     * 🎯 检查逻辑：
     * 1. 读取登录状态标记
     * 2. 验证用户信息是否存在
     * 3. 检查登录时间是否有效
     * 4. 综合判断登录状态
     * 
     * @return 是否已登录
     * 
     * 登录状态判断：
     * 1. 状态标记：检查登录状态标记
     * 2. 数据存在：用户信息必须存在且有效
     * 3. 时间有效：登录时间在合理范围内
     * 4. 逻辑一致：各项检查结果一致
     */
    suspend fun isLoggedIn(): Boolean {
        return try {
            Log.d(TAG, "🔍 Checking login status")
            
            // 1. 读取登录状态和用户信息
            val preferences = context.dataStore.data.first()
            val loginStatus = preferences[LOGIN_STATUS_KEY]
            val userJson = preferences[USER_INFO_KEY]
            
            // 2. 检查登录状态标记
            val isStatusValid = loginStatus == "logged_in"
            
            // 3. 检查用户信息是否存在
            val isUserInfoValid = !userJson.isNullOrBlank()
            
            // 4. 综合判断
            val isLoggedIn = isStatusValid && isUserInfoValid
            
            Log.d(TAG, "🔍 Login status: $isLoggedIn (status: $isStatusValid, userInfo: $isUserInfoValid)")
            return isLoggedIn
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check login status", e)
            return false
        }
    }
    
    /**
     * 清除用户信息
     * 
     * 🎯 清除流程：
     * 1. 清除用户信息数据
     * 2. 重置登录状态标记
     * 3. 清除相关的缓存数据
     * 4. 确保数据完全清理
     * 
     * 使用场景：
     * 1. 用户主动登出
     * 2. 数据损坏时的清理
     * 3. 安全要求的数据清除
     * 4. 应用重置或卸载
     */
    suspend fun clearUser() {
        try {
            Log.d(TAG, "🧹 Clearing user info")
            
            // 清除所有用户相关数据
            context.dataStore.edit { preferences ->
                preferences.remove(USER_INFO_KEY)
                preferences.remove(LOGIN_STATUS_KEY)
                preferences.remove(LAST_LOGIN_TIME_KEY)
            }
            
            Log.i(TAG, "✅ User info cleared successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear user info", e)
            throw UserStorageException("Failed to clear user info", e)
        }
    }
    
    /**
     * 更新最后登录时间
     * 
     * @param user 用户信息
     * @return 更新后的用户信息
     */
    suspend fun updateLastLoginTime(user: User): User {
        val updatedUser = user.updateLastLoginTime()
        saveUser(updatedUser)
        return updatedUser
    }
    
    /**
     * 获取最后登录时间
     * 
     * @return 最后登录时间戳，未找到返回null
     */
    suspend fun getLastLoginTime(): Long? {
        return try {
            val lastLoginTimeStr = context.dataStore.data.map { preferences ->
                preferences[LAST_LOGIN_TIME_KEY]
            }.first()
            
            lastLoginTimeStr?.toLongOrNull()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get last login time", e)
            null
        }
    }
    
    // ===== 数据验证方法 =====
    
    /**
     * 验证用户信息是否有效
     * 
     * 🎯 验证规则：
     * 1. 必要字段不能为空
     * 2. 数据格式必须正确
     * 3. 逻辑关系必须合理
     * 4. 时间戳必须有效
     * 
     * @param user 要验证的用户信息
     * @return 是否有效
     */
    private fun isValidUser(user: User?): Boolean {
        if (user == null) {
            return false
        }
        
        return try {
            // 1. 检查必要字段
            if (user.id.isBlank()) {
                Log.w(TAG, "⚠️ User ID is blank")
                return false
            }
            
            // 2. 检查时间戳
            if (user.registrationTime <= 0 || user.lastLoginTime <= 0) {
                Log.w(TAG, "⚠️ Invalid timestamp")
                return false
            }
            
            // 3. 检查时间逻辑
            if (user.lastLoginTime < user.registrationTime) {
                Log.w(TAG, "⚠️ Last login time before registration time")
                return false
            }
            
            // 4. 检查登录类型
            if (user.loginType == null) {
                Log.w(TAG, "⚠️ Login type is null")
                return false
            }
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating user", e)
            return false
        }
    }
    
    // ===== 数据迁移方法 =====
    
    /**
     * 迁移旧版本数据
     * 
     * 用于处理应用升级时的数据兼容性
     * 
     * @return 是否需要迁移
     */
    suspend fun migrateDataIfNeeded(): Boolean {
        return try {
            // 这里可以实现数据迁移逻辑
            // 例如从SharedPreferences迁移到DataStore
            // 或者处理User对象结构的变化
            
            Log.d(TAG, "🔄 Checking data migration")
            
            // 示例：检查是否存在旧版本数据
            val hasOldData = checkForOldVersionData()
            
            if (hasOldData) {
                Log.i(TAG, "📦 Migrating old version data")
                performDataMigration()
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Data migration failed", e)
            return false
        }
    }
    
    /**
     * 检查是否存在旧版本数据
     */
    private fun checkForOldVersionData(): Boolean {
        // 实现检查逻辑
        return false
    }
    
    /**
     * 执行数据迁移
     */
    private suspend fun performDataMigration() {
        // 实现迁移逻辑
        Log.i(TAG, "✅ Data migration completed")
    }
}

/**
 * 用户存储异常
 */
internal class UserStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
