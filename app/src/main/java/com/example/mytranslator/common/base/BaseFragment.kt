package com.example.mytranslator.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.mytranslator.BuildConfig

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