package com.example.mytranslator.presentation.ui.login

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.login.api.*
import com.example.mytranslator.databinding.ActivityNewLoginBinding
import com.example.mytranslator.presentation.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * 新的登录Activity - 使用login模块
 * 
 * 🎯 功能特性：
 * 1. 使用新的login模块API
 * 2. 支持多种登录方式
 * 3. 完整的错误处理和用户反馈
 * 4. 现代化的UI设计
 * 
 * 🏗️ 技术架构：
 * - 使用ViewBinding进行视图绑定
 * - 协程处理异步操作
 * - 状态管理和进度显示
 * - 响应式UI更新
 * 
 * 📱 支持的登录方式：
 * - 微信应用内登录
 * - 微信二维码登录
 * - 游客登录
 * 
 * 🎓 学习要点：
 * 1. 模块化API的使用方式
 * 2. 异步登录流程的处理
 * 3. 用户体验的优化
 * 4. 错误处理的最佳实践
 */
class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NewLoginActivity"
    }
    
    // ViewBinding
    private lateinit var binding: ActivityNewLoginBinding
    
    // Login模块管理器
    private lateinit var loginManager: LoginManager
    
    // 当前登录状态
    private var isLoggingIn = false
    
    // ===== Activity生命周期 =====
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewBinding
        binding = ActivityNewLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化login模块
        initializeLoginModule()
        
        // 设置UI
        setupUI()
        
        // 检查登录状态
        checkLoginStatus()
    }
    
    // ===== 初始化方法 =====
    
    /**
     * 初始化login模块（自动延迟初始化）
     */
    private fun initializeLoginModule() {
        try {
            Log.d(TAG, "🚀 Getting login module instance")

            // 获取LoginManager实例
            // 如果在Application中已经注册，这里会自动进行延迟初始化
            loginManager = LoginManager.getInstance()

            Log.i(TAG, "✅ Login module ready")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get login module", e)
            showError("登录模块获取失败: ${e.message}")
        }
    }
    
    /**
     * 设置UI界面
     */
    private fun setupUI() {
        // 设置标题
        binding.tvTitle.text = "选择登录方式"
        
        // 设置按钮点击事件
        binding.btnWechatApp.setOnClickListener {
            if (!isLoggingIn) {
                loginWithWeChatApp()
            }
        }
        
        binding.btnWechatQr.setOnClickListener {
            if (!isLoggingIn) {
                loginWithWeChatQR()
            }
        }
        
        binding.btnGuest.setOnClickListener {
            if (!isLoggingIn) {
                loginAsGuest()
            }
        }
        
        // 设置微信状态检查按钮
        binding.btnCheckWechat.setOnClickListener {
            checkWeChatStatus()
        }
        
        // 初始状态
        updateUIState(false)
    }
    
    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        lifecycleScope.launch {
            try {
                val isLoggedIn = loginManager.isLoggedIn()
                if (isLoggedIn) {
                    val user = loginManager.getCurrentUser()
                    Log.d(TAG, "👤 User already logged in: ${user?.getSummary()}")
                    
                    // 用户已登录，跳转到主界面
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to check login status", e)
            }
        }
    }
    
    // ===== 登录方法 =====
    
    /**
     * 微信应用内登录
     */
    private fun loginWithWeChatApp() {
        Log.d(TAG, "📱 Starting WeChat app login")
        
        // 先检查微信状态
        val weChatStatus = loginManager.checkWeChatStatus()
        if (!weChatStatus.canUseAppLogin()) {
            showWeChatStatusDialog(weChatStatus)
            return
        }
        
        updateUIState(true)
        showProgress("正在启动微信登录...")
        
        loginManager.loginWithWeChatApp(object : LoginCallback {
            override fun onSuccess(result: LoginResult.Success) {
                Log.i(TAG, "✅ WeChat app login successful")
                
                val user = result.user
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    showSuccess("登录成功，欢迎 ${user.getDisplayName()}")
                    
                    // 延迟跳转，让用户看到成功消息
                    binding.root.postDelayed({
                        navigateToMain()
                    }, 1500)
                }
            }
            
            override fun onFailure(result: LoginResult.Failure) {
                Log.e(TAG, "❌ WeChat app login failed: ${result.message}")
                
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    handleLoginError(result.error, result.message)
                }
            }
            
            override fun onProgress(progress: LoginProgress) {
                runOnUiThread {
                    showProgress(progress.message)
                }
            }
        })
    }
    
    /**
     * 微信二维码登录
     */
    private fun loginWithWeChatQR() {
        Log.d(TAG, "📱 Starting WeChat QR code login")
        
        updateUIState(true)
        showProgress("正在生成二维码...")
        
        loginManager.loginWithWeChatQR(object : LoginCallback {
            override fun onSuccess(result: LoginResult.Success) {
                Log.i(TAG, "✅ WeChat QR login successful")
                
                val user = result.user
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    hideQRCode()
                    showSuccess("扫码登录成功，欢迎 ${user.getDisplayName()}")
                    
                    // 延迟跳转
                    binding.root.postDelayed({
                        navigateToMain()
                    }, 1500)
                }
            }
            
            override fun onFailure(result: LoginResult.Failure) {
                Log.e(TAG, "❌ WeChat QR login failed: ${result.message}")
                
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    hideQRCode()
                    handleLoginError(result.error, result.message)
                }
            }
            
            override fun onProgress(progress: LoginProgress) {
                runOnUiThread {
                    when (progress.type) {
                        ProgressType.QR_CODE_GENERATING -> {
                            showProgress("正在生成二维码...")
                        }
                        
                        ProgressType.QR_CODE_GENERATED -> {
                            hideProgress()
                            showProgress("请使用微信扫描二维码")
                            
                            // 显示二维码
                            val qrCodeBitmap = progress.data as? Bitmap
                            if (qrCodeBitmap != null) {
                                showQRCode(qrCodeBitmap)
                            }
                        }
                        
                        ProgressType.QR_CODE_SCANNED -> {
                            showProgress("已扫码，请在微信中确认登录")
                        }
                        
                        ProgressType.QR_CODE_EXPIRED -> {
                            hideQRCode()
                            showError("二维码已过期，请重新生成")
                            updateUIState(false)
                        }
                        
                        else -> {
                            showProgress(progress.message)
                        }
                    }
                }
            }
        })
    }
    
    /**
     * 游客登录
     */
    private fun loginAsGuest() {
        Log.d(TAG, "👤 Starting guest login")
        
        updateUIState(true)
        showProgress("正在创建游客账户...")
        
        loginManager.loginAsGuest(object : LoginCallback {
            override fun onSuccess(result: LoginResult.Success) {
                Log.i(TAG, "✅ Guest login successful")
                
                val user = result.user
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    showSuccess("游客登录成功，开始体验应用")
                    
                    // 延迟跳转
                    binding.root.postDelayed({
                        navigateToMain()
                    }, 1500)
                }
            }
            
            override fun onFailure(result: LoginResult.Failure) {
                Log.e(TAG, "❌ Guest login failed: ${result.message}")
                
                runOnUiThread {
                    updateUIState(false)
                    hideProgress()
                    showError("游客登录失败: ${result.message}")
                }
            }
        })
    }
    
    // ===== UI更新方法 =====
    
    /**
     * 更新UI状态
     */
    private fun updateUIState(isLoading: Boolean) {
        isLoggingIn = isLoading
        
        binding.btnWechatApp.isEnabled = !isLoading
        binding.btnWechatQr.isEnabled = !isLoading
        binding.btnGuest.isEnabled = !isLoading
        binding.btnCheckWechat.isEnabled = !isLoading
        
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    /**
     * 显示进度信息
     */
    private fun showProgress(message: String) {
        binding.tvProgress.text = message
        binding.tvProgress.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
    }
    
    /**
     * 隐藏进度信息
     */
    private fun hideProgress() {
        binding.tvProgress.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }
    
    /**
     * 显示二维码
     */
    private fun showQRCode(bitmap: Bitmap) {
        binding.ivQrCode.setImageBitmap(bitmap)
        binding.ivQrCode.visibility = View.VISIBLE
        binding.tvQrHint.visibility = View.VISIBLE
    }
    
    /**
     * 隐藏二维码
     */
    private fun hideQRCode() {
        binding.ivQrCode.visibility = View.GONE
        binding.tvQrHint.visibility = View.GONE
    }
    
    /**
     * 显示成功消息
     */
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示错误消息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    // ===== 辅助方法 =====
    
    /**
     * 检查微信状态
     */
    private fun checkWeChatStatus() {
        val status = loginManager.checkWeChatStatus()
        showWeChatStatusDialog(status)
    }
    
    /**
     * 显示微信状态对话框
     */
    private fun showWeChatStatusDialog(status: WeChatStatus) {
        val message = when (status) {
            WeChatStatus.AVAILABLE -> "✅ 微信客户端正常，可以使用应用内登录"
            WeChatStatus.NOT_INSTALLED -> "❌ 微信客户端未安装，建议使用二维码登录"
            WeChatStatus.VERSION_TOO_LOW -> "⚠️ 微信版本过低，建议更新后使用应用内登录"
            WeChatStatus.NOT_SUPPORTED -> "❌ 微信客户端不支持登录，请使用二维码登录"
            WeChatStatus.UNKNOWN -> "❓ 无法确定微信状态，建议尝试登录"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("微信状态检查")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 处理登录错误
     */
    private fun handleLoginError(error: LoginError, message: String) {
        when (error) {
            LoginError.WECHAT_NOT_INSTALLED -> {
                showWeChatNotInstalledDialog()
            }
            
            LoginError.WECHAT_VERSION_LOW -> {
                showWeChatVersionLowDialog()
            }
            
            LoginError.USER_CANCELLED -> {
                showError("登录已取消")
            }
            
            LoginError.QR_CODE_EXPIRED -> {
                showError("二维码已过期，请重新生成")
            }
            
            else -> {
                showError("登录失败: $message")
            }
        }
    }
    
    /**
     * 显示微信未安装对话框
     */
    private fun showWeChatNotInstalledDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("微信未安装")
            .setMessage("检测到您的设备未安装微信客户端，建议：\n\n1. 安装微信客户端后使用应用内登录\n2. 使用二维码登录\n3. 使用游客登录体验应用")
            .setPositiveButton("二维码登录") { _, _ ->
                loginWithWeChatQR()
            }
            .setNegativeButton("游客登录") { _, _ ->
                loginAsGuest()
            }
            .setNeutralButton("取消", null)
            .show()
    }
    
    /**
     * 显示微信版本过低对话框
     */
    private fun showWeChatVersionLowDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("微信版本过低")
            .setMessage("您的微信版本过低，不支持应用内登录，建议：\n\n1. 更新微信到最新版本\n2. 使用二维码登录\n3. 使用游客登录体验应用")
            .setPositiveButton("二维码登录") { _, _ ->
                loginWithWeChatQR()
            }
            .setNegativeButton("游客登录") { _, _ ->
                loginAsGuest()
            }
            .setNeutralButton("取消", null)
            .show()
    }
    
    /**
     * 跳转到主界面
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    // ===== Activity生命周期 =====
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 清理资源
        if (::loginManager.isInitialized) {
            // 如果有正在进行的登录，可以在这里取消
        }
    }
}
