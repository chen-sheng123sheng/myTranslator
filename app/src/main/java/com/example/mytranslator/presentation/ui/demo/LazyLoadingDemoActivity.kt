package com.example.mytranslator.presentation.ui.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.login.api.LazyLoginManager
import com.example.mytranslator.databinding.ActivityLazyLoadingDemoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 懒加载模式演示Activity
 * 
 * 🎯 演示目的：
 * 1. 展示不同懒加载模式的实现
 * 2. 对比各种模式的性能差异
 * 3. 提供学习和面试的参考案例
 * 4. 演示实际项目中的应用场景
 * 
 * 🏗️ 演示内容：
 * - Kotlin lazy委托
 * - 双重检查锁定
 * - 同步方法懒加载
 * - 枚举单例模式
 * - 性能对比测试
 * 
 * 📱 学习价值：
 * - 理解懒加载的核心概念
 * - 掌握不同模式的适用场景
 * - 学习性能优化技巧
 * - 准备面试相关问题
 * 
 * 🎓 面试要点：
 * 1. 懒加载的实现原理
 * 2. 线程安全的保证机制
 * 3. 性能优化的考虑
 * 4. 实际项目中的应用
 */
class LazyLoadingDemoActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LazyLoadingDemo"
    }
    
    // ViewBinding
    private lateinit var binding: ActivityLazyLoadingDemoBinding
    
    // ===== 演示不同的懒加载模式 =====
    
    /**
     * 1. Kotlin lazy委托（推荐）
     * 
     * 🔧 特点：
     * - 线程安全（默认SYNCHRONIZED模式）
     * - 代码简洁
     * - 性能优秀
     * - 内存友好（初始化后释放lambda引用）
     */
    private val lazyDelegateExample: String by lazy {
        Log.d(TAG, "🚀 Initializing lazy delegate example")
        Thread.sleep(100) // 模拟耗时操作
        "Lazy Delegate Initialized at ${System.currentTimeMillis()}"
    }
    
    /**
     * 2. 自定义懒加载（双重检查锁定）
     * 
     * 🔧 特点：
     * - 手动实现线程安全
     * - 性能优秀
     * - 代码复杂度中等
     * - 需要处理内存管理
     */
    @Volatile
    private var customLazyValue: String? = null
    
    private fun getCustomLazyValue(): String {
        if (customLazyValue == null) {
            synchronized(this) {
                if (customLazyValue == null) {
                    Log.d(TAG, "🚀 Initializing custom lazy value")
                    Thread.sleep(100) // 模拟耗时操作
                    customLazyValue = "Custom Lazy Initialized at ${System.currentTimeMillis()}"
                }
            }
        }
        return customLazyValue!!
    }
    
    /**
     * 3. 同步方法懒加载
     * 
     * 🔧 特点：
     * - 实现简单
     * - 线程安全
     * - 性能较差（每次访问都需要获取锁）
     * - 适合访问频率低的场景
     */
    @Volatile
    private var syncLazyValue: String? = null
    
    @Synchronized
    private fun getSyncLazyValue(): String {
        if (syncLazyValue == null) {
            Log.d(TAG, "🚀 Initializing sync lazy value")
            Thread.sleep(100) // 模拟耗时操作
            syncLazyValue = "Sync Lazy Initialized at ${System.currentTimeMillis()}"
        }
        return syncLazyValue!!
    }
    
    /**
     * 4. 使用LazyLoginManager演示
     */
    private val lazyLoginManager: LazyLoginManager by lazy {
        Log.d(TAG, "🚀 Getting LazyLoginManager instance")
        LazyLoginManager.instance.apply {
            setContext(this@LazyLoadingDemoActivity)
        }
    }
    
    // ===== Activity生命周期 =====
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewBinding
        binding = ActivityLazyLoadingDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置工具栏
        setupToolbar()
        
        // 设置UI
        setupUI()
        
        Log.d(TAG, "📱 LazyLoadingDemoActivity created - no lazy values initialized yet")
    }
    
    // ===== 初始化方法 =====
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "懒加载模式演示"
        }
    }
    
    private fun setupUI() {
        // 演示按钮点击事件
        binding.btnLazyDelegate.setOnClickListener {
            demonstrateLazyDelegate()
        }
        
        binding.btnCustomLazy.setOnClickListener {
            demonstrateCustomLazy()
        }
        
        binding.btnSyncLazy.setOnClickListener {
            demonstrateSyncLazy()
        }
        
        binding.btnLoginManager.setOnClickListener {
            demonstrateLoginManager()
        }
        
        binding.btnPerformanceTest.setOnClickListener {
            performanceTest()
        }
        
        binding.btnClearResults.setOnClickListener {
            clearResults()
        }
    }
    
    // ===== 演示方法 =====
    
    /**
     * 演示Kotlin lazy委托
     */
    private fun demonstrateLazyDelegate() {
        Log.d(TAG, "🎯 Demonstrating Kotlin lazy delegate")
        
        val startTime = System.nanoTime()
        val value = lazyDelegateExample // 首次访问会触发初始化
        val endTime = System.nanoTime()
        
        val result = """
            🚀 Kotlin Lazy Delegate:
            Value: $value
            Time: ${(endTime - startTime) / 1_000_000}ms
            
            特点：
            ✅ 线程安全
            ✅ 代码简洁
            ✅ 性能优秀
            ✅ 内存友好
        """.trimIndent()
        
        appendResult(result)
        
        // 第二次访问测试
        val startTime2 = System.nanoTime()
        val value2 = lazyDelegateExample // 直接返回缓存值
        val endTime2 = System.nanoTime()
        
        appendResult("第二次访问时间: ${(endTime2 - startTime2)}ns (缓存值)")
    }
    
    /**
     * 演示自定义懒加载
     */
    private fun demonstrateCustomLazy() {
        Log.d(TAG, "🎯 Demonstrating custom lazy loading")
        
        val startTime = System.nanoTime()
        val value = getCustomLazyValue()
        val endTime = System.nanoTime()
        
        val result = """
            🔧 Custom Lazy Loading:
            Value: $value
            Time: ${(endTime - startTime) / 1_000_000}ms
            
            特点：
            ✅ 手动控制
            ✅ 性能优秀
            ⚠️ 代码复杂
            ⚠️ 需要手动内存管理
        """.trimIndent()
        
        appendResult(result)
    }
    
    /**
     * 演示同步方法懒加载
     */
    private fun demonstrateSyncLazy() {
        Log.d(TAG, "🎯 Demonstrating synchronized lazy loading")
        
        val startTime = System.nanoTime()
        val value = getSyncLazyValue()
        val endTime = System.nanoTime()
        
        val result = """
            🔒 Synchronized Lazy Loading:
            Value: $value
            Time: ${(endTime - startTime) / 1_000_000}ms
            
            特点：
            ✅ 实现简单
            ✅ 线程安全
            ❌ 性能较差
            ⚠️ 每次访问都加锁
        """.trimIndent()
        
        appendResult(result)
    }
    
    /**
     * 演示LazyLoginManager
     */
    private fun demonstrateLoginManager() {
        Log.d(TAG, "🎯 Demonstrating LazyLoginManager")
        
        lifecycleScope.launch {
            try {
                val startTime = System.nanoTime()
                
                // 演示不同的懒加载模式
                lazyLoginManager.demonstrateLazyPatterns()

                // 演示内部组件的懒加载
                lazyLoginManager.demonstrateWeChatServiceLazy()
                lazyLoginManager.demonstrateUserStorageLazy()
                
                val endTime = System.nanoTime()
                
                val result = """
                    📱 LazyLoginManager Demo:
                    执行时间: ${(endTime - startTime) / 1_000_000}ms
                    
                    演示了以下模式：
                    🚀 Lazy委托
                    🔧 双重检查锁定
                    🔒 同步方法
                    🛡️ 枚举单例
                    
                    查看Logcat获取详细性能数据
                """.trimIndent()
                
                appendResult(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error demonstrating LazyLoginManager", e)
                appendResult("❌ 演示失败: ${e.message}")
            }
        }
    }
    
    /**
     * 性能测试
     */
    private fun performanceTest() {
        Log.d(TAG, "🎯 Starting performance test")
        
        lifecycleScope.launch {
            appendResult("🚀 开始性能测试...")
            
            // 测试多次访问的性能
            val iterations = 10000
            
            // 测试lazy委托
            val lazyStartTime = System.nanoTime()
            repeat(iterations) {
                lazyDelegateExample
            }
            val lazyEndTime = System.nanoTime()
            val lazyTime = (lazyEndTime - lazyStartTime) / 1_000_000
            
            // 测试自定义懒加载
            val customStartTime = System.nanoTime()
            repeat(iterations) {
                getCustomLazyValue()
            }
            val customEndTime = System.nanoTime()
            val customTime = (customEndTime - customStartTime) / 1_000_000
            
            // 测试同步方法
            val syncStartTime = System.nanoTime()
            repeat(iterations) {
                getSyncLazyValue()
            }
            val syncEndTime = System.nanoTime()
            val syncTime = (syncEndTime - syncStartTime) / 1_000_000
            
            val result = """
                📊 性能测试结果 ($iterations 次访问):
                
                🚀 Lazy委托: ${lazyTime}ms
                🔧 自定义懒加载: ${customTime}ms
                🔒 同步方法: ${syncTime}ms
                
                结论：
                ${if (lazyTime <= customTime && lazyTime <= syncTime) "✅ Lazy委托性能最佳" else ""}
                ${if (syncTime > lazyTime && syncTime > customTime) "❌ 同步方法性能最差" else ""}
            """.trimIndent()
            
            appendResult(result)
        }
    }
    
    /**
     * 清除结果
     */
    private fun clearResults() {
        binding.tvResults.text = "点击按钮开始演示懒加载模式...\n\n"
    }
    
    /**
     * 添加结果到显示区域
     */
    private fun appendResult(result: String) {
        val currentText = binding.tvResults.text.toString()
        binding.tvResults.text = "$currentText\n$result\n${"=".repeat(50)}\n"
        
        // 滚动到底部
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
    
    // ===== 工具栏事件 =====
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
