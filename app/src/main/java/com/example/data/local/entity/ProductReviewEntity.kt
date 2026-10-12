package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_reviews")
data class ProductReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val orderId: Long,
    val buyerId: Long,
    val buyerName: String,
    val rating: Int, // 1 to 5 stars
    val comment: String,
    val createdAtEpochMs: Long
)
