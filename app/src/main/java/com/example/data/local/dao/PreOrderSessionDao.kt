package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PreOrderSessionDao {
    @Query("SELECT * FROM pre_order_sessions ORDER BY closeDateEpochMs DESC")
    fun getAllSessions(): Flow<List<PreOrderSessionEntity>>

    @Query("SELECT * FROM pre_order_sessions WHERE status = 'OPEN' ORDER BY closeDateEpochMs ASC LIMIT 1")
    fun getActiveSession(): Flow<PreOrderSessionEntity?>

    @Query("SELECT * FROM pre_order_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<PreOrderSessionEntity?>

    @Query("SELECT * FROM pre_order_sessions WHERE id = :id")
    suspend fun getSessionByIdSync(id: Long): PreOrderSessionEntity?

    @Query("SELECT COUNT(*) FROM pre_order_sessions")
    suspend fun getSessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PreOrderSessionEntity): Long

    @Query("UPDATE pre_order_sessions SET status = :status WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, status: SessionStatus)

    @Update
    suspend fun updateSession(session: PreOrderSessionEntity)
}
