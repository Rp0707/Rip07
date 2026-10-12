package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DeliveryType
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringStatus

@Entity(tableName = "recurring_schedules")
data class RecurringScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buyerId: Long,
    val productId: Long,
    val quantity: Double,
    val frequency: RecurringFrequency,
    val deliveryType: DeliveryType,
    val popupLocationId: Long?,
    val homeAddress: String?,
    val receiverName: String,
    val receiverPhone: String,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val nextDeliveryDateEpochMs: Long,
    val lastGeneratedOrderId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
