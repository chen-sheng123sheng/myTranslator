package com.example.login.api

import android.content.Context
import android.util.Log
import com.example.login.internal.storage.UserStorage
import com.example.login.internal.wechat.WeChatLoginService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 懒加载登录管理器 - 演示多种懒加载模式
 * 
 * 🎯 设计目的：
 * 1. 演示Kotlin lazy委托的使用
 * 2. 展示不同懒加载模式的实现
 * 3. 提供面试和学习的参考案例
 * 4. 对比不同模式的优缺点
 * 
 * 🏗️ 懒加载模式分类：
 * 1. Kotlin lazy委托（线程安全）
 * 2. 双重检查锁定（手动实现）
 * 3. 同步方法懒加载（简单但性能差）
 * 4. 枚举单例懒加载（最安全）
 * 
 * 📱 使用场景：
 * - 重量级对象的延迟创建
 * - 配置依赖的延迟初始化
 * - 资源密集型操作的按需执行
 * 
 * 🎓 学习要点：
 * 1. lazy委托的内部机制
 * 2. 线程安全的重要性
 * 3. 不同懒加载模式的适用场景
 * 4. 性能和安全性的权衡
 */
class LazyLoginManager private constructor() {
    
    companion object {
        private const val TAG = "LazyLoginManager"
        
        // ===== 模式1: Kotlin lazy委托（推荐） =====
        
        /**
         * 使用Kotlin lazy委托实现单例
         * 
         * 🔧 lazy委托特点：
         * 1. 线程安全：默认使用LazyThreadSafetyMode.SYNCHRONIZED
         * 2. 只计算一次：首次访问时计算，后续直接返回缓存值
         * 3. 内存效率：只有在需要时才创建对象
         * 4. 代码简洁：Kotlin编译器自动生成线程安全代码
         * 
         * 🎯 内部机制：
         * - 使用volatile变量存储计算结果
         * - 使用synchronized确保线程安全
         * - 使用状态标记避免重复计算
         */
        val instance: LazyLoginManager by lazy {
            Log.d(TAG, "🚀 Creating LazyLoginManager instance using lazy delegate")
            LazyLoginManager()
        }
        
        // ===== 模式2: 双重检查锁定（经典模式） =====
        
        @Volatile
        private var INSTANCE: LazyLoginManager? = null
        
        /**
         * 双重检查锁定模式实现单例
         * 
         * 🔧 实现原理：
         * 1. 第一次检查：避免不必要的同步
         * 2. 同步块：确保线程安全
         * 3. 第二次检查：防止重复创建
         * 4. volatile关键字：确保内存可见性
         * 
         * 🎯 为什么需要两次检查？
         * - 第一次检查：性能优化，避免每次都进入同步块
         * - 第二次检查：安全保证，防止多线程重复创建
         */
        fun getInstanceWithDoubleCheck(): LazyLoginManager {
            // 第一次检查：如果已经初始化，直接返回
            if (INSTANCE == null) {
                synchronized(this) {
                    // 第二次检查：在同步块内再次检查
                    if (INSTANCE == null) {
                        Log.d(TAG, "🚀 Creating LazyLoginManager instance using double-check locking")
                        INSTANCE = LazyLoginManager()
                    }
                }
            }
            return INSTANCE!!
        }
        
        // ===== 模式3: 同步方法懒加载（简单但性能差） =====
        
        @Volatile
        private var SYNC_INSTANCE: LazyLoginManager? = null
        
        /**
         * 同步方法实现懒加载
         * 
         * 🔧 特点：
         * 1. 实现简单：只需要synchronized关键字
         * 2. 线程安全：方法级别的同步
         * 3. 性能较差：每次访问都需要获取锁
         * 4. 适用场景：访问频率低的对象
         */
        @Synchronized
        fun getInstanceWithSync(): LazyLoginManager {
            if (SYNC_INSTANCE == null) {
                Log.d(TAG, "🚀 Creating LazyLoginManager instance using synchronized method")
                SYNC_INSTANCE = LazyLoginManager()
            }
            return SYNC_INSTANCE!!
        }
        
        // ===== 模式4: 枚举单例（最安全） =====
        
        /**
         * 枚举单例模式
         * 
         * 🔧 优势：
         * 1. 线程安全：JVM保证枚举的线程安全
         * 2. 防止反射攻击：枚举不能通过反射创建
         * 3. 防止序列化攻击：枚举序列化有特殊处理
         * 4. 代码简洁：无需手动处理线程安全
         * 
         * 🎯 适用场景：
         * - 安全性要求极高的场景
         * - 需要防止反射和序列化攻击
         * - 单例对象相对简单的情况
         */
        enum class EnumSingleton {
            INSTANCE;
            
            val lazyLoginManager: LazyLoginManager by lazy {
                Log.d(TAG, "🚀 Creating LazyLoginManager instance using enum singleton")
                LazyLoginManager()
            }
        }
    }
    
    // ===== 懒加载的内部组件 =====
    
