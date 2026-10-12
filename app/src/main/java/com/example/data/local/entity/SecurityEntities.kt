package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val sessionId: String,
    val deviceName: String,
    val ipAddress: String,
    val loginTime: Long,
    val lastActiveTime: Long,
    val isRevoked: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorId: Long,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val timestamp: Long,
    val result: String, // "SUCCESS" or "FAILURE"
    val isAnomaly: Boolean = false
)
