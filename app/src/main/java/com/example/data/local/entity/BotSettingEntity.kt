package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_settings")
data class BotSettingEntity(
    @PrimaryKey
    val id: Int = 1,

    // eBay Browse API (Application Token, Client Credentials)
    val ebayClientId: String = "",
    val ebayClientSecret: String = "",
    val ebayUseSandbox: Boolean = false,
    val ebayMarketplaceId: String = "EBAY_DE",

    // AliExpress Affiliate API
    val aliAppKey: String = "",
    val aliAppSecret: String = "",
    val aliTrackingId: String = "",

    // Gemini (leer = Schlüssel aus .env / BuildConfig)
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-flash-latest",

    // Scan
    val searchKeywords: String = "Handyhalterung, LED Streifen, Kabelorganizer",
    val productsPerKeyword: Int = 10,
    val ebayCandidatesPerProduct: Int = 5,

    // Zuordnung
    val minConfidence: Int = 40, // darunter wird ein Treffer verworfen
    val autoAcceptConfidence: Int = 85, // ab hier gilt ein Treffer als sicher
    val autoAdjustThreshold: Boolean = true, // Auto-Schwelle aus Prüf-Entscheidungen lernen

    val notificationsEnabled: Boolean = true,
    val lastScanRun: Long = 0L
) {
    fun hasEbayKeys(): Boolean = ebayClientId.isNotBlank() && ebayClientSecret.isNotBlank()

    fun hasAliKeys(): Boolean = aliAppKey.isNotBlank() && aliAppSecret.isNotBlank() && aliTrackingId.isNotBlank()

    fun keywordList(): List<String> =
        searchKeywords.split(",", ";", "\n").map { it.trim() }.filter { it.isNotEmpty() }
}
