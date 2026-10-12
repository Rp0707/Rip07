package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FarmerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerProfileDao {
    @Query("SELECT * FROM farmer_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<FarmerProfileEntity>>

    @Query("SELECT * FROM farmer_profiles WHERE id = :id")
    fun getProfileById(id: Long): Flow<FarmerProfileEntity?>

    @Query("SELECT * FROM farmer_profiles WHERE id = :id")
    suspend fun getProfileByIdSync(id: Long): FarmerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FarmerProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<FarmerProfileEntity>)

    @Query("UPDATE farmer_profiles SET isApprovedByAdmin = :isApproved WHERE id = :id")
    suspend fun updateApprovalStatus(id: Long, isApproved: Boolean)
}
