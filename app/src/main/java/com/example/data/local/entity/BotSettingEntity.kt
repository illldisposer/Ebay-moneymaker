package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_settings")
data class BotSettingEntity(
    @PrimaryKey
    val id: Int = 1,
    val isBotActive: Boolean = true,
    val syncIntervalSeconds: Int = 30,
    val autoBuyOnSale: Boolean = true,
    val handlingDays: Int = 2,
    val marginEuro: Double = 1.00,
    val searchKeywords: String = "Elektronik, Zubehör, Haushalt, Werkzeuge",
    val minResonanceScore: Int = 75,
    val connectedAccount: String = "MeinEbayShop_DE (Aktiv)",
    val notificationsEnabled: Boolean = true,
    val lastSyncRun: Long = System.currentTimeMillis()
)
