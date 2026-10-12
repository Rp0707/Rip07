package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SessionStatus

@Entity(tableName = "pre_order_sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionCode: String,
    val title: String,
    val description: String,
    val openDateEpochMs: Long,
    val closeDateEpochMs: Long,
    val deliveryDateEpochMs: Long,
    val status: SessionStatus = SessionStatus.OPEN,
    val cancellationPolicy: String = "Cho phép sửa hoặc hủy miễn phí trước hạn khóa đơn 18:00 ngày trước thu hoạch."
)


