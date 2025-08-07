package com.example.mytranslator.presentation.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mytranslator.R
import com.example.mytranslator.databinding.ItemHistoryHeaderBinding
import com.example.mytranslator.databinding.ItemTranslationHistoryBinding
import com.example.mytranslator.domain.model.TranslationHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * 翻译历史记录RecyclerView适配器
 *
 * 🎯 设计目的：
 * 1. 高效展示翻译历史记录列表
 * 2. 支持分组显示和头部标题
 * 3. 处理用户交互（点击、长按、收藏等）
 * 4. 支持选择模式和批量操作
 *
 * 🏗️ 适配器设计：
 * - ListAdapter：自动处理数据变更和动画
 * - DiffUtil：高效的列表差异计算
 * - ViewBinding：类型安全的视图绑定
 * - 多视图类型：支持头部和内容项
 *
 * 📱 功能特性：
 * - 分组显示历史记录
 * - 收藏状态切换
 * - 选择模式支持
 * - 流畅的动画效果
 *
 * 🎓 学习要点：
 * RecyclerView适配器的现代化实现：
 * 1. ListAdapter - 替代传统的RecyclerView.Adapter
 * 2. DiffUtil - 自动计算列表差异
 * 3. ViewBinding - 替代findViewById
 * 4. 多视图类型 - 支持复杂的列表布局
 */
