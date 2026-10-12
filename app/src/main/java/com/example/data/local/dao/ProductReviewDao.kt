package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ProductReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ProductReviewEntity): Long

    @Query("SELECT * FROM product_reviews WHERE productId = :productId ORDER BY createdAtEpochMs DESC")
    fun getReviewsForProduct(productId: Long): Flow<List<ProductReviewEntity>>

    @Query("SELECT * FROM product_reviews WHERE orderId = :orderId AND productId = :productId LIMIT 1")
    suspend fun getReviewByOrderAndProduct(orderId: Long, productId: Long): ProductReviewEntity?

    @Query("SELECT * FROM product_reviews")
    fun getAllReviews(): Flow<List<ProductReviewEntity>>
}
