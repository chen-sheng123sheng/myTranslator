package com.example.mytranslator.data.mapper

import com.example.mytranslator.data.local.entity.TranslationHistoryEntity
import com.example.mytranslator.domain.model.TranslationHistory

/**
 * 翻译历史记录数据映射器
 *
 * 🎯 设计目的：
 * 1. 在Entity（数据层）和Domain Model（领域层）之间进行转换
 * 2. 隔离不同层次的数据模型，保持架构清晰
 * 3. 处理数据格式的差异和兼容性
 * 4. 提供类型安全的数据转换
 *
 * 🏗️ 映射器设计原则：
 * - 单一职责：只负责数据转换
 * - 双向转换：支持Entity ↔ Domain的双向映射
 * - 数据完整性：确保转换过程中数据不丢失
 * - 性能优化：避免不必要的对象创建
 *
 * 📱 使用场景：
 * - Repository层进行数据转换
 * - 数据库操作的输入输出转换
 * - 不同层次模型的适配
 *
 * 🎓 学习要点：
 * 数据映射的关键概念：
 * 1. 层次分离 - 不同层使用不同的数据模型
 * 2. 数据转换 - 处理字段名称和类型的差异
 * 3. 默认值处理 - 为新增字段提供合理的默认值
 * 4. 向后兼容 - 处理数据模型的演进
 */
object TranslationHistoryMapper {

    /**
     * 将Domain模型转换为Entity
     *
     * 🔧 转换说明：
     * 将业务层的TranslationHistory转换为数据库层的TranslationHistoryEntity
     * 
     * 转换要点：
     * - 保持所有字段的完整映射
     * - 处理可选字段的null值
     * - 确保数据类型的正确转换
     *
     * @param domain 领域模型对象
     * @return 数据库实体对象
     */
    fun toEntity(domain: TranslationHistory): TranslationHistoryEntity {
        return TranslationHistoryEntity(
            id = domain.id,
            originalText = domain.originalText,
            translatedText = domain.translatedText,
            sourceLanguageCode = domain.sourceLanguageCode,
            targetLanguageCode = domain.targetLanguageCode,
            sourceLanguageName = domain.sourceLanguageName,
            targetLanguageName = domain.targetLanguageName,
            timestamp = domain.timestamp,
            isFavorite = domain.isFavorite,
            translationProvider = domain.translationProvider
        )
    }

    /**
     * 将Entity转换为Domain模型
     *
     * 🔧 转换说明：
     * 将数据库层的TranslationHistoryEntity转换为业务层的TranslationHistory
     * 
     * 转换要点：
     * - 为新增的Domain字段提供默认值
     * - 处理Entity中可能缺失的字段
     * - 保持数据的业务语义
     *
     * @param entity 数据库实体对象
     * @return 领域模型对象
     */
    fun toDomain(entity: TranslationHistoryEntity): TranslationHistory {
        return TranslationHistory(
            id = entity.id,
            originalText = entity.originalText,
            translatedText = entity.translatedText,
            sourceLanguageCode = entity.sourceLanguageCode,
            targetLanguageCode = entity.targetLanguageCode,
            sourceLanguageName = entity.sourceLanguageName,
            targetLanguageName = entity.targetLanguageName,
            timestamp = entity.timestamp,
            isFavorite = entity.isFavorite,
            translationProvider = entity.translationProvider,
            
            // Domain模型中的扩展字段，使用默认值
            qualityScore = null, // Entity中暂未存储质量评分
            usageCount = 0, // Entity中暂未存储使用次数
            lastAccessTime = entity.timestamp, // 使用创建时间作为默认访问时间
            tags = emptyList(), // Entity中暂未存储标签
            notes = null // Entity中暂未存储备注
        )
    }

    /**
     * 批量转换Entity列表为Domain列表
     *
     * @param entities Entity列表
     * @return Domain模型列表
     */
    fun toDomainList(entities: List<TranslationHistoryEntity>): List<TranslationHistory> {
        return entities.map { toDomain(it) }
    }

    /**
     * 批量转换Domain列表为Entity列表
     *
     * @param domains Domain模型列表
     * @return Entity列表
     */
    fun toEntityList(domains: List<TranslationHistory>): List<TranslationHistoryEntity> {
        return domains.map { toEntity(it) }
    }

    /**
     * 更新Entity的部分字段
     *
     * 🔧 使用场景：
     * 当只需要更新Entity的某些字段时，避免完整的对象转换
     * 
     * @param entity 原始Entity
     * @param domain 包含更新数据的Domain模型
     * @return 更新后的Entity
     */
    fun updateEntity(entity: TranslationHistoryEntity, domain: TranslationHistory): TranslationHistoryEntity {
        return entity.copy(
            originalText = domain.originalText,
            translatedText = domain.translatedText,
            sourceLanguageCode = domain.sourceLanguageCode,
            targetLanguageCode = domain.targetLanguageCode,
            sourceLanguageName = domain.sourceLanguageName,
            targetLanguageName = domain.targetLanguageName,
            isFavorite = domain.isFavorite,
            translationProvider = domain.translationProvider
            // 注意：不更新id和timestamp，保持原有值
        )
    }

    /**
     * 创建用于测试的映射
     *
     * 🔧 测试支持：
     * 提供便于测试的数据转换方法
     */
    fun createTestEntity(): TranslationHistoryEntity {
        return toEntity(TranslationHistory.createSample())
    }

    /**
     * 验证映射的完整性
     *
     * 🔧 数据验证：
     * 检查Entity和Domain之间的映射是否保持了数据完整性
     * 
     * @param entity 原始Entity
     * @param domain 转换后的Domain
     * @return 如果映射正确返回true
     */
    fun validateMapping(entity: TranslationHistoryEntity, domain: TranslationHistory): Boolean {
        return entity.id == domain.id &&
                entity.originalText == domain.originalText &&
                entity.translatedText == domain.translatedText &&
                entity.sourceLanguageCode == domain.sourceLanguageCode &&
                entity.targetLanguageCode == domain.targetLanguageCode &&
                entity.sourceLanguageName == domain.sourceLanguageName &&
                entity.targetLanguageName == domain.targetLanguageName &&
                entity.timestamp == domain.timestamp &&
                entity.isFavorite == domain.isFavorite &&
                entity.translationProvider == domain.translationProvider
    }

    /**
     * 获取映射统计信息
     *
     * 🔧 调试支持：
     * 提供映射过程的统计信息，用于性能分析和调试
     */
    fun getMappingInfo(): String {
        return """
            TranslationHistoryMapper 映射信息:
            - Entity字段数: ${TranslationHistoryEntity::class.java.declaredFields.size}
            - Domain字段数: ${TranslationHistory::class.java.declaredFields.size}
            - 支持双向转换: ✅
            - 批量转换支持: ✅
            - 数据验证支持: ✅
        """.trimIndent()
    }
}
