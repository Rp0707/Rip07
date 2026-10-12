package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentProviderType(val labelVi: String) {
    VNPAY("Cổng thanh toán VNPay"),
    MOMO("Ví điện tử MoMo"),
    ZALOPAY("Ví điện tử ZaloPay"),
    BANK_QR("Chuyển khoản QR Ngân hàng")
}

enum class PaymentAttemptStatus(val labelVi: String) {
    PENDING("Chờ thanh toán"),
    VERIFYING("Đang xác minh giao dịch"),
    SUCCESS("Đã thanh toán thành công"),
    FAILED("Thanh toán thất bại"),
    EXPIRED("Phiên thanh toán hết hạn"),
    USER_CANCELLED("Người mua hủy giao dịch")
}

@Entity(tableName = "payment_attempts")
data class PaymentAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentCode: String,        // Mã phiên thanh toán nội bộ (unique)
    val orderId: Long,
    val buyerId: Long,
    val provider: PaymentProviderType,
    val amount: Double,             // Số tiền giao dịch thực tế
    val isDepositAttempt: Boolean = false, // Có phải thanh toán tiền cọc hay không
    val depositAmount: Double = 0.0, // Số tiền cọc cần đóng
    val remainingAmount: Double = 0.0, // Số tiền còn lại phải thanh toán sau
    val currency: String = "VND",
    val status: PaymentAttemptStatus = PaymentAttemptStatus.PENDING,
    val providerReferenceCode: String?, // Mã tham chiếu nhà cung cấp
    val providerTransactionCode: String?, // Mã giao dịch nhà cung cấp trả về
    val errorMessage: String? = null,
    val idempotencyKey: String,
    val expiresAtEpochMs: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val verifiedAtEpochMs: Long? = null
)

@Entity(tableName = "refund_requests")
data class RefundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val refundCode: String,         // Mã hoàn tiền nội bộ (unique)
    val orderId: Long,
    val paymentAttemptId: Long,
    val buyerId: Long,
    val amount: Double,             // Số tiền hoàn trả
    val reason: String,
    val status: String = "PENDING", // "PENDING", "SUCCESS", "FAILED"
    val providerReferenceCode: String? = null,
    val idempotencyKey: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val processedAtEpochMs: Long? = null
)

@Entity(tableName = "webhook_events")
data class WebhookEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,            // Unique ID từ nhà cung cấp hoặc hash
    val provider: String,
    val payloadJson: String,
    val isProcessed: Boolean = false,
    val receivedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_reservations")
data class InventoryReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val quantityReserved: Double,
    val expiresAtEpochMs: Long,
    val isReleased: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "outbox_events")
data class OutboxEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,          // Loại sự kiện (e.g., "PAYMENT_SUCCESS_NOTIF")
    val payloadJson: String,
    val isDispatched: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
