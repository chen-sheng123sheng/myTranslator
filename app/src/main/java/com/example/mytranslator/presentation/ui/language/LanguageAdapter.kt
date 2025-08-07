package com.example.mytranslator.presentation.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mytranslator.common.utils.LanguageLocalizer
import com.example.mytranslator.databinding.ItemLanguageBinding
import com.example.mytranslator.domain.model.Language

/**
 * 语言列表适配器
 *
 * 🎯 设计目的：
 * 1. 高效显示语言列表
 * 2. 支持选中状态指示
 * 3. 优化滚动性能
 * 4. 响应用户点击事件
 *
 * 🔧 技术特性：
 * - 使用 ListAdapter 和 DiffUtil 优化性能
 * - ViewBinding 管理视图
 * - 支持当前选中语言高亮
 * - 点击事件回调
 *
 * 🎨 UI特性：
 * - 显示语言名称和原生名称
 * - 选中状态图标指示
 * - Material Design 风格
 * - 适配深色模式
 *
 * 🎓 学习要点：
 * RecyclerView.Adapter 最佳实践：
 * 1. 使用 ListAdapter 和 DiffUtil
 * 2. ViewBinding 替代 findViewById
 * 3. 合理的点击事件处理
 * 4. 状态管理和UI更新
 */
class LanguageAdapter(
    private var currentSelectedLanguage: Language? = null,
    private val onLanguageClick: (Language) -> Unit
) : ListAdapter<Language, LanguageAdapter.LanguageViewHolder>(LanguageDiffCallback()) {

    /**
     * 更新语言列表
     */
    fun updateLanguages(languages: List<Language>) {
        submitList(languages)
    }

    /**
     * 更新当前选中的语言
     */
    fun updateSelectedLanguage(language: Language?) {
        val oldSelected = currentSelectedLanguage
        currentSelectedLanguage = language
        
        // 只刷新相关的item，提高性能
        if (oldSelected != null) {
            val oldIndex = currentList.indexOf(oldSelected)
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex)
            }
        }
        
        if (language != null) {
            val newIndex = currentList.indexOf(language)
            if (newIndex != -1) {
                notifyItemChanged(newIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 语言项ViewHolder
     */
    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(language: Language) {
            binding.apply {
                // 获取本地化的语言名称
                val localizedName = LanguageLocalizer.getLocalizedLanguageName(root.context, language)

                // 设置语言信息 - 使用本地化文本
                tvLanguageName.text = localizedName
                tvLanguageNativeName.text = language.displayName  // 保留原生名称用于识别
                tvLanguageCode.text = language.code.uppercase()

                // 设置选中状态
                val isSelected = language == currentSelectedLanguage
                ivSelected.isVisible = isSelected
                root.isSelected = isSelected

                // 设置点击事件
                root.setOnClickListener {
                    onLanguageClick(language)
                }

                // 设置内容描述（无障碍支持）- 使用本地化文本
                root.contentDescription = "$localizedName (${language.displayName})"
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class LanguageDiffCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem == newItem
        }
    }
}
