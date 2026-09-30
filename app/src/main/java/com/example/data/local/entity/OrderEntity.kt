package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listingId: Long,
    val myListingId: String,
    val originalItemId: String,
    val itemTitle: String,
    val imageUrl: String,
    val salePrice: Double, // The price our buyer paid (e.g. 35.99)
    val originalCostTotal: Double, // originalPrice + originalShipping (e.g. 34.99)
    val profit: Double = 1.00, // Fixed 1€ profit
    val orderDate: Long = System.currentTimeMillis(),
    val buyerName: String,
    val buyerStreet: String,
    val buyerCity: String,
    val buyerZip: String,
    val buyerCountry: String = "Deutschland",
    val buyerEmail: String,
    val paymentStatus: String, // "WAITING_FOR_PAYMENT", "PAYMENT_RECEIVED"
    val purchaseStatus: String, // "BOT_PURCHASE_INITIATED", "ADDRESS_ENTERED", "READY_FOR_SELLER_PAYOUT", "COMPLETED"
    val originalPurchaseId: String? = null,
    val paymentConfirmedAt: Long? = null,
    val trackingNumber: String? = null,
    val sellerPayoutMethod: String = "PayPal / Banküberweisung",
    val sellerName: String,
    val sellerIbanOrPaypal: String = "seller-pay@example.de",
    val notes: String? = null
)
