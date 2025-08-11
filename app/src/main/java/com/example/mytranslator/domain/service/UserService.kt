package com.example.mytranslator.domain.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mytranslator.domain.model.User
import com.example.mytranslator.domain.model.LoginType
import com.example.mytranslator.domain.model.UserStatus
import com.example.mytranslator.domain.model.UserPreferences
import com.example.mytranslator.domain.model.MembershipInfo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户服务
 *
 * 🎯 设计目的：
 * 1. 管理用户信息的存储和读取
 * 2. 处理用户登录状态的持久化
 * 3. 提供用户信息的增删改查功能
 * 4. 管理用户偏好设置和会员信息
 *
 * 🏗️ 服务设计：
 * - 数据持久化：使用SharedPreferences存储用户信息
 * - JSON序列化：使用Gson进行对象序列化
 * - 异步操作：所有IO操作都在后台线程执行
 * - 错误处理：完善的异常处理和数据恢复
 *
 * 📱 功能特性：
 * - 用户信息的本地存储
 * - 登录状态的持久化
 * - 用户偏好设置管理
 * - 会员信息管理
 *
 * 🎓 学习要点：
 * 用户服务的设计模式：
 * 1. 单例模式 - 确保全局唯一的用户状态
 * 2. 仓储模式 - 封装数据存储的复杂性
 * 3. 策略模式 - 支持多种存储策略
 * 4. 观察者模式 - 通知用户状态变化
 */
