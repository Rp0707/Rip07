package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.DeliveryType
import com.example.data.model.OrderStatus

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderCode"], unique = true),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderCode: String,
    val buyerId: Long,
    val sessionId: Long,
    val deliveryType: DeliveryType,
    val popupLocationId: Long?,
    val homeAddress: String?,
    val receiverName: String,
    val receiverPhone: String,
    val status: OrderStatus = OrderStatus.PLACED,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double,
    val pickupCode: String,
    val idempotencyKey: String,
    val buyerNote: String? = null,
    val shortageCompensationAction: String? = null,
    val shortageRefundAmount: Double = 0.0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val farmerId: Long,
    val farmerName: String,
    val unit: String,
    val quantity: Double,
    val unitPriceSnapshot: Double,
    val lineTotal: Double,
    val isShortageAdjusted: Boolean = false,
    val originalQuantity: Double? = null,
    val compensationNote: String? = null
)


