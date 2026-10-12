package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class AppConverters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.BUYER)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = runCatching { SessionStatus.valueOf(value) }.getOrDefault(SessionStatus.OPEN)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = runCatching { OrderStatus.valueOf(value) }.getOrDefault(OrderStatus.PLACED)

    @TypeConverter
    fun fromDeliveryType(value: DeliveryType): String = value.name

    @TypeConverter
    fun toDeliveryType(value: String): DeliveryType = runCatching { DeliveryType.valueOf(value) }.getOrDefault(DeliveryType.POPUP_PICKUP)

    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = runCatching { RecurringFrequency.valueOf(value) }.getOrDefault(RecurringFrequency.WEEKLY)

    @TypeConverter
    fun fromRecurringStatus(value: RecurringStatus): String = value.name

    @TypeConverter
    fun toRecurringStatus(value: String): RecurringStatus = runCatching { RecurringStatus.valueOf(value) }.getOrDefault(RecurringStatus.ACTIVE)

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus): String = value.name

    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus = runCatching { VerificationStatus.valueOf(value) }.getOrDefault(VerificationStatus.PENDING)

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = runCatching { NotificationType.valueOf(value) }.getOrDefault(NotificationType.ORDER_CONFIRMED)
}
