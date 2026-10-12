package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_profiles")
data class FarmerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdName: String,
    val representativeName: String,
    val phone: String,
    val addressDetail: String,
    val acreageHa: Double,
    val hasVietGap: Boolean,
    val vietGapCertCode: String,
    val vietGapValidUntil: String,
    val isApprovedByAdmin: Boolean,
    val bio: String,
    val mainCrops: String = "Rau muống nước, rau muống cạn, bầu, bí"
)
