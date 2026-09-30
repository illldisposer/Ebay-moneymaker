package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_logs")
data class SystemLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "SYNC_SUCCESS", "PRICE_MIRROR", "ORIGINAL_DELETED", "ORIGINAL_SOLD", "AUTO_BUY", "PAYMENT_CONFIRMED", "SEARCH_DISCOVERY", "ERROR"
    val title: String,
    val details: String,
    val relatedItemId: String? = null,
    val isError: Boolean = false
)
