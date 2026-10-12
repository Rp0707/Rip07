package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PopUpLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PopUpLocationDao {
    @Query("SELECT * FROM popup_locations ORDER BY id ASC")
    fun getAllLocations(): Flow<List<PopUpLocationEntity>>

    @Query("SELECT * FROM popup_locations WHERE id = :id")
    fun getLocationById(id: Long): Flow<PopUpLocationEntity?>

    @Query("SELECT * FROM popup_locations WHERE id = :id")
    suspend fun getLocationByIdSync(id: Long): PopUpLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<PopUpLocationEntity>)
}
