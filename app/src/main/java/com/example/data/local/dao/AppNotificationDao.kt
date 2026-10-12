package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AppNotificationEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications WHERE userId = :userId OR targetRole = :userRole OR (userId = 0 AND targetRole IS NULL) ORDER BY timestampEpochMs DESC")
    fun getNotificationsForUser(userId: Long, userRole: UserRole): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE (userId = :userId OR targetRole = :userRole OR (userId = 0 AND targetRole IS NULL)) AND isRead = 0")
    fun getUnreadCount(userId: Long, userRole: UserRole): Flow<Int>

    @Query("SELECT * FROM app_notifications ORDER BY timestampEpochMs DESC")
    fun getAllNotifications(): Flow<List<AppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotificationEntity>)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE userId = :userId OR targetRole = :userRole OR (userId = 0 AND targetRole IS NULL)")
    suspend fun markAllAsRead(userId: Long, userRole: UserRole)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)
}
