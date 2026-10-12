package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "popup_locations")
data class PopUpLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val operatingHours: String,
    val contactPhone: String,
    val supportsHomeDelivery: Boolean = true,
    val baseDeliveryFee: Double = 15000.0,
    val notes: String = "Có kho bảo quản mát tạm thời trong ngày nhận"
)
