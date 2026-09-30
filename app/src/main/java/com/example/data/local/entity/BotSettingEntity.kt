package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_settings")
data class BotSettingEntity(
    @PrimaryKey
    val id: Int = 1,
    val isBotActive: Boolean = true,
    val isLiveMode: Boolean = false, // false = Test-Modus (Simulation), true = Live-Modus (eBay.de Live)
    val marginType: String = "PERCENTAGE", // Standardmäßig auf Prozentual beim ersten Start
    val marginEuro: Double = 1.00,
    val marginPercentage: Double = 5.0, // Standardmäßig auf 5% Prozent
    val autoCloneCount: Int = 10, // Einstellbar von 0 bis 1000
    val autoCloneOnLogin: Boolean = false, // Kein automatisches Klonen beim ersten Start / Login ohne Auslösung
    val autoRefillOnSale: Boolean = true, // Wenn 1 Artikel verkauft wird, automatisch direkt neuen erstellen
    val syncIntervalSeconds: Int = 30,
    val autoBuyOnSale: Boolean = true,
    val handlingDays: Int = 2,
    val searchKeywords: String = "Elektronik, Zubehör, Haushalt, Werkzeuge",
    val minResonanceScore: Int = 75,
    val connectedAccount: String = "MeinEbayShop_DE",
    val ebayUsername: String = "",
    val isEbayLoggedIn: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val lastSyncRun: Long = System.currentTimeMillis()
) {
    fun calculatePrice(originalPrice: Double, originalShipping: Double): Double {
        val base = originalPrice + originalShipping
        val calculated = if (marginType == "PERCENTAGE") {
            base * (1.0 + (marginPercentage / 100.0))
        } else {
            base + marginEuro
        }
        return Math.round(calculated * 100.0) / 100.0
    }

    fun calculateMargin(originalPrice: Double, originalShipping: Double): Double {
        val sellPrice = calculatePrice(originalPrice, originalShipping)
        return Math.round((sellPrice - (originalPrice + originalShipping)) * 100.0) / 100.0
    }
}
