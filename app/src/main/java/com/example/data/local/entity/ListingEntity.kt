package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalItemId: String,
    val originalTitle: String,
    val originalDescription: String,
    val originalPrice: Double,
    val originalShippingCost: Double,
    val originalSeller: String,
    val originalUrl: String,
    val imageUrl: String,
    val category: String,
    val viewsCount: Int,
    val soldCount: Int,
    val resonanceScore: Int,
    val resonanceReason: String,
    val myListingId: String,
    val myTitle: String,
    val myDescription: String,
    val myPrice: Double, // originalPrice + originalShippingCost + 1.00
    val myShippingCost: Double = 0.0, // Always 0.00 (Kostenloser Versand)
    val handlingTimeDays: Int = 2, // 2 days handling time
    val status: String, // "ACTIVE", "PAUSED", "ORIGINAL_DELETED", "ORIGINAL_SOLD", "DEACTIVATED", "SOLD"
    val autoRelist: Boolean = true,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncNote: String = "Neu angelegt und synchronisiert"
)
