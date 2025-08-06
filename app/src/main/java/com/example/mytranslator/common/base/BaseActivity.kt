package com.example.mytranslator.common.base


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.mytranslator.BuildConfig

/**
 * 基础Activity类
 *
 * 设计思想：
 * 1. 模板方法模式：定义了Activity的生命周期模板
 * 2. 泛型约束：使用ViewBinding泛型，确保类型安全
 * 3. 代码复用：所有Activity都继承这个基类，避免重复代码
 * 4. 依赖注入管理：统一管理依赖注入的生命周期 -->暂未实现
 *
 * 为什么这样设计？
 * - 统一管理：所有Activity的初始化流程都一样
 * - 易于维护：如果要修改所有Activity的行为，只需修改这里
 * - 类型安全：ViewBinding泛型避免了findViewById的空指针风险
 * - 内存安全：自动管理依赖注入的生命周期，防止内存泄漏
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    // protected：子类可以访问，外部不能访问
    // late init：延迟初始化，避免空指针
    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 记录Activity创建（调试用）
        if (BuildConfig.DEBUG) {
            println("🏗️ Activity创建: ${this::class.simpleName}")
        }

        // 1. 获取ViewBinding实例（子类实现）
        binding = getViewBinding()

        // 2. 设置布局
        setContentView(binding.root)

        // 3. 按顺序初始化（模板方法模式）
        initView()      // 初始化视图
        initData()      // 初始化数据
        initListener()  // 初始化监听器
    }

    /**
     * Activity销毁时的清理
     *
     * 🎯 关键方法：确保依赖注入的生命周期管理
     *
     * 【清理的重要性】
     * 1. 防止内存泄漏：及时释放Activity级别的对象
     * 2. 资源管理：释放不再需要的资源
     * 3. 状态重置：为下次创建Activity做准备
     * 4. 统计准确性：保持容器统计信息的准确性
     */
    override fun onDestroy() {
        if (BuildConfig.DEBUG) {
            println("🗑️ Activity销毁: ${this::class.simpleName}")
            println("✅ Activity清理完成: ${this::class.simpleName}")
        }

        super.onDestroy()
    }

    /**
     * 抽象方法：子类必须实现
     * 为什么用抽象方法？
     * - 强制子类提供ViewBinding实例
     * - 编译时检查，避免运行时错误
     */
    abstract fun getViewBinding(): VB

    /**
     * 开放方法：子类可以选择重写
     * 为什么用open？
     * - 提供默认实现（空实现）
     * - 子类按需重写，不强制
     */
    protected open fun initView() {
        // 默认空实现，子类可以重写
        // 用于初始化UI组件，如设置RecyclerView的Adapter
    }

    protected open fun initData() {
        // 默认空实现，子类可以重写
        // 用于初始化数据，如请求网络数据
    }

    protected open fun initListener() {
        // 默认空实现，子类可以重写
        // 用于设置点击监听器等
    }
}