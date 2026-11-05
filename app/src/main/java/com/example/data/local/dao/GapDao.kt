package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GapDao {
    /** Sortierung: Verkaufsschätzung absteigend (Angaben zuerst), danach günstigster Preis. */
    @Query("SELECT * FROM gaps ORDER BY (ebaySoldEstimate IS NULL), ebaySoldEstimate DESC, ebayPrice ASC")
    fun observeAll(): Flow<List<GapEntity>>

    @Query("SELECT * FROM gaps WHERE ebayItemId = :ebayItemId LIMIT 1")
    suspend fun getByEbayItemId(ebayItemId: String): GapEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(gap: GapEntity): Long

    @Update
    suspend fun update(gap: GapEntity)

    @Query("UPDATE gaps SET amazonStatus = :status, decidedAt = :decidedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, decidedAt: Long)

    @Query("DELETE FROM gaps WHERE amazonStatus = 'OPEN'")
    suspend fun deleteOpen(): Int
}
