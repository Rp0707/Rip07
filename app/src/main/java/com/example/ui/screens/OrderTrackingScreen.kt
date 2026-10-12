package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.data.model.DeliveryType
import com.example.data.model.OrderStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatVnd
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    sessions: List<PreOrderSessionEntity>,
    popups: List<PopUpLocationEntity>,
    farmers: List<FarmerProfileEntity>,
    lots: List<ProductLotEntity> = emptyList(),
    initialOrderId: Long? = null,
    onBack: () -> Unit,
    onNavigateToAiChat: (String) -> Unit = {},
    onNavigateToTrace: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedOrderId by remember(initialOrderId, orders) {
        mutableStateOf(initialOrderId ?: orders.firstOrNull()?.id)
    }

    val selectedOrder = remember(selectedOrderId, orders) {
        orders.find { it.id == selectedOrderId }
    }

    val itemsInOrder = remember(selectedOrderId, orderItems) {
        orderItems.filter { it.orderId == selectedOrderId }
    }

    val session = remember(selectedOrder, sessions) {
        sessions.find { it.id == selectedOrder?.sessionId }
    }

    val popupLocation = remember(selectedOrder, popups) {
        popups.find { it.id == selectedOrder?.popupLocationId }
    }

    var showQrDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theo Dõi Hành Trình Rau Sạch", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_tracking")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmBackground,
                    titleContentColor = FarmGreenDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FarmBackground)
        ) {
            DemoDataBanner()

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = FarmEarthSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không tìm thấy dữ liệu đơn hàng đặt trước",
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Vui lòng đặt mua rau tại Sàn Bình Mỹ trước khi theo dõi hành trình vận chuyển.",
                            fontSize = 13.sp,
                            color = FarmTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        )
                    }
                }
            } else {
                // Main tracking content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dropdown selector if multiple orders exist
                    if (orders.size > 1) {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDropdown = true },
                                colors = CardDefaults.outlinedCardColors(containerColor = FarmSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Đang xem đơn: ${selectedOrder?.orderCode ?: "Chọn đơn hàng"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = FarmTextPrimary
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FarmTextSecondary)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(FarmSurface)
                            ) {
                                orders.forEach { o ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(o.orderCode, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                                                Text("Đặt ngày: ${formatDateTime(o.createdAtEpochMs)}", fontSize = 11.sp, color = FarmTextSecondary)
                                            }
                                        },
                                        onClick = {
                                            selectedOrderId = o.id
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    selectedOrder?.let { order ->
                        // 1. Order summary card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FarmSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Mã Đơn Hàng", fontSize = 12.sp, color = FarmTextSecondary)
                                        Text(order.orderCode, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FarmTextPrimary)
                                    }
                                    OrderStatusBadge(status = order.status)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Thời gian đặt:", fontSize = 12.sp, color = FarmTextSecondary)
                                    Text(formatDateTime(order.createdAtEpochMs), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = FarmTextPrimary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Điểm nhận rau:", fontSize = 12.sp, color = FarmTextSecondary)
                                    Text(
                                        text = if (order.deliveryType == DeliveryType.POPUP_PICKUP) {
                                            popupLocation?.name ?: "Trạm Pop-up Bình Mỹ"
                                        } else {
                                            "Giao tận nhà: ${order.homeAddress ?: ""}"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FarmTextPrimary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 200.dp)
                                    )
                                }
                            }
                        }

                        // 2. Beautiful Simulated Supply Chain Shipping Map Sơ Đồ
                        SupplyChainVisualMap(status = order.status)

                        // 3. 6-Stages Progress Timeline
                        OrderDetailedTimeline(order = order)

                        // 4. Products list in this order
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FarmSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Nông Sản Đặt Thu Hoạch", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
                                Spacer(modifier = Modifier.height(10.dp))

                                itemsInOrder.forEachIndexed { index, item ->
                                    val cleanName = item.productName
                                        .replace("[DỮ LIỆU MẪU]", "")
                                        .replace("DỮ LIỆU MẪU", "")
                                        .trim()

                                    val lot = lots.find { it.productId == item.productId }
                                    val farmer = farmers.find { it.id == lot?.farmerId }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(cleanName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmTextPrimary)
                                                Text(
                                                    text = "Canh tác: ${farmer?.householdName ?: "Nông hộ Bình Mỹ"}",
                                                    fontSize = 11.sp,
                                                    color = FarmTextSecondary
                                                )
                                                if (lot != null) {
                                                    Text(
                                                        text = "Lô VietGAP: ${lot.lotCode}",
                                                        fontSize = 10.sp,
                                                        color = FarmGreenPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable { onNavigateToTrace(lot.lotCode) }
                                                            .padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${"%.1f".format(item.quantity).replace(".0", "")} ${item.unit} • ${formatVnd(item.lineTotal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = FarmTextPrimary
                                            )
                                        }

                                        if (index < itemsInOrder.size - 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(thickness = 0.5.dp, color = FarmOutline.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Action Buttons (QR, AI Recipes, Trace)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showQrDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_view_qr_tracking"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Xem QR & Mã Nhận Hàng (PU-${order.pickupCode})", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val itemNames = itemsInOrder.joinToString(", ") { it.productName }
                                    onNavigateToAiChat("Tôi có đơn hàng chứa $itemNames từ Sàn Bình Mỹ, hãy tư vấn cho tôi các công thức nấu ăn ngon và cách bảo quản rau muống tươi lâu!")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_ai_recipe_tracking"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmGreenPrimary)
                            ) {
                                Text("🤖 Hỏi Trợ Lý AI: Tư Vấn Chế Biến & Bảo Quản", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog && selectedOrder != null) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Đóng")
                }
            },
            title = {
                Text(
                    text = "Mã QR Giao Nhận Rau Sạch",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Vui lòng đưa mã này cho nhân viên tại trạm Pop-up:",
                        fontSize = 13.sp,
                        color = FarmTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw a beautifully stylized QR placeholder
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .border(2.dp, FarmGreenPrimary, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            tint = FarmGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "MÃ NHẬN HÀNG POP-UP:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextSecondary
                    )
                    Text(
                        text = selectedOrder.pickupCode,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }
        )
    }
}

/**
 * Supply chain graphic map from farmland to pop-up hub
 */
@Composable
fun SupplyChainVisualMap(status: OrderStatus) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FarmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text("Hành Trình Giao Nhận Rau Muống Nước", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
            Text("Sơ đồ chuỗi cung ứng trực tiếp từ đồng ruộng Củ Chi", fontSize = 11.sp, color = FarmTextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // supply chain path canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val y = h / 2f

                    // Segment points
                    val p1 = Offset(w * 0.15f, y) // Farm
                    val p2 = Offset(w * 0.45f, y) // VietGAP Check
                    val p3 = Offset(w * 0.85f, y) // Pop-up

                    // Status calculations
                    val step1Done = status != OrderStatus.AWAITING_CONFIRMATION && status != OrderStatus.CANCELLED
                    val step2Done = status == OrderStatus.READY_FOR_PICKUP_OR_DELIVERING || status == OrderStatus.COMPLETED

                    // Segment 1 (Ruộng -> Trạm Thẩm Định)
                    drawLine(
                        color = if (step1Done) FarmGreenPrimary else Color(0xFFE0E0E0),
                        start = p1,
                        end = p2,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Segment 2 (Trạm Thẩm Định -> Trạm Pop-up)
                    drawLine(
                        color = if (step2Done) FarmGreenPrimary else Color(0xFFE0E0E0),
                        start = p2,
                        end = p3,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // UI Nodes Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SupplyChainMapNode(
                        icon = "🥬",
                        label = "Ruộng Rau\nBình Mỹ",
                        isCompleted = true
                    )
                    SupplyChainMapNode(
                        icon = "🔍",
                        label = "Kiểm Định\nVietGAP",
                        isCompleted = status != OrderStatus.AWAITING_CONFIRMATION && status != OrderStatus.PLACED && status != OrderStatus.CANCELLED
                    )
                    SupplyChainMapNode(
                        icon = "📦",
                        label = "Tủ Mát\nPop-up",
                        isCompleted = status == OrderStatus.READY_FOR_PICKUP_OR_DELIVERING || status == OrderStatus.COMPLETED
                    )
                }
            }
        }
    }
}

@Composable
fun SupplyChainMapNode(
    icon: String,
    label: String,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isCompleted) FarmGreenContainer else Color(0xFFF3F4F6),
                    shape = CircleShape
                )
                .border(
                    2.dp,
                    if (isCompleted) FarmGreenPrimary else Color(0xFFD1D5DB),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCompleted) FarmTextPrimary else Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

