package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ProductLotEntity
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductLotDao {
    @Query("SELECT * FROM product_lots ORDER BY id DESC")
    fun getAllLots(): Flow<List<ProductLotEntity>>

    @Query("SELECT * FROM product_lots WHERE lotCode = :lotCode LIMIT 1")
    fun getLotByCode(lotCode: String): Flow<ProductLotEntity?>

    @Query("SELECT * FROM product_lots WHERE lotCode = :lotCode LIMIT 1")
    suspend fun getLotByCodeSync(lotCode: String): ProductLotEntity?

    @Query("SELECT * FROM product_lots WHERE farmerId = :farmerId")
    fun getLotsByFarmer(farmerId: Long): Flow<List<ProductLotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: ProductLotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLots(lots: List<ProductLotEntity>)

    @Query("UPDATE product_lots SET verificationStatus = :status, verifierName = :verifierName, verificationTimeEpochMs = :timeMs, sha256Hash = :hash WHERE lotCode = :lotCode")
    suspend fun updateVerification(
        lotCode: String,
        status: VerificationStatus,
        verifierName: String,
        timeMs: Long,
        hash: String
    )

    @Query("UPDATE product_lots SET isShortage = :isShortage, shortageKg = :shortageKg, shortageNote = :note WHERE lotCode = :lotCode")
    suspend fun updateShortage(lotCode: String, isShortage: Boolean, shortageKg: Double, note: String)

    @Query("UPDATE product_lots SET actualYieldKg = :actualKg WHERE lotCode = :lotCode")
    suspend fun updateActualYield(lotCode: String, actualKg: Double)
}
