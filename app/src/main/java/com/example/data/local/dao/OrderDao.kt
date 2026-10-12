package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAtEpochMs DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY createdAtEpochMs DESC")
    fun getOrdersByBuyer(buyerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderById(orderId: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderByIdSync(orderId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE orderCode = :orderCode LIMIT 1")
    fun getOrderByCode(orderCode: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE orderCode = :orderCode LIMIT 1")
    suspend fun getOrderByCodeSync(orderCode: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE pickupCode = :pickupCode LIMIT 1")
    fun getOrderByPickupCode(pickupCode: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE pickupCode = :pickupCode LIMIT 1")
    suspend fun getOrderByPickupCodeSync(pickupCode: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE sessionId = :sessionId")
    fun getOrdersBySession(sessionId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sessionId = :sessionId")
    suspend fun getOrdersBySessionSync(sessionId: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE popupLocationId = :popupId ORDER BY createdAtEpochMs DESC")
    fun getOrdersByPopupLocation(popupId: Long): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :status, updatedAtEpochMs = :updatedTime WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus, updatedTime: Long)

    @Query("UPDATE orders SET status = :status, shortageCompensationAction = :action, shortageRefundAmount = :refundAmount, totalAmount = totalAmount - :refundAmount, updatedAtEpochMs = :updatedTime WHERE id = :orderId")
    suspend fun updateOrderCompensation(
        orderId: Long,
        action: String,
        refundAmount: Double,
        status: OrderStatus,
        updatedTime: Long
    )

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): OrderEntity?
}

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("UPDATE order_items SET quantity = :newQuantity, originalQuantity = :originalQty, lineTotal = :newLineTotal, isShortageAdjusted = 1, compensationNote = :note WHERE id = :itemId")
    suspend fun updateItemShortage(
        itemId: Long,
        newQuantity: Double,
        originalQty: Double,
        note: String,
        newLineTotal: Double
    )
}
