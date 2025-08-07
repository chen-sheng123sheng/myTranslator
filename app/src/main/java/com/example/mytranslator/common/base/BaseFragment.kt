package com.example.mytranslator.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.example.mytranslator.BuildConfig
import com.example.mytranslator.presentation.viewmodel.ViewModelFactory

/**
 * 基础Fragment类
 *
 * 设计思想：
 * 1. 模板方法模式：定义了Fragment的生命周期模板
 * 2. 泛型约束：使用ViewBinding泛型，确保类型安全
 * 3. 代码复用：所有Fragment都继承这个基类，避免重复代码
 * 4. 依赖注入管理：统一管理依赖注入的生命周期 -->暂未实现
 *
 * 为什么这样设计？
 * - 统一管理：所有Fragment的初始化流程都一样
 * - 易于维护：如果要修改所有Fragment的行为，只需修改这里
 * - 类型安全：ViewBinding泛型避免了findViewById的空指针风险
 * - 内存安全：自动管理ViewBinding的生命周期，防止内存泄漏
 *
 * Fragment与Activity的区别：
 * - Fragment有更复杂的生命周期（onCreateView, onViewCreated等）
 * - Fragment需要手动管理ViewBinding的销毁，避免内存泄漏
 * - Fragment可能会被重新创建，需要考虑状态保存和恢复
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    // private var：只有当前类可以访问
    // 使用可空类型，因为Fragment的View可能被销毁
    private var _binding: VB? = null

    // protected val：子类可以访问，但不能修改
    // 提供非空的binding访问，简化子类使用
    protected val binding get() = _binding!!

    /**
     * Fragment创建View的生命周期方法
     *
     * 🎯 关键方法：Fragment的View创建入口
     *
     * 【为什么重写onCreateView？】
     * 1. 统一ViewBinding创建：所有Fragment都用相同的方式创建View
     * 2. 模板方法模式：定义标准流程，子类只需实现具体细节
     * 3. 错误预防：避免子类忘记正确设置ViewBinding
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 记录Fragment创建（调试用）
        if (BuildConfig.DEBUG) {
            println("🏗️ Fragment创建View: ${this::class.simpleName}")
        }

        // 1. 获取ViewBinding实例（子类实现）
        _binding = getViewBinding(inflater, container)

        // 2. 返回根视图
        return binding.root
    }

    /**
     * View创建完成后的回调
     *
     * 🎯 关键方法：View创建完成后的初始化入口
     *
     * 【为什么使用onViewCreated？】
     * 1. 时机正确：此时View已经创建完成，可以安全访问View组件
     * 2. 生命周期保证：确保在Fragment的View可用时进行初始化
     * 3. 标准实践：Android官方推荐在此方法中进行View相关的初始化
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.DEBUG) {
            println("🎨 Fragment View创建完成: ${this::class.simpleName}")
        }

        // 按顺序初始化（模板方法模式）
        initView()      // 初始化视图
        initData()      // 初始化数据
        initListener()  // 初始化监听器
    }

    /**
     * Fragment销毁View时的清理
     *
     * 🎯 关键方法：防止内存泄漏的核心方法
     *
     * 【为什么必须重写onDestroyView？】
     * 1. 内存泄漏预防：Fragment可能比View存活更久，必须及时释放View引用
     * 2. ViewBinding清理：避免持有已销毁View的引用
     * 3. 资源释放：释放View相关的资源和监听器
     * 4. Fragment特性：Fragment可能会重新创建View，需要正确管理生命周期
     */
    override fun onDestroyView() {
        if (BuildConfig.DEBUG) {
            println("🗑️ Fragment销毁View: ${this::class.simpleName}")
        }

        // 清理ViewBinding引用，防止内存泄漏
        _binding = null

        if (BuildConfig.DEBUG) {
            println("✅ Fragment View清理完成: ${this::class.simpleName}")
        }

        super.onDestroyView()
    }

    /**
     * 抽象方法：子类必须实现
     * 为什么用抽象方法？
     * - 强制子类提供ViewBinding实例
     * - 编译时检查，避免运行时错误
     * - 类型安全：确保每个Fragment都有正确的ViewBinding
     */
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 开放方法：子类可以选择重写
     * 为什么用open？
     * - 提供默认实现（空实现）
     * - 子类按需重写，不强制
     * - 灵活性：不是所有Fragment都需要这些初始化步骤
     */
    protected open fun initView() {
        // 默认空实现，子类可以重写
        // 用于初始化UI组件，如设置RecyclerView的Adapter
        // 示例：setupRecyclerView(), configureToolbar()
    }

    protected open fun initData() {
        // 默认空实现，子类可以重写
        // 用于初始化数据，如请求网络数据、加载本地数据
        // 示例：loadUserData(), requestNetworkData()
    }

    protected open fun initListener() {
        // 默认空实现，子类可以重写
        // 用于设置点击监听器、观察LiveData等
        // 示例：setClickListeners(), observeViewModel()
    }

    /**
     * 工具方法：检查Fragment是否处于有效状态
     *
     * 【为什么需要这个方法？】
     * 1. 异步安全：在异步操作回调中检查Fragment状态
     * 2. 崩溃预防：避免在Fragment销毁后操作UI
     * 3. 最佳实践：提供统一的状态检查方法
     */
    protected fun isFragmentValid(): Boolean {
        return isAdded && !isDetached && !isRemoving && _binding != null
    }

    /**
     * 创建ViewModel实例的统一方法
     *
     * 🎯 设计目标：
     * 1. 统一ViewModel创建方式：所有Fragment使用相同的创建模式
     * 2. 生命周期管理：确保ViewModel与Fragment生命周期正确绑定
     * 3. 扩展性：为未来的依赖注入、工厂模式等预留接口
     * 4. 类型安全：使用泛型确保返回正确的ViewModel类型
     *
     * 【为什么这样设计ViewModel创建？】
     *
     * ✅ **统一管理的好处：**
     * - 一致性：所有Fragment都用相同方式创建ViewModel，避免代码重复
     * - 可维护性：如果需要修改ViewModel创建逻辑（如添加工厂），只需修改这里
     * - 扩展性：未来可以轻松集成Hilt、Koin等依赖注入框架
     * - 调试友好：统一的创建点便于调试和日志记录
     *
     * ✅ **生命周期绑定的重要性：**
     * - Fragment级别：ViewModel与Fragment生命周期绑定，Fragment销毁时ViewModel也会清理
     * - 配置变化：屏幕旋转等配置变化时，ViewModel会保持数据不丢失
     * - 内存安全：避免ViewModel持有已销毁Fragment的引用
     *
     * ✅ **与其他创建方式的对比：**
     * ```kotlin
     * // ❌ 直接创建 - 不推荐
     * val viewModel = MyViewModel()  // 无生命周期管理，配置变化时数据丢失
     *
     * // ❌ 每次都写ViewModelProvider - 代码重复
     * val viewModel = ViewModelProvider(this)[MyViewModel::class.java]
     *
     * // ✅ 使用BaseFragment统一方法 - 推荐
     * val viewModel = createMyViewModel(MyViewModel::class.java)
     * ```
     *
     * 【最佳调用时机】
     * 建议在以下时机调用：
     * 1. **initData()方法中** - 推荐，数据初始化阶段创建ViewModel
     * 2. **initListener()方法中** - 如果需要立即观察LiveData
     * 3. **lazy委托** - 延迟创建，首次使用时才初始化
     *
     * 【使用示例】
     * ```kotlin
     * class MyFragment : BaseFragment<MyBinding>() {
     *     // 方式1：在initData中创建（推荐）
     *     private lateinit var viewModel: MyViewModel
     *
     *     override fun initData() {
     *         super.initData()
     *         viewModel = createMyViewModel(MyViewModel::class.java)
     *         observeViewModel()
     *     }
     *
     *     // 方式2：使用lazy委托（也很好）
     *     private val viewModel by lazy {
     *         createMyViewModel(MyViewModel::class.java)
     *     }
     * }
     * ```
     *
     * @param T ViewModel的具体类型，必须继承自ViewModel
     * @param modelClass ViewModel的Class对象，用于ViewModelProvider创建实例
     * @return 创建的ViewModel实例，与Fragment生命周期绑定
     */
    protected open fun <T : ViewModel> createMyViewModel(modelClass: Class<T>): T {
        if (BuildConfig.DEBUG) {
            println("🏭 创建ViewModel: ${modelClass.simpleName} for ${this::class.simpleName}")
        }

        // 使用ViewModelFactory进行依赖注入
        return try {
            val factory = ViewModelFactory.getInstance(requireContext())

            if (BuildConfig.DEBUG) {
                println("✅ 成功使用ViewModelFactory创建ViewModel: ${modelClass.simpleName}")
            }

            ViewModelProvider(this, factory)[modelClass]
        } catch (e: Exception) {
            // 如果ViewModelFactory创建失败，使用默认方式（仅适用于无参数构造函数的ViewModel）
            if (BuildConfig.DEBUG) {
                println("⚠️ ViewModelFactory创建失败，尝试使用默认ViewModelProvider")
                println("   错误详情: ${e.javaClass.simpleName}: ${e.message}")
                println("   注意: 如果ViewModel需要依赖注入，请检查ViewModelFactory实现")
            }

            ViewModelProvider(this)[modelClass]
        }
    }

    /**
     * 工具方法：安全地执行UI操作
     *
     * 【使用场景】
     * 1. 网络请求回调中更新UI
     * 2. 异步任务完成后更新界面
     * 3. 定时器回调中的UI操作
     *
     * 【使用示例】
     * safeExecute {
     *     binding.textView.text = "更新文本"
     *     binding.progressBar.visibility = View.GONE
     * }
     */
    protected inline fun safeExecute(action: () -> Unit) {
        if (isFragmentValid()) {
            action()
        } else if (BuildConfig.DEBUG) {
            println("⚠️ Fragment状态无效，跳过UI操作: ${this::class.simpleName}")
        }
    }
}