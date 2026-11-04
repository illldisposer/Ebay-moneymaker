package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE status IN ('AUTO', 'KEPT') ORDER BY (ebayPrice + ebayShipping - aliPrice) DESC")
    fun getScannerMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'REVIEW' ORDER BY confidence DESC, updatedAt DESC")
    fun getReviewQueue(): Flow<List<MatchEntity>>

    @Query("SELECT COUNT(*) FROM matches WHERE status = 'REVIEW'")
    fun getReviewCount(): Flow<Int>

    @Query("SELECT * FROM matches WHERE pairKey = :pairKey LIMIT 1")
    suspend fun getByPairKey(pairKey: String): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE status IN ('KEPT', 'REJECTED') AND decidedAt IS NOT NULL")
    suspend fun getDecidedMatches(): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(match: MatchEntity): Long

    @Update
    suspend fun update(match: MatchEntity)

    @Query("UPDATE matches SET status = :status, decidedAt = :decidedAt, updatedAt = :decidedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, decidedAt: Long)

    @Query("UPDATE matches SET status = 'AUTO', updatedAt = :now WHERE status = 'REVIEW' AND confidence >= :threshold")
    suspend fun promoteReviewToAuto(threshold: Int, now: Long): Int

    @Query("UPDATE matches SET status = 'REVIEW', updatedAt = :now WHERE status = 'AUTO' AND confidence < :threshold")
    suspend fun demoteAutoToReview(threshold: Int, now: Long): Int

    /** Leert offene Ergebnisse, behält aber deine Entscheidungen (Behalten/Raus). */
    @Query("DELETE FROM matches WHERE status IN ('AUTO', 'REVIEW')")
    suspend fun clearUndecided(): Int
}
