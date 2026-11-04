package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ein Preisvergleichs-Treffer: ein AliExpress-Produkt und das beste passende eBay-Angebot.
 *
 * status:
 *  - "AUTO"     = sicher zugeordnet (Konfidenz >= Auto-Schwelle), erscheint im Scanner
 *  - "REVIEW"   = unsicher, wartet im Menüpunkt "Prüfen" auf deine Entscheidung
 *  - "KEPT"     = von dir bestätigt (Behalten), erscheint im Scanner
 *  - "REJECTED" = von dir aussortiert (Raus), taucht nie wieder auf
 */
@Entity(
    tableName = "matches",
    indices = [Index(value = ["pairKey"], unique = true)]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pairKey: String, // "ali:<productId>|ebay:<itemId>"
    val keyword: String,

    // AliExpress
    val aliProductId: String,
    val aliTitle: String,
    val aliPrice: Double,
    val aliOriginalPrice: Double,
    val aliCurrency: String,
    val aliImageUrl: String,
    val aliProductUrl: String,
    val aliAffiliateUrl: String,
    val aliSalesVolume: Int,
    val aliCommissionRate: String,
    val aliCategory: String,

    // eBay
    val ebayItemId: String,
    val ebayTitle: String,
    val ebayPrice: Double,
    val ebayShipping: Double,
    val ebayCurrency: String,
    val ebayImageUrl: String,
    val ebayItemUrl: String,
    val ebayCondition: String,
    val ebayCandidateCount: Int,

    // Zuordnung
    val matchMethod: String, // "EAN", "KI-Bildvergleich", "Titelvergleich"
    val confidence: Int, // 0-100
    val matchReason: String,
    val status: String,
    val decidedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun MatchEntity.ebayTotal(): Double = ebayPrice + ebayShipping

fun MatchEntity.priceDiff(): Double = ebayTotal() - aliPrice

fun MatchEntity.priceDiffPercent(): Double = if (aliPrice > 0.0) (priceDiff() / aliPrice) * 100.0 else 0.0

fun MatchEntity.bestAliLink(): String = aliAffiliateUrl.ifBlank { aliProductUrl }
