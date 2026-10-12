package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerId: Long,
    val name: String,
    val category: String,
    val unit: String,
    val currentPricePerUnit: Double,
    val originalEstimateKg: Double,
    val reservedKg: Double = 0.0,
    val storageInstructions: String,
    val originInfo: String,
    val isAvailable: Boolean = true,
    val lotCode: String,
    val imageUrl: String = ""
)


