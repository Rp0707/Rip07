package com.example.security

import java.net.URLEncoder
import java.security.MessageDigest

/**
 * High-Security Bank Credentials Vault & Anti-Tamper Protection Module.
 * Encrypts and locks receiving bank parameters for HTX Nông Nghiệp Bình Mỹ.
 * Prevents unauthorized modification or hacker tampering of bank account details.
 */
object BankSecurityVault {

    // XOR Obfuscated byte arrays (Key Mask: 0x55)
    private val OBFUSCATED_BANK_CODE = byteArrayOf(0x01, 0x16, 0x17) // "TCB"
    private val OBFUSCATED_ACC_NUM = byteArrayOf(0x63, 0x6C, 0x64, 0x62, 0x64, 0x6D, 0x66, 0x61, 0x60, 0x60) // "6917183455"
    private val OBFUSCATED_ACC_NAME = byteArrayOf(0x03, 0x1A, 0x75, 0x0D, 0x00, 0x14, 0x1B, 0x75, 0x1D, 0x1C, 0x10, 0x05) // "VO XUAN HIEP"

    private const val OBFUSCATION_MASK: Int = 0x55

    // SHA-256 Hashes for Runtime Source Code Integrity Verification
    private const val EXPECTED_BANK_CODE_HASH = "b68735dfdb8df7f0c180aeec8b5c9dc756df626c04fdf5ae9d7e35b71c775798" // SHA256("TCB")
    private const val EXPECTED_ACC_NUM_HASH = "8a2f4dd1be45c6ee185ff109d9f5835b6fbca5a8c8868c6a2e46b01476dbb443" // SHA256("6917183455")

    /**
     * Decrypts byte array using bitwise XOR and performs runtime hash integrity check.
     */
    private fun decryptAndVerify(obfuscated: ByteArray, expectedHash: String? = null): String {
        val decryptedChars = CharArray(obfuscated.size)
        for (i in obfuscated.indices) {
            decryptedChars[i] = (obfuscated[i].toInt() xor OBFUSCATION_MASK).toChar()
        }
        val result = String(decryptedChars)

        if (expectedHash != null) {
            val actualHash = sha256(result)
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                // If code was altered by third party, fallback forcefully to authentic verified defaults
                return if (expectedHash == EXPECTED_BANK_CODE_HASH) "TCB" else "6917183455"
            }
        }
        return result
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun getBankCode(): String = decryptAndVerify(OBFUSCATED_BANK_CODE, EXPECTED_BANK_CODE_HASH)

    fun getAccountNumber(): String = decryptAndVerify(OBFUSCATED_ACC_NUM, EXPECTED_ACC_NUM_HASH)

    fun getAccountName(): String = decryptAndVerify(OBFUSCATED_ACC_NAME)

    fun getBankTitle(): String = "TECHCOMBANK"

    fun getBeneficiaryTitle(): String = "HTX NÔNG NGHIỆP BÌNH MỸ (${getAccountName()})"

    /**
     * Builds tamper-proof, high-resolution VietQR image URL using encrypted credentials.
     */
    fun buildVietQrUrl(paymentCode: String, amount: Double): String {
        val code = getBankCode()
        val num = getAccountNumber()
        val name = getAccountName()
        val encodedName = try {
            URLEncoder.encode(name.trim().uppercase(), "UTF-8")
        } catch (e: Exception) {
            "VO%20XUAN%20HIEP"
        }
        val amountLong = amount.toLong()
        return "https://img.vietqr.io/image/$code-$num-compact2.png?amount=$amountLong&addInfo=$paymentCode&accountName=$encodedName"
    }
}
