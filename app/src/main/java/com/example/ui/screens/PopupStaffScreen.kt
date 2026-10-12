package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.data.model.OrderStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun PopupStaffScreen(
    staffUser: UserEntity?,
    assignedPopup: PopUpLocationEntity?,
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    onCheckInCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputCode by remember { mutableStateOf("") }

    val myOrders = orders.filter { it.popupLocationId == assignedPopup?.id || assignedPopup == null }
    val pendingPickup = myOrders.filter { it.status == OrderStatus.READY_FOR_PICKUP_OR_DELIVERING || it.status == OrderStatus.PREPARING || it.status == OrderStatus.LOCKED }
    val completedOrders = myOrders.filter { it.status == OrderStatus.COMPLETED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("popup_staff_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        // Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FarmGreenDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ĐIỂM NHẬN HÀNG POP-UP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmGoldContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = assignedPopup?.name ?: "Trạm Pop-up Bình Mỹ 4A",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Nhân viên trực trạm: ${staffUser?.fullName ?: "Nguyễn Văn Tuấn"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Địa chỉ: ${assignedPopup?.address ?: "Xã Bình Mỹ, Củ Chi"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Scan / Code Check-in Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kiểm Tra Mã Nhận Hàng Hoặc Quét QR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nhập mã nhận (VD: PU-718) hoặc mã đơn (VD: BM-ORD-84920) để đối soát và trao hàng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            placeholder = { Text("Nhập PU-718...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_staff_pickup_code"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (inputCode.isNotBlank()) {
                                    onCheckInCode(inputCode.trim())
                                    inputCode = ""
                                }
                            },
                            enabled = inputCode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("btn_staff_checkin")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bàn Giao", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List Orders Awaiting Handover
        item {
            Text(
                text = "Đơn Hàng Cần Bàn Giao (${pendingPickup.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (pendingPickup.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FarmSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hiện không có đơn nào đang chờ nhận tại trạm này.",
                        modifier = Modifier.padding(16.dp),
                        color = FarmTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(pendingPickup) { order ->
                val items = orderItems.filter { it.orderId == order.id }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mã nhận: ${order.pickupCode}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                                Text(
                                    text = "Đơn: ${order.orderCode} • ${order.receiverName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextSecondary
                                )
                            }
                            OrderStatusChip(status = order.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        items.forEach { item ->
                            Text(
                                text = "• ${item.productName}: ${"%.0f".format(item.quantity)} ${item.unit}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thu tiền: ${formatVnd(order.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { onCheckInCode(order.pickupCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Khách Đã Nhận", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // List Completed Orders
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đơn Đã Giao Thành Công (${completedOrders.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FarmTextSecondary
            )
        }

        items(completedOrders.take(5)) { order ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FarmSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${order.orderCode} • ${order.receiverName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Mã nhận: ${order.pickupCode} • ${formatDateTime(order.updatedAtEpochMs)}", fontSize = 11.sp, color = FarmTextSecondary)
                    }
                    Text("Đã nhận", color = FarmGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