/**
 * Highly granular visual progress bar showing the 6 pre-order journey steps
 */
@Composable
fun OrderDetailedTimeline(order: OrderEntity) {
    val currentStepIndex = when (order.status) {
        OrderStatus.AWAITING_CONFIRMATION -> 0
        OrderStatus.PLACED -> 1
        OrderStatus.LOCKED -> 2
        OrderStatus.PREPARING -> 3
        OrderStatus.READY_FOR_PICKUP_OR_DELIVERING -> 4
        OrderStatus.COMPLETED -> 5
        OrderStatus.CANCELLED -> -1
        OrderStatus.ISSUE_PENDING -> 3
    }

    val stages = listOf(
        TimelineStage("📝", "Đã ghi nhận pre-order", "Khách hàng gửi yêu cầu đặt thành công", "BM-SYS"),
        TimelineStage("🔒", "Đã chốt phiên thu hoạch", "Tổng số kg gom theo hộ đã khóa, không thể hủy", "Admin"),
        TimelineStage("🥬", "Đang thu hoạch sáng sớm", "Nông dân cắt rau trực tiếp lúc 04:30 sáng mát", "Nông Hộ"),
        TimelineStage("🔍", "Đã kiểm định an toàn", "Dán nhãn QR VietGAP chống giả mạo vùng trồng", "Kiểm Định"),
        TimelineStage("📦", "Đã cập bến Pop-up", "Rau sạch có mặt tại tủ mát trạm nhận", "Pop-up"),
        TimelineStage("✅", "Hoàn tất giao nhận", "Mã PU nhận diện thành công, khép lại hành trình", "Hệ Thống")
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FarmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Tiến Trình Hành Trình Rau Sạch", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
            Spacer(modifier = Modifier.height(14.dp))

            if (order.status == OrderStatus.CANCELLED) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Đơn hàng này đã bị hủy", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 13.sp)
                            Text("Các tiến trình gieo trồng và thu hoạch đã dừng lại.", color = Color(0xFF7F1D1D), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                stages.forEachIndexed { idx, stage ->
                    val isDone = idx < currentStepIndex
                    val isActive = idx == currentStepIndex
                    val isUpcoming = idx > currentStepIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Node Line Drawing Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = when {
                                            isDone -> FarmGreenPrimary
                                            isActive -> Color(0xFF6D28D9)
                                            else -> Color(0xFFE5E7EB)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text(
                                        text = stage.emoji,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (idx < stages.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(38.dp)
                                        .background(
                                            if (idx < currentStepIndex) FarmGreenPrimary else Color(0xFFE5E7EB)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Text content Column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stage.title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = when {
                                    isActive -> Color(0xFF6D28D9)
                                    isDone -> FarmTextPrimary
                                    else -> Color.Gray
                                }
                            )
                            Text(
                                text = stage.desc,
                                fontSize = 11.sp,
                                color = if (isActive || isDone) FarmTextSecondary else Color.LightGray,
                                lineHeight = 14.sp
                            )
                            if (isDone || isActive) {
                                Text(
                                    text = "Đồng hành bởi: ${stage.by} • Hoàn tất",
                                    fontSize = 9.sp,
                                    color = if (isActive) Color(0xFF6D28D9).copy(alpha = 0.8f) else FarmGreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TimelineStage(
    val emoji: String,
    val title: String,
    val desc: String,
    val by: String
)

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (text, color, containerColor) = when (status) {
        OrderStatus.AWAITING_CONFIRMATION -> Triple("Chờ xác nhận", Color(0xFFE65100), Color(0xFFFFF3E0))
        OrderStatus.PLACED -> Triple("Đơn đã đặt", FarmGreenPrimary, FarmGreenContainer)
        OrderStatus.LOCKED -> Triple("Đã khóa", Color(0xFFF57F17), Color(0xFFFFF9C4))
        OrderStatus.PREPARING -> Triple("Đang chuẩn bị", Color(0xFF0277BD), Color(0xFFE1F5FE))
        OrderStatus.READY_FOR_PICKUP_OR_DELIVERING -> Triple("Đã đến Pop-up", Color(0xFF388E3C), Color(0xFFE8F5E9))
        OrderStatus.COMPLETED -> Triple("Hoàn tất", Color(0xFF1B5E20), Color(0xFFC8E6C9))
        OrderStatus.CANCELLED -> Triple("Đã hủy", Color(0xFFC62828), Color(0xFFFFEBEE))
        OrderStatus.ISSUE_PENDING -> Triple("Sự cố hụt hàng", Color(0xFFD32F2F), Color(0xFFFFEBEE))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
