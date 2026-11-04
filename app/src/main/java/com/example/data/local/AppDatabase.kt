package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BotSettingDao
import com.example.data.local.dao.MatchDao
import com.example.data.local.dao.SystemLogDao
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.SystemLogEntity

@Database(
    entities = [
        MatchEntity::class,
        SystemLogEntity::class,
        BotSettingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun systemLogDao(): SystemLogDao
    abstract fun botSettingDao(): BotSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebay_bot_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
