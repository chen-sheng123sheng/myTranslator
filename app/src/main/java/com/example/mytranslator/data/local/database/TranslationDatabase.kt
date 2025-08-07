package com.example.mytranslator.data.local.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mytranslator.data.local.converter.DatabaseConverters
import com.example.mytranslator.data.local.dao.TranslationHistoryDao
import com.example.mytranslator.data.local.entity.TranslationHistoryEntity

/**
 * 🏠 translationHistory分支 - Room数据库配置类
 * 
 * Database类是Room数据库的核心配置中心，负责：
 * - 注册所有Entity（数据表）
 * - 提供DAO访问接口
 * - 管理数据库版本和迁移
 * - 实现单例模式确保全局唯一实例
 * 
 * 🎯 设计目标：
 * - 类型安全的数据库访问
 * - 优雅的版本升级机制
 * - 高性能的数据库操作
 * - 便于测试和维护
 */
@Database(
    entities = [
        TranslationHistoryEntity::class  // 📋 注册翻译历史记录表
        // 💡 未来可以在这里添加更多Entity，如：
        // UserPreferencesEntity::class,
        // TranslationStatisticsEntity::class
    ],
    version = 1,  // 🔢 数据库版本号，每次结构变化都要递增
    exportSchema = false  // 📊 暂时关闭Schema导出，简化配置
)
@TypeConverters(DatabaseConverters::class)  // 🔄 注册类型转换器
abstract class TranslationDatabase : RoomDatabase() {
    
    // 🎯 ===== DAO访问方法 =====
    
    /**
     * 获取翻译历史记录DAO
     * 
     * abstract方法说明：
     * - Room会在编译时自动生成实现
     * - 返回DAO接口的具体实现类
     * - 每次调用返回同一个实例（单例）
     */
    abstract fun translationHistoryDao(): TranslationHistoryDao
    
    // 💡 未来扩展：可以添加更多DAO
    // abstract fun userPreferencesDao(): UserPreferencesDao
    // abstract fun statisticsDao(): StatisticsDao
    
    companion object {
        
        // 🏷️ 数据库名称
        private const val DATABASE_NAME = "translation_database"
        
        // 🔒 单例实例，使用@Volatile确保线程安全
        @Volatile
        private var INSTANCE: TranslationDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         * 
         * 单例模式的重要性：
         * - 避免多个数据库连接造成的资源浪费
         * - 确保数据一致性
         * - 提升性能，减少初始化开销
         * 
         * 双重检查锁定（Double-Check Locking）：
         * - 第一次检查：避免不必要的同步
         * - synchronized：确保线程安全
         * - 第二次检查：防止多线程重复创建
         */
        fun getDatabase(context: Context): TranslationDatabase {
            // 第一次检查：如果实例已存在，直接返回
            return INSTANCE ?: synchronized(this) {
                // 第二次检查：在同步块内再次检查
                val instance = INSTANCE ?: buildDatabase(context).also { 
                    INSTANCE = it 
                }
                instance
            }
        }
        
        /**
         * 构建数据库实例
         * 
         * Room.databaseBuilder()配置说明：
         * - context: 应用上下文
         * - TranslationDatabase::class.java: 数据库类
         * - DATABASE_NAME: 数据库文件名
         */
        private fun buildDatabase(context: Context): TranslationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,  // 使用applicationContext避免内存泄漏
                TranslationDatabase::class.java,
                DATABASE_NAME
            )
                // 🔄 添加数据库迁移策略
                .addMigrations(
                    // MIGRATION_1_2,  // 未来版本升级时添加
                    // MIGRATION_2_3
                )
                // 🚨 开发阶段配置（生产环境需要移除）
                // .fallbackToDestructiveMigration()  // 迁移失败时重建数据库
                
                // 🎯 生产环境配置
                .build()
        }
        
        // 🔄 ===== 数据库迁移脚本 =====
        
        /**
         * 版本1到版本2的迁移示例
         * 
         * 迁移脚本的作用：
         * - 保留用户数据
         * - 安全地修改表结构
         * - 处理数据类型变更
         * 
         * 常见迁移操作：
         * - ALTER TABLE ADD COLUMN: 添加新列
         * - CREATE TABLE: 创建新表
         * - CREATE INDEX: 添加索引
         * - UPDATE: 数据格式转换
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 示例：添加新列
                // database.execSQL(
                //     "ALTER TABLE translation_history ADD COLUMN user_rating INTEGER DEFAULT 0 NOT NULL"
                // )
                
                // 示例：创建新索引
                // database.execSQL(
                //     "CREATE INDEX index_translation_history_user_rating ON translation_history(user_rating)"
                // )
            }
        }
        
        /**
         * 版本2到版本3的迁移示例
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 示例：创建新表
                // database.execSQL("""
                //     CREATE TABLE user_preferences (
                //         id INTEGER PRIMARY KEY NOT NULL,
                //         theme_mode TEXT NOT NULL,
                //         auto_save_history INTEGER NOT NULL DEFAULT 1
                //     )
                // """)
            }
        }
        
        // 🧪 ===== 测试支持方法 =====
        
        /**
         * 清除数据库实例（仅用于测试）
         * 
         * 测试场景：
         * - 单元测试需要干净的数据库环境
         * - 集成测试需要重置数据库状态
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
        
        /**
         * 创建内存数据库（仅用于测试）
         * 
         * 内存数据库特点：
         * - 数据存储在内存中，测试结束后自动清除
         * - 速度快，适合单元测试
         * - 不会影响真实的数据库文件
         */
        @androidx.annotation.VisibleForTesting
        fun createInMemoryDatabase(context: Context): TranslationDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                TranslationDatabase::class.java
            )
                .allowMainThreadQueries()  // 测试时允许主线程查询
                .build()
        }
    }
}

/**
 * 🎓 Database类设计学习要点：
 * 
 * 1. @Database注解配置：
 *    - entities: 注册所有Entity类
 *    - version: 数据库版本号，结构变化时递增
 *    - exportSchema: 是否导出Schema文件
 * 
 * 2. 单例模式实现：
 *    - @Volatile: 确保多线程可见性
 *    - synchronized: 线程安全的实例创建
 *    - 双重检查锁定: 性能优化
 * 
 * 3. 数据库迁移策略：
 *    - Migration类: 定义版本间的升级脚本
 *    - addMigrations(): 注册迁移脚本
 *    - fallbackToDestructiveMigration(): 迁移失败时的回退策略
 * 
 * 4. 版本管理最佳实践：
 *    - 每次表结构变化都要增加版本号
 *    - 编写详细的迁移脚本保留用户数据
 *    - 测试迁移脚本确保数据完整性
 *    - 考虑向后兼容性
 * 
 * 5. 性能优化考虑：
 *    - 使用applicationContext避免内存泄漏
 *    - 单例模式减少初始化开销
 *    - 合理的索引设计
 *    - 异步操作避免阻塞UI
 * 
 * 6. 测试支持：
 *    - 内存数据库用于单元测试
 *    - clearInstance()用于测试环境重置
 *    - @VisibleForTesting注解标记测试方法
 */
