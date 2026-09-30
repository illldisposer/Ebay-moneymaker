package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    fun getAllListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id")
    fun getListingById(id: Long): Flow<ListingEntity?>

    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getListingByIdOnce(id: Long): ListingEntity?

    @Query("SELECT * FROM listings WHERE originalItemId = :originalId LIMIT 1")
    suspend fun getListingByOriginalId(originalId: String): ListingEntity?

    @Query("SELECT * FROM listings WHERE status = 'ACTIVE' ORDER BY lastSyncedAt ASC")
    suspend fun getActiveListingsSyncQueue(): List<ListingEntity>

    @Query("SELECT COUNT(*) FROM listings WHERE status = 'ACTIVE'")
    fun getActiveListingsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<ListingEntity>)

    @Update
    suspend fun updateListing(listing: ListingEntity)

    @Delete
    suspend fun deleteListing(listing: ListingEntity)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListingById(id: Long)

    @Query("UPDATE listings SET status = :status, lastSyncNote = :note, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateListingStatus(id: Long, status: String, note: String, timestamp: Long)
}