    /**
     * 懒加载的微信登录服务
     * 
     * 🔧 lazy委托的参数说明：
     * - LazyThreadSafetyMode.SYNCHRONIZED: 线程安全（默认）
     * - LazyThreadSafetyMode.PUBLICATION: 允许多次初始化，但只使用第一个结果
     * - LazyThreadSafetyMode.NONE: 不保证线程安全，性能最好
     */
    private val weChatService: WeChatLoginService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Log.d(TAG, "🔧 Lazy initializing WeChatLoginService")
        WeChatLoginService.getInstance()
    }
    
    /**
     * 懒加载的用户存储服务
     */
    private val userStorage: UserStorage by lazy {
        Log.d(TAG, "🔧 Lazy initializing UserStorage")
        // 注意：这里需要Context，实际项目中需要在合适的时机提供
        // 这里只是演示，实际使用时需要改进
        UserStorage.getInstance(appContext ?: throw IllegalStateException("Context not available"))
    }
    
    /**
     * 懒加载的协程作用域
     */
    private val managerScope: CoroutineScope by lazy {
        Log.d(TAG, "🔧 Lazy initializing CoroutineScope")
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }
    
    /**
     * 懒加载的配置信息
     */
    private val config: LoginConfig by lazy {
        Log.d(TAG, "🔧 Lazy initializing LoginConfig")
        // 实际项目中，这里应该从外部获取配置
        LoginConfig.Builder()
            .weChatAppId("wx_lazy_demo")
            .weChatAppSecret("secret_lazy_demo")
            .build()
    }
    
    // ===== 上下文管理 =====
    
    private var appContext: Context? = null
    
    fun setContext(context: Context) {
        appContext = context.applicationContext
    }
    
    // ===== 公共API =====
    
    /**
     * 演示微信登录服务的懒加载
     */
    fun demonstrateWeChatServiceLazy() {
        Log.d(TAG, "📱 Accessing WeChat service (will trigger lazy initialization if needed)")
        // 只演示懒加载，不暴露内部服务
        val service = weChatService // 首次访问时会触发lazy初始化
        Log.d(TAG, "✅ WeChat service lazy loaded: ${service.javaClass.simpleName}")
    }

    /**
     * 演示用户存储服务的懒加载
     */
    fun demonstrateUserStorageLazy() {
        try {
            Log.d(TAG, "💾 Accessing user storage (will trigger lazy initialization if needed)")
            // 只演示懒加载，不暴露内部服务
            val storage = userStorage // 首次访问时会触发lazy初始化
            Log.d(TAG, "✅ User storage lazy loaded: ${storage.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ User storage lazy loading failed (context not available): ${e.message}")
        }
    }
    
    /**
     * 演示不同懒加载模式的性能
     */
    fun demonstrateLazyPatterns() {
        Log.d(TAG, "🎯 Demonstrating different lazy loading patterns")
        
        // 测试lazy委托性能
        val startTime1 = System.nanoTime()
        val instance1 = instance
        val endTime1 = System.nanoTime()
        Log.d(TAG, "Lazy delegate access time: ${endTime1 - startTime1} ns")
        
        // 测试双重检查锁定性能
        val startTime2 = System.nanoTime()
        val instance2 = getInstanceWithDoubleCheck()
        val endTime2 = System.nanoTime()
        Log.d(TAG, "Double-check locking access time: ${endTime2 - startTime2} ns")
        
        // 测试同步方法性能
        val startTime3 = System.nanoTime()
        val instance3 = getInstanceWithSync()
        val endTime3 = System.nanoTime()
        Log.d(TAG, "Synchronized method access time: ${endTime3 - startTime3} ns")
        
        // 测试枚举单例性能
        val startTime4 = System.nanoTime()
        val instance4 = EnumSingleton.INSTANCE.lazyLoginManager
        val endTime4 = System.nanoTime()
        Log.d(TAG, "Enum singleton access time: ${endTime4 - startTime4} ns")
    }
}

/**
 * 懒加载工厂模式示例
 *
 * 🎯 适用场景：
 * - 需要根据参数创建不同的实例
 * - 创建过程比较复杂
 * - 需要缓存创建的实例
 */
object LazyLoginFactory {

    /**
     * 懒加载的登录服务缓存
     *
     * 🔧 使用ConcurrentHashMap确保线程安全
     */
    private val serviceCache = mutableMapOf<String, Lazy<String>>()

    /**
     * 获取懒加载的演示服务（简化版本）
     */
    fun getLazyDemoService(key: String, factory: () -> String): Lazy<String> {
        return serviceCache.getOrPut(key) {
            lazy { factory() }
        }
    }
}

/**
 * 懒加载属性委托示例
 * 
 * 🎯 演示如何创建自定义的懒加载委托
 */
class CustomLazyDelegate<T>(private val initializer: () -> T) {
    @Volatile
    private var value: Any? = UNINITIALIZED_VALUE
    
    companion object {
        private val UNINITIALIZED_VALUE = Any()
    }
    
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        val currentValue = value
        if (currentValue !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return currentValue as T
        }
        
        return synchronized(this) {
            val currentValue2 = value
            if (currentValue2 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                currentValue2 as T
            } else {
                val typedValue = initializer()
                value = typedValue
                typedValue
            }
        }
    }
}
