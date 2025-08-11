package com.example.mytranslator.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.login.internal.wechat.WeChatLoginService
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * 微信回调Activity - Login模块版本
 * 
 * 🎯 重要说明：
 * 1. 包名路径：必须是{主应用包名}.wxapi.WXEntryActivity
 * 2. 当前路径：com.example.mytranslator.wxapi.WXEntryActivity ✅
 * 3. 虽然在login模块中，但使用主应用包名
 * 4. 这样既满足微信SDK要求，又保持模块化架构
 * 
 * 🏗️ 模块化设计：
 * - 代码在login模块中，保持功能内聚
 * - 使用主应用包名，满足微信SDK要求
 * - 通过AndroidManifest合并机制生效
 * - 保持模块的独立性和可复用性
 * 
 * 📱 工作原理：
 * 1. 编译时，login模块的清单文件会合并到主应用
 * 2. 微信客户端通过包名约定找到这个Activity
 * 3. Activity在login模块中，直接调用模块内的服务
 * 4. 实现了功能内聚和包名要求的完美结合
 * 
 * 🎓 学习要点：
 * 1. 模块化架构中的第三方SDK集成策略
 * 2. AndroidManifest合并机制的应用
 * 3. 包名约定与模块化的平衡
 * 4. 跨模块依赖的最佳实践
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {
    
    companion object {
        private const val TAG = "WXEntryActivity"
    }
    
    // 微信API实例
    private var wxApi: IWXAPI? = null
    
    // ===== Activity生命周期 =====
    
    /**
     * Activity创建时的初始化
     * 
     * 🎯 初始化流程：
     * 1. 创建微信API实例
     * 2. 注册到微信SDK
     * 3. 处理Intent中的回调数据
     * 4. 设置透明主题（用户无感知）
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "🚀 WXEntryActivity created in login module")
            
            // 1. 从login模块配置中获取微信AppID
            val appId = getWeChatAppId()
            
            // 2. 创建微信API实例
            wxApi = WXAPIFactory.createWXAPI(this, appId, false)
            
            // 3. 处理微信回调
            val intent = intent
            if (intent != null) {
                wxApi?.handleIntent(intent, this)
            } else {
                Log.w(TAG, "⚠️ Intent is null, finishing activity")
                finish()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onCreate", e)
            finish()
        }
    }
    
    /**
     * 处理新的Intent（当Activity已存在时）
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        try {
            Log.d(TAG, "📨 Received new intent")
            
            setIntent(intent)
            if (intent != null) {
                wxApi?.handleIntent(intent, this)
            } else {
                Log.w(TAG, "⚠️ New intent is null")
                finish()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onNewIntent", e)
            finish()
        }
    }
    
    // ===== 微信回调处理 =====
    
    /**
     * 处理微信请求
     * 
     * @param baseReq 微信请求对象
     */
    override fun onReq(baseReq: BaseReq?) {
        Log.d(TAG, "📥 Received WeChat request: ${baseReq?.type}")
        
        try {
            when (baseReq?.type) {
                // 可以在这里处理不同类型的微信请求
                else -> {
                    Log.d(TAG, "🤷 Unhandled request type: ${baseReq?.type}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling WeChat request", e)
        } finally {
            // 处理完请求后关闭Activity
            finish()
        }
    }
    
    /**
     * 处理微信响应
     * 
     * 🎯 这是登录流程的关键方法
     * 
     * @param baseResp 微信响应对象
     */
    override fun onResp(baseResp: BaseResp?) {
        Log.d(TAG, "📤 Received WeChat response: type=${baseResp?.type}, errCode=${baseResp?.errCode}")
        
        try {
            when (baseResp?.type) {
                // 处理登录授权响应
                1 -> handleAuthResponse(baseResp as? SendAuth.Resp)
                
                // 处理分享响应
                2 -> handleShareResponse(baseResp)
                
                // 处理支付响应
                5 -> handlePayResponse(baseResp)
                
                else -> {
                    Log.w(TAG, "⚠️ Unknown response type: ${baseResp?.type}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling WeChat response", e)
            
            // 发生异常时，通知登录服务
            notifyLoginError("处理微信回调时发生异常: ${e.message}")
            
        } finally {
            // 处理完响应后关闭Activity
            finish()
        }
    }
    
    /**
     * 处理登录授权响应
     * 
     * 🎯 直接调用login模块内的服务处理
     * 
     * @param authResp 授权响应对象
     */
    private fun handleAuthResponse(authResp: SendAuth.Resp?) {
        if (authResp == null) {
            Log.e(TAG, "❌ Auth response is null")
            notifyLoginError("授权响应为空")
            return
        }
        
        Log.d(TAG, "🔐 Handling auth response: errCode=${authResp.errCode}, code=${authResp.code}")
        
        try {
            // 🔗 直接调用同模块内的服务，无跨模块调用
            val loginService = WeChatLoginService.getInstance()
            
            loginService.handleWeChatCallback(
                code = authResp.code,
                state = authResp.state,
                errCode = authResp.errCode,
                errStr = authResp.errStr
            )
            
            Log.i(TAG, "✅ Auth response handled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to handle auth response", e)
            notifyLoginError("处理授权响应失败: ${e.message}")
        }
    }
    
    /**
     * 处理分享响应
     */
    private fun handleShareResponse(shareResp: BaseResp) {
        Log.d(TAG, "📤 Share response: errCode=${shareResp.errCode}")
        // 可以在这里处理分享结果
    }
    
    /**
     * 处理支付响应
     */
    private fun handlePayResponse(payResp: BaseResp) {
        Log.d(TAG, "💰 Pay response: errCode=${payResp.errCode}")
        // 可以在这里处理支付结果
    }
    
    // ===== 私有辅助方法 =====
    
    /**
     * 获取微信AppID
     * 
     * 🎯 从login模块的配置中获取，保持一致性
     */
    private fun getWeChatAppId(): String {
        // 这里应该从login模块的配置中获取
        // 为了演示，返回一个示例值
        return "wx1234567890abcdef"
        
        // 实际实现可以：
        // 1. 从WeChatLoginService获取配置
        // 2. 从SharedPreferences读取
        // 3. 从BuildConfig获取
    }
    
    /**
     * 通知登录错误
     */
    private fun notifyLoginError(errorMessage: String) {
        try {
            val loginService = WeChatLoginService.getInstance()
            loginService.handleWeChatCallback(
                code = null,
                state = null,
                errCode = -999,  // 自定义错误码
                errStr = errorMessage
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify login error", e)
        }
    }
    
    // ===== Activity生命周期管理 =====
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔚 WXEntryActivity destroyed")
        
        // 清理资源
        wxApi = null
    }
    
    /**
     * 禁用返回键
     */
    override fun onBackPressed() {
        // 不调用super.onBackPressed()，禁用返回键
        Log.d(TAG, "🚫 Back key disabled during WeChat callback")
    }
}
