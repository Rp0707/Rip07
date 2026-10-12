package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.NotificationType
import com.example.data.model.UserRole

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0L,
    val targetRole: UserRole? = null,
    val title: String,
    val message: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val orderId: Long? = null,
    val sessionId: Long? = null,
    val type: NotificationType = NotificationType.ORDER_CONFIRMED,
    val priority: String = "NORMAL"
)
