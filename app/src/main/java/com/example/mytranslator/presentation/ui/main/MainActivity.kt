package com.example.mytranslator.presentation.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.mytranslator.BuildConfig
import com.example.mytranslator.R
import com.example.mytranslator.common.base.BaseActivity
import com.example.mytranslator.databinding.ActivityMainBinding

import com.example.mytranslator.presentation.ui.translation.text.TextTranslationFragment

/**
 * 主界面Activity
 *
 * 设计思想：
 * 1. Fragment容器：管理不同翻译功能的Fragment
 * 2. 底部导航：提供功能切换入口
 * 3. 状态管理：记住用户当前选择的功能
 *
 * 为什么使用Fragment？
 * - 模块化：每个翻译功能独立开发和维护
 * - 内存优化：按需加载，不用的Fragment可以销毁
 * - 用户体验：快速切换，保持状态
 */

class MainActivity : BaseActivity<ActivityMainBinding>() {

    // 当前显示的Fragment类型
    private enum class FragmentType {
        TEXT_TRANSLATION,
        VOICE_TRANSLATION,
        CAMERA_TRANSLATION
    }

    private var currentFragmentType = FragmentType.TEXT_TRANSLATION
    /**
     * 🎯 BaseActivity模板方法：获取ViewBinding实例
     *
     * 这是BaseActivity要求子类实现的抽象方法
     */
    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    /**
     * 🎯 BaseActivity模板方法：初始化视图
     *
     * 在这里进行视图相关的初始化工作
     */
    override fun initView() {
        // 启用边到边显示
        enableEdgeToEdge()

        // 设置系统栏适配
        setupSystemBars()
    }

    /**
     * 🎯 BaseActivity模板方法：初始化数据
     *
     * 在这里进行数据相关的初始化工作
     */
    override fun initData() {
        // 默认显示文本翻译Fragment
        showTextTranslationFragment()

        // 设置初始选中状态
        updateBottomNavigationState(FragmentType.TEXT_TRANSLATION)
    }

    /**
     * 🎯 BaseActivity模板方法：初始化监听器
     *
     * 在这里设置各种点击监听器
     */
    override fun initListener() {
        setupBottomNavigation()
    }

    /**
     * 设置系统栏适配
     */
    private fun setupSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }



    /**
     * 设置底部导航点击监听
     */
    private fun setupBottomNavigation() {
        // 文本翻译
        binding.btnTextTranslation.setOnClickListener {
            if (currentFragmentType != FragmentType.TEXT_TRANSLATION) {
                showTextTranslationFragment()
                updateBottomNavigationState(FragmentType.TEXT_TRANSLATION)
            }
        }

        // 语音翻译（暂未实现）
        binding.btnVoiceTranslation.setOnClickListener {
            // TODO: 实现语音翻译功能
            showComingSoonMessage("语音翻译功能即将上线")
        }

        // 拍照翻译（暂未实现）
        binding.btnCameraTranslation.setOnClickListener {
            // TODO: 实现拍照翻译功能
            showComingSoonMessage("拍照翻译功能即将上线")
        }
    }

    /**
     * 显示文本翻译Fragment
     */
    private fun showTextTranslationFragment() {
        // 检查是否已经是当前Fragment，避免重复创建
        if (currentFragmentType != FragmentType.TEXT_TRANSLATION) {
            val fragment = TextTranslationFragment()
            replaceFragment(fragment)
            currentFragmentType = FragmentType.TEXT_TRANSLATION
        } else {
            // 如果是初始化时调用，且容器为空，则创建Fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment == null) {
                val fragment = TextTranslationFragment()
                replaceFragment(fragment)
            }
        }
    }

    /**
     * 替换Fragment的通用方法
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * 更新底部导航的选中状态
     */
    private fun updateBottomNavigationState(selectedType: FragmentType) {
        // 重置所有按钮状态
        resetBottomNavigationState()

        // 设置选中状态
        when (selectedType) {
            FragmentType.TEXT_TRANSLATION -> {
                setNavigationItemSelected(binding.btnTextTranslation, true)
            }
            FragmentType.VOICE_TRANSLATION -> {
                setNavigationItemSelected(binding.btnVoiceTranslation, true)
            }
            FragmentType.CAMERA_TRANSLATION -> {
                setNavigationItemSelected(binding.btnCameraTranslation, true)
            }
        }
    }

    /**
     * 重置所有底部导航按钮状态
     */
    private fun resetBottomNavigationState() {
        setNavigationItemSelected(binding.btnTextTranslation, false)
        setNavigationItemSelected(binding.btnVoiceTranslation, false)
        setNavigationItemSelected(binding.btnCameraTranslation, false)
    }

    /**
     * 设置导航项的选中状态
     */
    private fun setNavigationItemSelected(navigationItem: android.view.View, selected: Boolean) {
        // 简化实现：直接设置背景透明度来表示选中状态
        navigationItem.alpha = if (selected) 1.0f else 0.6f
    }

    /**
     * 显示即将上线提示
     */
    private fun showComingSoonMessage(message: String) {
        Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

}