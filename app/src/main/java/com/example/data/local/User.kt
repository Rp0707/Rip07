package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserRole

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val role: UserRole,
    val farmerProfileId: Long? = null,
    val assignedPopupLocationId: Long? = null,
    val avatarEmoji: String = "👤",
    
    // Security Tiers
    val passwordHash: String? = null,
    val googleSub: String? = null,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val failedLoginAttempts: Int = 0,
    val isMfaEnabled: Boolean = false,
    val mfaSecret: String? = null,
    val lastActiveTime: Long = 0L
)


