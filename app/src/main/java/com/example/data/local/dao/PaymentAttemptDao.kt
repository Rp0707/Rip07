package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.PaymentAttemptEntity
import com.example.data.local.RefundEntity
import com.example.data.local.WebhookEventEntity
import com.example.data.local.InventoryReservationEntity
import com.example.data.local.OutboxEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentAttempt(attempt: PaymentAttemptEntity): Long

    @Update
    suspend fun updatePaymentAttempt(attempt: PaymentAttemptEntity)

    @Query("SELECT * FROM payment_attempts WHERE paymentCode = :code LIMIT 1")
    suspend fun getPaymentAttemptByCode(code: String): PaymentAttemptEntity?

    @Query("SELECT * FROM payment_attempts WHERE orderId = :orderId")
    fun getPaymentAttemptsForOrder(orderId: Long): Flow<List<PaymentAttemptEntity>>

    @Query("SELECT * FROM payment_attempts WHERE buyerId = :buyerId")
    fun getPaymentAttemptsForBuyer(buyerId: Long): Flow<List<PaymentAttemptEntity>>

    @Query("SELECT * FROM payment_attempts")
    fun getAllPaymentAttempts(): Flow<List<PaymentAttemptEntity>>

    // Webhook events tracking
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWebhookEvent(event: WebhookEventEntity): Long

    @Query("SELECT * FROM webhook_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getWebhookEventById(eventId: String): WebhookEventEntity?

    @Update
    suspend fun updateWebhookEvent(event: WebhookEventEntity)

    // Refunds requests
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefund(refund: RefundEntity): Long

    @Update
    suspend fun updateRefund(refund: RefundEntity)

    @Query("SELECT * FROM refund_requests WHERE orderId = :orderId")
    fun getRefundsForOrder(orderId: Long): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refund_requests")
    fun getAllRefunds(): Flow<List<RefundEntity>>

    // Inventory reservations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: InventoryReservationEntity): Long

    @Query("SELECT * FROM inventory_reservations WHERE orderId = :orderId AND isReleased = 0")
    suspend fun getActiveReservationsForOrder(orderId: Long): List<InventoryReservationEntity>

    @Query("UPDATE inventory_reservations SET isReleased = 1 WHERE orderId = :orderId")
    suspend fun releaseReservationsForOrder(orderId: Long)

    // Outbox messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEvent(event: OutboxEventEntity): Long

    @Query("SELECT * FROM outbox_events WHERE isDispatched = 0")
    suspend fun getPendingOutboxEvents(): List<OutboxEventEntity>

    @Update
    suspend fun updateOutboxEvent(event: OutboxEventEntity)
}
