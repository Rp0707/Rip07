package com.example.data.util

import com.example.data.local.PaymentAttemptEntity
import com.example.data.local.PaymentAttemptStatus
import com.example.data.local.PaymentProviderType
import java.util.UUID

data class PaymentInitResult(
    val paymentCode: String,
    val amount: Double,
    val paymentUrl: String,
    val qrCodePayload: String,
    val expiresAtMs: Long
)

interface PaymentProvider {
    suspend fun initiatePayment(
        orderId: Long,
        buyerId: Long,
        amount: Double,
        provider: PaymentProviderType,
        isDeposit: Boolean = false,
        depositAmt: Double = 0.0,
        remainingAmt: Double = 0.0,
        idempotencyKey: String
    ): PaymentInitResult

    suspend fun queryStatus(paymentCode: String): PaymentAttemptStatus
}

class BinhMyPayProvider : PaymentProvider {
    
    override suspend fun initiatePayment(
        orderId: Long,
        buyerId: Long,
        amount: Double,
        provider: PaymentProviderType,
        isDeposit: Boolean,
        depositAmt: Double,
        remainingAmt: Double,
        idempotencyKey: String
    ): PaymentInitResult {
        val uniquePaymentCode = "BMP-${System.currentTimeMillis() / 1000}-${UUID.randomUUID().toString().take(5).uppercase()}"
        val actualPayAmount = if (isDeposit) depositAmt else amount
        val validityPeriodMs = 15 * 60 * 1000L // 15 Minutes countdown
        val expiresAtMs = System.currentTimeMillis() + validityPeriodMs
        
        // Structured Quick Response payload matching exact merchant regulations
        val qrPayload = when (provider) {
            PaymentProviderType.VNPAY -> "https://vnpay.vn/payment-qr?code=$uniquePaymentCode&amount=${actualPayAmount.toInt()}"
            PaymentProviderType.MOMO -> "momo://payment?code=$uniquePaymentCode&amount=${actualPayAmount.toInt()}"
            PaymentProviderType.ZALOPAY -> "zalopay://payment?code=$uniquePaymentCode&amount=${actualPayAmount.toInt()}"
            PaymentProviderType.BANK_QR -> "https://img.vietqr.io/image/TCB-6917183455-compact2.png?amount=${actualPayAmount.toInt()}&addInfo=$uniquePaymentCode&accountName=VO%20XUAN%20HIEP"
        }
        
        return PaymentInitResult(
            paymentCode = uniquePaymentCode,
            amount = actualPayAmount,
            paymentUrl = qrPayload,
            qrCodePayload = qrPayload,
            expiresAtMs = expiresAtMs
        )
    }

    override suspend fun queryStatus(paymentCode: String): PaymentAttemptStatus {
        // Query server or local memory simulation
        return PaymentAttemptStatus.PENDING
    }
}
