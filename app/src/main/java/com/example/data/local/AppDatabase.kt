package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        FarmerProfileEntity::class,
        PreOrderSessionEntity::class,
        ProductEntity::class,
        ProductLotEntity::class,
        PopUpLocationEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        RecurringScheduleEntity::class,
        AppNotificationEntity::class,
        UserSessionEntity::class,
        AuditLogEntity::class,
        ProductReviewEntity::class,
        PaymentAttemptEntity::class,
        RefundEntity::class,
        WebhookEventEntity::class,
        InventoryReservationEntity::class,
        OutboxEventEntity::class,
        RoleEntity::class,
        PermissionEntity::class,
        RolePermissionCrossRef::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun farmerProfileDao(): FarmerProfileDao
    abstract fun preOrderSessionDao(): PreOrderSessionDao
    abstract fun productDao(): ProductDao
    abstract fun productLotDao(): ProductLotDao
    abstract fun popUpLocationDao(): PopUpLocationDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun recurringScheduleDao(): RecurringScheduleDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun productReviewDao(): ProductReviewDao
    abstract fun paymentAttemptDao(): PaymentAttemptDao
    abstract fun rbacDao(): RbacDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "binh_my_farm_app_v2.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
