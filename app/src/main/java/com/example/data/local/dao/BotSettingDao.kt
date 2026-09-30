package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BotSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BotSettingDao {
    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<BotSettingEntity?>

    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): BotSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: BotSettingEntity)
}
