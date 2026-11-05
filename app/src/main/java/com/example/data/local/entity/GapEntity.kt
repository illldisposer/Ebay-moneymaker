package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ein eBay-Angebot aus der Marktlücken-Suche, das du auf Amazon prüfst.
 * Es werden nur Titel, Preis, Bildadresse und Link gespeichert, keine fremden Bilder oder Texte.
 *
 * amazonStatus:
 *  - "OPEN"    = noch nicht geprüft
 *  - "BOTH"    = gibt es auch auf Amazon (auf beiden Plattformen)
 *  - "GAP"     = nur auf eBay gefunden (Marktlücke)
 *  - "UNCLEAR" = nicht sicher, später nochmal anschauen
 *
 * ebaySoldEstimate: geschätzte Verkaufsmenge laut eBay (Einzelabfrage). null = eBay liefert keine Angabe.
 * eBay liefert keine Klick- oder Aufrufzahlen.
 */
@Entity(
    tableName = "gaps",
    indices = [Index(value = ["ebayItemId"], unique = true)]
)
data class GapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ebayItemId: String,
    val keyword: String,
    val ebayTitle: String,
    val ebayPrice: Double,
    val ebayShipping: Double,
    val ebayCurrency: String,
    val ebayImageUrl: String,
    val ebayItemUrl: String,
    val ebayCondition: String,
    val ebaySoldEstimate: Int?,
    val gtin: String?,
    val amazonQuery: String,
    val amazonStatus: String = "OPEN",
    val foundAt: Long = System.currentTimeMillis(),
    val decidedAt: Long? = null
)

fun GapEntity.ebayTotal(): Double = ebayPrice + ebayShipping
