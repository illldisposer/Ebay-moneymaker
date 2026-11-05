package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BotSettingDao
import com.example.data.local.dao.GapDao
import com.example.data.local.dao.MatchDao
import com.example.data.local.dao.SystemLogDao
import com.example.data.local.entity.BotSettingEntity
import com.example.data.local.entity.GapEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.SystemLogEntity

@Database(
    entities = [
        MatchEntity::class,
        SystemLogEntity::class,
        BotSettingEntity::class,
        GapEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun systemLogDao(): SystemLogDao
    abstract fun botSettingDao(): BotSettingDao
    abstract fun gapDao(): GapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Neue Tabelle für Marktlücken. Bestehende Treffer und Einstellungen bleiben erhalten. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `gaps` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ebayItemId` TEXT NOT NULL, " +
                        "`keyword` TEXT NOT NULL, " +
                        "`ebayTitle` TEXT NOT NULL, " +
                        "`ebayPrice` REAL NOT NULL, " +
                        "`ebayShipping` REAL NOT NULL, " +
                        "`ebayCurrency` TEXT NOT NULL, " +
                        "`ebayImageUrl` TEXT NOT NULL, " +
                        "`ebayItemUrl` TEXT NOT NULL, " +
                        "`ebayCondition` TEXT NOT NULL, " +
                        "`ebaySoldEstimate` INTEGER, " +
                        "`gtin` TEXT, " +
                        "`amazonQuery` TEXT NOT NULL, " +
                        "`amazonStatus` TEXT NOT NULL, " +
                        "`foundAt` INTEGER NOT NULL, " +
                        "`decidedAt` INTEGER)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_gaps_ebayItemId` ON `gaps` (`ebayItemId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebay_bot_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
