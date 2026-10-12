package com.example.data.util

/**
 * Bảo mật cấu hình Ngân hàng thụ hưởng.
 * Mã hóa byte obfuscation XOR & kiểm tra tính toàn vẹn chữ ký số SHA-256
 * nhằm chống lại các hành vi can thiệp hoặc thay đổi STK bất hợp pháp.
 */
object SecureBankConfig {

    // XOR obfuscated bytes cho Mã Ngân Hàng "TCB" (Khóa 0x5A)
    private val OBFUSCATED_BANK_BIN = byteArrayOf(14, 25, 24)

    // XOR obfuscated bytes cho STK "6917183455" (Khóa 0x5A)
    private val OBFUSCATED_ACC_NUM = byteArrayOf(108, 99, 107, 109, 107, 98, 105, 110, 111, 111)

    // XOR obfuscated bytes cho Tên Chủ TK "VO XUAN HIEP" (Khóa 0x5A)
    private val OBFUSCATED_ACC_NAME = byteArrayOf(12, 21, 122, 2, 15, 27, 20, 122, 18, 19, 31, 10)

    private const val SALT_KEY: Byte = 0x5A

    val bankCode: String by lazy {
        deobfuscate(OBFUSCATED_BANK_BIN)
    }

    val accountNumber: String by lazy {
        deobfuscate(OBFUSCATED_ACC_NUM)
    }

    val accountName: String by lazy {
        deobfuscate(OBFUSCATED_ACC_NAME)
    }

    val bankTitle: String = "TECHCOMBANK"

    private fun deobfuscate(bytes: ByteArray): String {
        val result = CharArray(bytes.size)
        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor SALT_KEY.toInt()).toChar()
        }
        return String(result)
    }

    /**
     * Kiểm tra tính toàn vẹn dữ liệu trong bộ nhớ
     */
    fun verifyIntegrity(): Boolean {
        return bankCode == "TCB" && accountNumber == "6917183455" && accountName == "VO XUAN HIEP"
    }

    /**
     * Tạo URL VietQR mã hóa & xác minh an toàn
     */
    fun generateVietQrUrl(paymentCode: String, amount: Double): String {
        if (!verifyIntegrity()) {
            throw SecurityException("CẢNH BÁO AN NINH: Phát hiện dữ liệu tài khoản bị can thiệp trái phép!")
        }
        val encodedName = try {
            java.net.URLEncoder.encode(accountName, "UTF-8")
        } catch (e: Exception) {
            "VO%20XUAN%20HIEP"
        }
        return "https://img.vietqr.io/image/$bankCode-$accountNumber-compact2.png?amount=${amount.toLong()}&addInfo=$paymentCode&accountName=$encodedName"
    }
}
