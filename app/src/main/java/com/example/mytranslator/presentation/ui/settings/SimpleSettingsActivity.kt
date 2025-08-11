package com.example.mytranslator.presentation.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.login.api.LoginManager
import com.example.mytranslator.R
import com.example.mytranslator.databinding.ActivitySimpleSettingsBinding
import com.example.mytranslator.presentation.ui.login.LoginActivity
import com.example.mytranslator.presentation.ui.demo.LazyLoadingDemoActivity
import kotlinx.coroutines.launch

/**
 * 简单设置Activity
 * 
 * 🎯 设计目的：
 * 1. 提供应用设置入口
 * 2. 集成登录功能（可选）
 * 3. 不阻塞主要功能的使用
 * 4. 适合学习项目的需求
 * 
 * 🏗️ 功能特性：
 * - 登录状态显示
 * - 登录/登出操作
 * - 用户信息显示
 * - 其他设置选项
 * 
 * 📱 使用场景：
 * - 从主界面的设置按钮进入
 * - 查看和管理登录状态
 * - 访问应用设置选项
 * 
 * 🎓 学习要点：
 * 1. 可选功能的设计模式
 * 2. 登录状态的管理
 * 3. 用户体验的优化
 * 4. 模块化功能的集成
 */
class SimpleSettingsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SimpleSettingsActivity"
    }
    
    // ViewBinding
    private lateinit var binding: ActivitySimpleSettingsBinding
    
    // Login模块管理器（可选）
    private var loginManager: LoginManager? = null
    
    // ===== Activity生命周期 =====
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewBinding
        binding = ActivitySimpleSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置工具栏
        setupToolbar()
        
        // 设置UI
        setupUI()
        
        // 检查登录状态
        checkLoginStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到页面时刷新登录状态
        checkLoginStatus()
    }
    
    // ===== 初始化方法 =====
    
    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "设置"
        }
    }
    
    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 登录相关按钮
        binding.btnLogin.setOnClickListener {
            openLoginActivity()
        }
        
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
        
        // 其他设置选项
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
        
        binding.btnClearCache.setOnClickListener {
            clearCache()
        }

        binding.btnLazyDemo.setOnClickListener {
            openLazyLoadingDemo()
        }
    }
    
    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        lifecycleScope.launch {
            try {
                // 尝试获取LoginManager实例
                loginManager = LoginManager.getInstance()
                
                // 检查是否已初始化
                if (!loginManager!!.isInitialized()) {
                    // 未初始化，显示未登录状态
                    updateLoginUI(false, null)
                    return@launch
                }
                
                // 检查登录状态
                val isLoggedIn = loginManager!!.isLoggedIn()
                val user = if (isLoggedIn) {
                    loginManager!!.getCurrentUser()
                } else {
                    null
                }
                
                updateLoginUI(isLoggedIn, user?.getDisplayName())
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to check login status", e)
                updateLoginUI(false, null)
            }
        }
    }
    
    /**
     * 更新登录UI状态
     */
    private fun updateLoginUI(isLoggedIn: Boolean, userName: String?) {
        if (isLoggedIn && userName != null) {
            // 已登录状态
            binding.tvLoginStatus.text = "已登录：$userName"
            binding.btnLogin.text = "切换账号"
            binding.btnLogout.isEnabled = true
            binding.cardLoginInfo.setCardBackgroundColor(getColor(R.color.success_color))
        } else {
            // 未登录状态
            binding.tvLoginStatus.text = "未登录"
            binding.btnLogin.text = "登录"
            binding.btnLogout.isEnabled = false
            binding.cardLoginInfo.setCardBackgroundColor(getColor(R.color.background_color_secondary))
        }
    }
    
    // ===== 登录相关方法 =====
    
    /**
     * 打开登录Activity
     */
    private fun openLoginActivity() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open login activity", e)
            Toast.makeText(this, "无法打开登录页面", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 执行登出操作
     */
    private fun performLogout() {
        lifecycleScope.launch {
            try {
                loginManager?.logout()
                updateLoginUI(false, null)
                Toast.makeText(this@SimpleSettingsActivity, "已退出登录", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to logout", e)
                Toast.makeText(this@SimpleSettingsActivity, "退出登录失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // ===== 其他设置方法 =====
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于翻译助手")
            .setMessage("这是一个Android学习项目\n\n版本：1.0.0\n\n功能特性：\n• 文本翻译\n• 语音翻译\n• 历史记录\n• 多语言支持\n• 登录功能（可选）")
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 清除缓存
     */
    private fun clearCache() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清除缓存")
            .setMessage("确定要清除应用缓存吗？这将删除翻译历史记录和临时文件。")
            .setPositiveButton("确定") { _, _ ->
                // 这里可以实现清除缓存的逻辑
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 打开懒加载演示页面
     */
    private fun openLazyLoadingDemo() {
        try {
            val intent = Intent(this, LazyLoadingDemoActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open lazy loading demo", e)
            Toast.makeText(this, "无法打开懒加载演示页面", Toast.LENGTH_SHORT).show()
        }
    }
    
    // ===== 工具栏事件 =====
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
