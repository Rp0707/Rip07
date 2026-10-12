package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductByIdSync(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE farmerId = :farmerId")
    fun getProductsByFarmer(farmerId: Long): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET reservedKg = reservedKg + :additionalKg WHERE id = :id")
    suspend fun updateReservedKg(id: Long, additionalKg: Double)

    @Query("UPDATE products SET reservedKg = :reservedKg WHERE id = :id")
    suspend fun setReservedKg(id: Long, reservedKg: Double)

    @Query("UPDATE products SET currentPricePerUnit = :newPrice WHERE id = :id")
    suspend fun updatePrice(id: Long, newPrice: Double)

    @Update
    suspend fun updateProduct(product: ProductEntity)
}
