package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.UserSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isRevoked = 0 ORDER BY lastActiveTime DESC")
    fun getActiveSessionsForUser(userId: Long): Flow<List<UserSessionEntity>>

    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isRevoked = 0 ORDER BY lastActiveTime DESC")
    suspend fun getActiveSessionsForUserSync(userId: Long): List<UserSessionEntity>

    @Query("SELECT * FROM user_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionByToken(sessionId: String): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity): Long

    @Query("UPDATE user_sessions SET isRevoked = 1 WHERE sessionId = :sessionId")
    suspend fun revokeSession(sessionId: String)

    @Query("UPDATE user_sessions SET isRevoked = 1 WHERE userId = :userId")
    suspend fun revokeAllSessionsForUser(userId: Long)
    
    @Query("UPDATE user_sessions SET lastActiveTime = :time WHERE sessionId = :sessionId")
    suspend fun updateActiveTime(sessionId: String, time: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSync(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}
