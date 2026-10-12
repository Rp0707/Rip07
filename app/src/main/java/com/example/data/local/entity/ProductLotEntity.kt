package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.VerificationStatus

@Entity(tableName = "product_lots")
data class ProductLotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lotCode: String,
    val productId: Long,
    val farmerId: Long,
    val harvestDateEpochMs: Long,
    val estimatedYieldKg: Double,
    val actualYieldKg: Double? = null,
    val isShortage: Boolean = false,
    val shortageKg: Double = 0.0,
    val shortageNote: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val verifierName: String? = null,
    val verificationTimeEpochMs: Long? = null,
    val sha256Hash: String? = null,
    val publicNotes: String = ""
)