class TranslationHistoryAdapter(
    private val onItemClick: (TranslationHistory) -> Unit,
    private val onFavoriteClick: (TranslationHistory) -> Unit,
    private val onDeleteClick: (TranslationHistory) -> Unit,
    private val onItemLongClick: (TranslationHistory) -> Boolean
) : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSLATION = 1
    }

    // ===== 选择模式状态 =====
    
    private var isSelectionMode = false
    private var selectedItems = setOf<String>()

    // ===== ViewHolder类型 =====

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryListItem.Header -> VIEW_TYPE_HEADER
            is HistoryListItem.Translation -> VIEW_TYPE_TRANSLATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemHistoryHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_TRANSLATION -> {
                val binding = ItemTranslationHistoryBinding.inflate(inflater, parent, false)
                TranslationViewHolder(binding)
            }
            else -> throw IllegalArgumentException("未知的视图类型: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is HistoryListItem.Translation -> {
                (holder as TranslationViewHolder).bind(item.translation)
            }
        }
    }

    // ===== ViewHolder实现 =====

    /**
     * 头部ViewHolder
     */
    inner class HeaderViewHolder(
        private val binding: ItemHistoryHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(header: HistoryListItem.Header) {
            binding.textHeaderTitle.text = header.title
            binding.textHeaderCount.text = "${header.count} 条记录"
        }
    }

    /**
     * 翻译记录ViewHolder
     */
    inner class TranslationViewHolder(
        private val binding: ItemTranslationHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(translation: TranslationHistory) {
            // 绑定基本信息
            binding.textOriginal.text = translation.originalText
            binding.textTranslated.text = translation.translatedText
            binding.textLanguagePair.text = "${translation.sourceLanguageName} → ${translation.targetLanguageName}"
            binding.textTimestamp.text = formatTimestamp(translation.timestamp)
            binding.textProvider.text = translation.translationProvider.uppercase()
            
            // 绑定收藏状态
            updateFavoriteButton(translation.isFavorite)
            
            // 绑定选择状态
            updateSelectionState(translation.id)
            
            // 绑定使用次数
            if (translation.usageCount > 0) {
                binding.textUsageCount.visibility = View.VISIBLE
                binding.textUsageCount.text = "使用 ${translation.usageCount} 次"
            } else {
                binding.textUsageCount.visibility = View.GONE
            }
            
            // 绑定质量分数
            translation.qualityScore?.let { score ->
                binding.ratingQuality.visibility = View.VISIBLE
                binding.ratingQuality.rating = (score * 5).toFloat()
            } ?: run {
                binding.ratingQuality.visibility = View.GONE
            }
            
            // 设置点击监听器
            setupClickListeners(translation)
        }

        /**
         * 更新收藏按钮状态
         */
        private fun updateFavoriteButton(isFavorite: Boolean) {
            binding.buttonFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite_filled 
                else R.drawable.ic_favorite_outline
            )
            
            binding.buttonFavorite.setColorFilter(
                binding.root.context.getColor(
                    if (isFavorite) R.color.favorite_color 
                    else R.color.icon_color_secondary
                )
            )
        }

        /**
         * 更新选择状态
         */
        private fun updateSelectionState(translationId: String) {
            val isSelected = selectedItems.contains(translationId)
            
            // 显示/隐藏选择框
            binding.checkboxSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.checkboxSelect.isChecked = isSelected
            
            // 更新背景
            binding.root.isSelected = isSelected
            binding.cardView.strokeWidth = if (isSelected) 2 else 0
        }

        /**
         * 设置点击监听器
         */
        private fun setupClickListeners(translation: TranslationHistory) {
            // 整体点击
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(translation.id)
                } else {
                    onItemClick(translation)
                }
            }
            
            // 长按
            binding.root.setOnLongClickListener {
                onItemLongClick(translation)
            }
            
            // 收藏按钮
            binding.buttonFavorite.setOnClickListener {
                onFavoriteClick(translation)
            }
            
            // 删除按钮
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(translation)
            }
            
            // 选择框
            binding.checkboxSelect.setOnClickListener {
                toggleSelection(translation.id)
            }
        }

        /**
         * 切换选择状态
         */
        private fun toggleSelection(translationId: String) {
            // 这个方法会触发适配器的选择状态更新
            // 实际的状态管理由ViewModel处理
        }

        /**
         * 格式化时间戳
         */
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} 分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} 小时前"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} 天前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    // ===== 公共方法 =====

    /**
     * 更新选择模式
     */
    fun updateSelectionMode(isSelectionMode: Boolean, selectedItems: Set<String>) {
        this.isSelectionMode = isSelectionMode
        this.selectedItems = selectedItems
        notifyDataSetChanged() // 刷新所有项目以更新选择状态
    }

    /**
     * 获取所有翻译记录ID
     */
    fun getAllTranslationIds(): List<String> {
        return currentList.filterIsInstance<HistoryListItem.Translation>()
            .map { it.translation.id }
    }

    /**
     * 获取选中的翻译记录
     */
    fun getSelectedTranslations(): List<TranslationHistory> {
        return currentList.filterIsInstance<HistoryListItem.Translation>()
            .filter { selectedItems.contains(it.translation.id) }
            .map { it.translation }
    }
}

/**
 * 历史记录列表项密封类
 *
 * 🎯 设计说明：
 * 支持多种类型的列表项，包括头部和翻译记录
 */
sealed class HistoryListItem {
    /**
     * 头部项
     */
    data class Header(
        val title: String,
        val count: Int
    ) : HistoryListItem()
    
    /**
     * 翻译记录项
     */
    data class Translation(
        val translation: TranslationHistory
    ) : HistoryListItem()
}

/**
 * DiffUtil回调
 *
 * 🎯 设计说明：
 * 高效计算列表差异，支持流畅的动画效果
 */
class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryListItem>() {
    
    override fun areItemsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
        return when {
            oldItem is HistoryListItem.Header && newItem is HistoryListItem.Header -> {
                oldItem.title == newItem.title
            }
            oldItem is HistoryListItem.Translation && newItem is HistoryListItem.Translation -> {
                oldItem.translation.id == newItem.translation.id
            }
            else -> false
        }
    }
    
    override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
        return when {
            oldItem is HistoryListItem.Header && newItem is HistoryListItem.Header -> {
                oldItem == newItem
            }
            oldItem is HistoryListItem.Translation && newItem is HistoryListItem.Translation -> {
                oldItem.translation == newItem.translation
            }
            else -> false
        }
    }
}