class UserService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "UserService"
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_LOGIN_STATUS = "login_status"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        
        @Volatile
        private var INSTANCE: UserService? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): UserService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ===== 存储相关 =====
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // ===== 用户状态 =====
    
    private var currentUser: User? = null
    private var isLoggedIn: Boolean = false

    init {
        Log.d(TAG, "🚀 UserService 初始化")
        loadUserFromStorage()
    }

    // ===== 用户管理方法 =====

    /**
     * 获取当前用户
     *
     * @return 当前登录的用户，如果未登录则返回null
     */
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        return@withContext currentUser
    }

    /**
     * 设置当前用户
     *
     * @param user 要设置的用户
     */
    suspend fun setCurrentUser(user: User?) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "👤 设置当前用户: ${user?.getDisplayName() ?: "null"}")
            
            currentUser = user
            isLoggedIn = user != null
            
            // 保存到本地存储
            saveUserToStorage(user)
            
            // 更新登录状态
            sharedPrefs.edit()
                .putBoolean(KEY_LOGIN_STATUS, isLoggedIn)
                .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                .apply()
            
            Log.i(TAG, "✅ 用户状态已更新")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 设置当前用户时发生异常", e)
            throw e
        }
    }

    /**
     * 保存用户信息
     *
     * @param user 要保存的用户
     */
    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 保存用户信息: ${user.getDisplayName()}")
            
            // 更新最后登录时间
            val updatedUser = user.copy(lastLoginTime = System.currentTimeMillis())
            
            // 保存到本地存储
            saveUserToStorage(updatedUser)
            
            // 如果是当前用户，更新内存中的用户信息
            if (currentUser?.id == user.id) {
                currentUser = updatedUser
            }
            
            Log.i(TAG, "✅ 用户信息保存成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存用户信息时发生异常", e)
            throw e
        }
    }

    /**
     * 更新用户偏好设置
     *
     * @param preferences 新的偏好设置
     */
    suspend fun updateUserPreferences(preferences: UserPreferences) = withContext(Dispatchers.IO) {
        try {
            val user = currentUser
            if (user != null) {
                Log.d(TAG, "⚙️ 更新用户偏好设置")
                
                val updatedUser = user.copy(preferences = preferences)
                saveUser(updatedUser)
                currentUser = updatedUser
                
                Log.i(TAG, "✅ 用户偏好设置更新成功")
            } else {
                Log.w(TAG, "⚠️ 当前没有登录用户，无法更新偏好设置")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 更新用户偏好设置时发生异常", e)
            throw e
        }
    }

    /**
     * 更新会员信息
     *
     * @param membershipInfo 新的会员信息
     */
    suspend fun updateMembershipInfo(membershipInfo: MembershipInfo) = withContext(Dispatchers.IO) {
        try {
            val user = currentUser
            if (user != null) {
                Log.d(TAG, "💎 更新会员信息")
                
                val updatedUser = user.copy(membershipInfo = membershipInfo)
                saveUser(updatedUser)
                currentUser = updatedUser
                
                Log.i(TAG, "✅ 会员信息更新成功")
            } else {
                Log.w(TAG, "⚠️ 当前没有登录用户，无法更新会员信息")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 更新会员信息时发生异常", e)
            throw e
        }
    }

    /**
     * 用户登出
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚪 用户登出")
            
            currentUser = null
            isLoggedIn = false
            
            // 清除本地存储
            sharedPrefs.edit()
                .remove(KEY_CURRENT_USER)
                .putBoolean(KEY_LOGIN_STATUS, false)
                .apply()
            
            Log.i(TAG, "✅ 用户登出成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 用户登出时发生异常", e)
            throw e
        }
    }

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return isLoggedIn && currentUser != null
    }

    /**
     * 检查是否为游客用户
     *
     * @return 是否为游客用户
     */
    fun isGuestUser(): Boolean {
        return currentUser?.loginType == LoginType.GUEST
    }

    /**
     * 检查是否为VIP用户
     *
     * @return 是否为VIP用户
     */
    fun isVipUser(): Boolean {
        return currentUser?.isVipMember() ?: false
    }

    /**
     * 获取用户显示名称
     *
     * @return 用户显示名称
     */
    fun getUserDisplayName(): String {
        return currentUser?.getDisplayName() ?: "未登录"
    }

    // ===== 私有方法 =====

    /**
     * 从本地存储加载用户信息
     */
    private fun loadUserFromStorage() {
        try {
            Log.d(TAG, "📖 从本地存储加载用户信息")
            
            val userJson = sharedPrefs.getString(KEY_CURRENT_USER, null)
            if (userJson != null) {
                currentUser = gson.fromJson(userJson, User::class.java)
                isLoggedIn = sharedPrefs.getBoolean(KEY_LOGIN_STATUS, false)
                
                Log.i(TAG, "✅ 用户信息加载成功: ${currentUser?.getDisplayName()}")
            } else {
                Log.d(TAG, "📭 本地存储中没有用户信息")
                currentUser = null
                isLoggedIn = false
            }
            
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "❌ 用户信息JSON解析失败", e)
            clearCorruptedData()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载用户信息时发生异常", e)
            clearCorruptedData()
        }
    }

    /**
     * 保存用户信息到本地存储
     */
    private fun saveUserToStorage(user: User?) {
        try {
            if (user != null) {
                val userJson = gson.toJson(user)
                sharedPrefs.edit()
                    .putString(KEY_CURRENT_USER, userJson)
                    .apply()
                
                Log.d(TAG, "💾 用户信息已保存到本地存储")
            } else {
                sharedPrefs.edit()
                    .remove(KEY_CURRENT_USER)
                    .apply()
                
                Log.d(TAG, "🗑️ 用户信息已从本地存储清除")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存用户信息到本地存储时发生异常", e)
            throw e
        }
    }

    /**
     * 清除损坏的数据
     */
    private fun clearCorruptedData() {
        Log.w(TAG, "🧹 清除损坏的用户数据")
        
        currentUser = null
        isLoggedIn = false
        
        sharedPrefs.edit()
            .remove(KEY_CURRENT_USER)
            .putBoolean(KEY_LOGIN_STATUS, false)
            .apply()
    }

    /**
     * 获取最后登录时间
     *
     * @return 最后登录时间戳
     */
    fun getLastLoginTime(): Long {
        return sharedPrefs.getLong(KEY_LAST_LOGIN_TIME, 0)
    }

    /**
     * 清除所有用户数据
     * 用于应用重置或数据清理
     */
    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 清除所有用户数据")
            
            currentUser = null
            isLoggedIn = false
            
            sharedPrefs.edit().clear().apply()
            
            Log.i(TAG, "✅ 所有用户数据已清除")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清除用户数据时发生异常", e)
            throw e
        }
    }
}
