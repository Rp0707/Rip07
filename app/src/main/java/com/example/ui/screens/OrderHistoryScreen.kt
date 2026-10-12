package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.local.*
import com.example.data.local.entity.*
import com.example.data.model.DeliveryType
import com.example.data.model.OrderStatus
import com.example.data.model.SessionStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OrderHistoryScreen(
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    sessions: List<PreOrderSessionEntity>,
    popups: List<PopUpLocationEntity>,
    reviews: List<ProductReviewEntity>,
    paymentAttempts: List<PaymentAttemptEntity> = emptyList(),
    onCancelOrder: (Long) -> Unit,
    onNavigateToProducts: () -> Unit,
    onSubmitReview: (Long, Long, Int, String) -> Unit,
    onPayOnline: (Long, PaymentProviderType, Boolean) -> Unit = { _, _, _ -> },
    onViewPayment: (String) -> Unit = {},
    onTrackOrder: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var orderToCancel by remember { mutableStateOf<OrderEntity?>(null) }
    var selectedOrderForQr by remember { mutableStateOf<OrderEntity?>(null) }
    var selectedReviewProductAndOrder by remember { mutableStateOf<Triple<Long, Long, String>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("order_history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        item {
            Text(
                text = "Đơn Hàng Đặt Trước Của Bạn",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Theo dõi tiến trình thu hoạch và mã nhận tại Pop-up",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bạn chưa có đơn đặt trước nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Các đơn đặt nông sản Bình Mỹ sẽ hiển thị tiến trình tại đây.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToProducts,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Khám Phá Nông Sản")
                        }
                    }
                }
            }
        } else {
            items(orders) { order ->
                val session = sessions.find { it.id == order.sessionId }
                val popup = popups.find { it.id == order.popupLocationId }
                val items = orderItems.filter { it.orderId == order.id }

                val isCancellable = order.status == OrderStatus.PLACED &&
                        session?.status == SessionStatus.OPEN &&
                        System.currentTimeMillis() < (session.closeDateEpochMs ?: 0L)

                val isDemoOrder = items.any { it.productName.contains("DỮ LIỆU MẪU") || it.productName.contains("SAMPLE") }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_card_${order.orderCode}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. Mã đơn và ngày đặt ở bên trái; trạng thái nhỏ gọn ở bên phải
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = order.orderCode,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = FarmTextPrimary
                                )
                                Text(
                                    text = formatDateTime(order.createdAtEpochMs),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = FarmTextSecondary
                                )
                            }
                            OrderStatusChip(status = order.status)
                        }

                        // Demo Label (if applicable, at the start of the card)
                        if (isDemoOrder) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = FarmGoldContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Dữ liệu mẫu trải nghiệm",
                                    color = FarmGoldOnContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Dòng mô tả trạng thái hiện tại dễ hiểu
                        val descriptiveStatus = when (order.status) {
                            OrderStatus.AWAITING_CONFIRMATION -> "Đang chờ xác nhận từ nông hộ Bình Mỹ"
                            OrderStatus.PLACED -> "Đơn hàng đã ghi nhận thành công, đang mở đặt trước"
                            OrderStatus.LOCKED -> "Nông hộ Bình Mỹ đã chốt đơn, sẵn sàng gặt cắt"
                            OrderStatus.PREPARING -> "Nông hộ đang cắt rau sáng sớm & chuyển tới điểm nhận"
                            OrderStatus.READY_FOR_PICKUP_OR_DELIVERING -> "Đã cập bến Pop-up, mời bạn qua nhận hoặc đang giao"
                            OrderStatus.COMPLETED -> "Đơn hàng đã hoàn tất giao nhận rau sạch"
                            OrderStatus.CANCELLED -> "Đơn hàng này đã được hủy"
                            OrderStatus.ISSUE_PENDING -> "Hụt sản lượng thu hoạch thực tế, đang xử lý đền bù"
                        }
                        Text(
                            text = "💡 $descriptiveStatus",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Medium,
                            color = if (order.status == OrderStatus.CANCELLED || order.status == OrderStatus.ISSUE_PENDING) StatusIssue else FarmGreenPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Tiến trình (Stepper)
                        OrderProgressStepper(currentStatus = order.status)

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 1.dp, color = FarmOutline)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Khu vực mã nhận hàng: "Mã nhận tại Pop-up", mã PU-718, nút "Xem QR"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FarmGreenContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Mã nhận tại Pop-up",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = FarmTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = order.pickupCode,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FarmTextPrimary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { selectedOrderForQr = order },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = FarmGreenPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Xem QR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. Danh sách sản phẩm (Tên, Số lượng, Đơn vị, Giá; tên xuống dòng tự nhiên, dấu phân cách nhẹ)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items.forEachIndexed { idx, item ->
                                val cleanName = item.productName
                                    .replace("[DỮ LIỆU MẪU]", "")
                                    .replace("DỮ LIỆU MẪU", "")
                                    .trim()

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "$cleanName  × ${"%.1f".format(item.quantity).replace(".0", "")} ${item.unit}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                            color = FarmTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatVnd(item.lineTotal),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = FarmTextPrimary,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }

                                    // Product Review / Rating Action Block
                                    if (order.status == OrderStatus.COMPLETED) {
                                        val review = reviews.find { it.orderId == order.id && it.productId == item.productId }
                                        if (review != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp, bottom = 4.dp)
                                                    .background(FarmGreenContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⭐", fontSize = 12.sp)
                                                Text(
                                                    text = " ${review.rating}/5",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = FarmHarvestGold,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    text = "\"${review.comment}\"",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = FarmTextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                TextButton(
                                                    onClick = { 
                                                        selectedReviewProductAndOrder = Triple(item.productId, order.id, cleanName)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(12.dp), tint = FarmGreenPrimary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Viết đánh giá", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (idx < items.size - 1) {
                                    HorizontalDivider(thickness = 1.dp, color = FarmOutline.copy(alpha = 0.6f))
                                }
                            }
                        }

                        // Shortage compensation alert
                        if (order.status == OrderStatus.ISSUE_PENDING || order.shortageCompensationAction != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ Xử lý hụt sản lượng thu hoạch:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = order.shortageCompensationAction ?: "Nông hộ đang rà soát lại lượng rau, Quản trị viên sẽ cập nhật đền bù công khai cho bạn.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 1.dp, color = FarmOutline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 6. Điểm nhận và ngày nhận
                        val deliveryDateStr = session?.let { formatDate(it.deliveryDateEpochMs) } ?: "Đợt cắt sắp tới"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (order.deliveryType == DeliveryType.POPUP_PICKUP) {
                                    "Nhận tại: ${popup?.name ?: "Trạm Pop-up"} • Ngày nhận: $deliveryDateStr"
                                } else {
                                    "Giao tận nhà: ${order.homeAddress ?: ""} • Ngày nhận: $deliveryDateStr"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = FarmTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 7. Tổng tiền và thao tác phụ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tổng thanh toán",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = FarmTextSecondary
                                )
                                Text(
                                    text = formatVnd(order.totalAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = FarmTextPrimary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onTrackOrder(order.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(38.dp).testTag("btn_track_order_${order.orderCode}")
                                ) {
                                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Theo dõi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                val successAttempt = paymentAttempts.find { it.orderId == order.id && it.status == PaymentAttemptStatus.SUCCESS }
                                val pendingAttempt = paymentAttempts.find { it.orderId == order.id && it.status == PaymentAttemptStatus.PENDING }

                                if (successAttempt != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FarmGreenContainer
                                    ) {
                                        Text(
                                            text = "Đã trả trực tuyến",
                                            color = FarmGreenDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (pendingAttempt != null) {
                                    Button(
                                        onClick = { onViewPayment(pendingAttempt.paymentCode) },
                                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Thanh toán ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (order.status == OrderStatus.PLACED) {
                                    var showProviderSelectDialog by remember { mutableStateOf(false) }

                                    Button(
                                        onClick = { showProviderSelectDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Thanh toán Online", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (showProviderSelectDialog) {
                                        var selectedProvider by remember { mutableStateOf(PaymentProviderType.VNPAY) }
                                        var selectedDeposit by remember { mutableStateOf(false) }

                                        AlertDialog(
                                            onDismissRequest = { showProviderSelectDialog = false },
                                            title = { Text("Chọn Cổng Thanh Toán Online", color = FarmGreenDark, fontWeight = FontWeight.Bold) },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Chọn phương thức trả trước:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (!selectedDeposit) FarmGreenContainer else FarmSurfaceVariant,
                                                            modifier = Modifier.weight(1f).clickable { selectedDeposit = false }
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("Trả 100%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (!selectedDeposit) FarmGreenDark else Color.DarkGray)
                                                            }
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (selectedDeposit) FarmGoldContainer else FarmSurfaceVariant,
                                                            modifier = Modifier.weight(1f).clickable { selectedDeposit = true }
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("Đặt Cọc 30%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedDeposit) FarmGoldOnContainer else Color.DarkGray)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("Chọn ví / ngân hàng thanh toán:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    PaymentProviderType.values().forEach { provider ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (selectedProvider == provider) FarmGreenContainer.copy(alpha = 0.6f) else Color.Transparent)
                                                                .clickable { selectedProvider = provider }
                                                                .padding(6.dp)
                                                        ) {
                                                            RadioButton(
                                                                selected = selectedProvider == provider,
                                                                onClick = { selectedProvider = provider },
                                                                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(provider.labelVi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        showProviderSelectDialog = false
                                                        onPayOnline(order.id, selectedProvider, selectedDeposit)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                                                ) {
                                                    Text("Thanh toán")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showProviderSelectDialog = false }) {
                                                    Text("Hủy")
                                                }
                                            }
                                        )
                                    }
                                }

                                if (isCancellable) {
                                    OutlinedButton(
                                        onClick = { orderToCancel = order },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusIssue),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusIssue.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text("Hủy đơn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (order.status == OrderStatus.LOCKED || order.status == OrderStatus.PREPARING) {
                                    Text(
                                        text = "Đã khóa",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = StatusLocked,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for cancelling order
    if (orderToCancel != null) {
        val o = orderToCancel!!
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Hủy Đặt Trước?") },
            text = {
                Text("Bạn có chắc chắn muốn hủy đơn hàng ${o.orderCode}? Vì phiên vẫn đang mở, số kg bạn đặt sẽ được hoàn trả lại cho người khác đặt.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(o.id)
                        orderToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Đồng ý hủy")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) {
                    Text("Giữ lại đơn")
                }
            }
        )
    }

    // QR Pickup Dialog
    if (selectedOrderForQr != null) {
        val o = selectedOrderForQr!!
        AlertDialog(
            onDismissRequest = { selectedOrderForQr = null },
            title = { Text("Mã Nhận Hàng Pop-up") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StylizedQrCode(
                        codeText = o.pickupCode,
                        title = "Đưa mã này cho nhân viên trạm Pop-up"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mã đơn: ${o.orderCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedOrderForQr = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Product Review Rating Dialog
    if (selectedReviewProductAndOrder != null) {
        val (productId, orderId, productName) = selectedReviewProductAndOrder!!
        AddReviewDialog(
            productName = productName,
            onDismiss = { selectedReviewProductAndOrder = null },
            onSubmit = { rating, comment ->
                onSubmitReview(productId, orderId, rating, comment)
            }
        )
    }
}

@Composable
fun AddReviewDialog(
    productName: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đánh giá sản phẩm", fontWeight = FontWeight.Bold, color = FarmGreenDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = productName,
                    fontWeight = FontWeight.SemiBold,
                    color = FarmGreenPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Chọn số sao đánh giá:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star sao",
                                tint = if (star <= rating) FarmHarvestGold else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét của bạn") },
                    placeholder = { Text("Rau muống non, giòn ngon, đóng gói sạch sẽ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FarmGreenDark,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(rating, comment)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                enabled = comment.isNotBlank()
            ) {
                Text("Gửi nhận xét")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
