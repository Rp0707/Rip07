package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RecurringScheduleEntity
import com.example.data.model.RecurringStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringScheduleDao {
    @Query("SELECT * FROM recurring_schedules WHERE buyerId = :buyerId ORDER BY createdAtEpochMs DESC")
    fun getSchedulesByBuyer(buyerId: Long): Flow<List<RecurringScheduleEntity>>

    @Query("SELECT * FROM recurring_schedules WHERE status = 'ACTIVE'")
    fun getAllActiveSchedules(): Flow<List<RecurringScheduleEntity>>

    @Query("SELECT * FROM recurring_schedules WHERE status = 'ACTIVE'")
    suspend fun getAllActiveSchedulesSync(): List<RecurringScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: RecurringScheduleEntity): Long

    @Query("UPDATE recurring_schedules SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: RecurringStatus)

    @Query("UPDATE recurring_schedules SET nextDeliveryDateEpochMs = :nextDateEpochMs WHERE id = :id")
    suspend fun updateNextDeliveryDate(id: Long, nextDateEpochMs: Long)

    @Update
    suspend fun updateSchedule(schedule: RecurringScheduleEntity)
}
