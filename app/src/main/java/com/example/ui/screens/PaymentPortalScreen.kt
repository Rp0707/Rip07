package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.StateFlow
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.StylizedQrCode
import com.example.ui.components.TechcombankVietQrCode
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatVnd
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPortalScreen(
    paymentAttempts: StateFlow<List<PaymentAttemptEntity>>,
    orders: StateFlow<List<OrderEntity>>,
    orderItems: StateFlow<List<OrderItemEntity>>,
    refunds: StateFlow<List<RefundEntity>>,
    onBack: () -> Unit,
    onSubmitReview: (Long, Long, Int, String) -> Unit,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val activeCode by viewModel.activePaymentAttemptCode.collectAsStateWithLifecycle()
    val allAttempts by paymentAttempts.collectAsStateWithLifecycle()
    val allOrders by orders.collectAsStateWithLifecycle()
    val allItems by orderItems.collectAsStateWithLifecycle()
    val allRefunds by refunds.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Find active payment attempt
    val activeAttempt = remember(allAttempts, activeCode) {
        allAttempts.find { it.paymentCode == activeCode }
    }

    // Find related order
    val order = remember(allOrders, activeAttempt) {
        allOrders.find { it.id == activeAttempt?.orderId }
    }

    // Find related order items
    val items = remember(allItems, order) {
        allItems.filter { it.orderId == order?.id }
    }

    // Timer calculation
    var timeLeftSeconds by remember { mutableStateOf(900) } // Default 15 mins
    LaunchedEffect(activeAttempt) {
        if (activeAttempt != null) {
            while (true) {
                val diffMs = activeAttempt.expiresAtEpochMs - System.currentTimeMillis()
                timeLeftSeconds = (diffMs / 1000).toInt().coerceAtLeast(0)
                if (timeLeftSeconds <= 0) {
                    if (activeAttempt.status == PaymentAttemptStatus.PENDING) {
                        viewModel.simulatePaymentExpiration(activeAttempt.paymentCode)
                    }
                    break
                }
                delay(1000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cổng Thanh Toán Trực Tuyến", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = FarmGreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmBackground,
                    titleContentColor = FarmGreenDark
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (activeAttempt == null || order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(FarmBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Không có phiên thanh toán trực tuyến nào đang hoạt động.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FarmTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                    ) {
                        Text("Quay lại danh sách đơn hàng")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(FarmBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DemoDataBanner()

                // Header status badge
                PaymentStatusCard(status = activeAttempt.status, attempt = activeAttempt, order = order)

                // Main Info Group
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mã hóa đơn: ${order.orderCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FarmGreenContainer
                            ) {
                                Text(
                                    text = if (activeAttempt.isDepositAttempt) "Trả Cọc 30%" else "Thanh toán 100%",
                                    color = FarmGreenDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mã thanh toán: ${activeAttempt.paymentCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = FarmOutline)

                        // Products list
                        Text(
                            text = "Chi tiết nông sản đặt trước:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${item.productName} (${"%.1f".format(item.quantity).replace(".0", "")} ${item.unit})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatVnd(item.lineTotal),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmTextPrimary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = FarmOutline)

                        // Order totals details
                        PriceDetailRow(label = "Tạm tính tiền hàng:", value = formatVnd(order.totalAmount - order.deliveryFee))
                        PriceDetailRow(label = "Phí giao hàng:", value = if (order.deliveryFee == 0.0) "Miễn phí" else formatVnd(order.deliveryFee))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tổng giá trị đơn hàng:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary
                            )
                            Text(
                                formatVnd(order.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmTextPrimary
                            )
                        }

                        if (activeAttempt.isDepositAttempt) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(FarmGoldContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Tiền đặt cọc (30%):",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmGoldOnContainer
                                    )
                                    Text(
                                        "Còn lại sau thu hoạch: ${formatVnd(activeAttempt.remainingAmount)}",
                                        fontSize = 10.sp,
                                        color = FarmGoldOnContainer
                                    )
                                }
                                Text(
                                    formatVnd(activeAttempt.depositAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmHarvestGold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(FarmGreenContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Số tiền cần thanh toán:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                                Text(
                                    formatVnd(activeAttempt.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmGreenDark
                                )
                            }
                        }
                    }
                }

                // Delivery information
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Thông Tin Vận Chuyển & Nhận Hàng",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = FarmGreenDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DeliveryInfoRow(icon = Icons.Default.Person, label = "Người nhận:", value = order.receiverName)
                        DeliveryInfoRow(icon = Icons.Default.Phone, label = "Số điện thoại:", value = order.receiverPhone)
                        DeliveryInfoRow(icon = Icons.Default.Place, label = "Địa chỉ nhận:", value = order.homeAddress ?: "Nhận tại điểm Pop-up")
                        DeliveryInfoRow(icon = Icons.Default.Agriculture, label = "Ngày thu hoạch:", value = "Rạng sáng ngày giao hàng dự kiến")
                        DeliveryInfoRow(icon = Icons.Default.CalendarToday, label = "Ngày giao dự kiến:", value = formatDateTime(order.createdAtEpochMs + 2 * 24 * 3600 * 1000L).take(10))

                        if (!order.buyerNote.isNullOrBlank()) {
                            DeliveryInfoRow(icon = Icons.Default.Notes, label = "Ghi chú:", value = order.buyerNote)
                        }
                    }
                }

                // Cancellation & Refund Policy
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = FarmGreenDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chính sách hủy đơn & đặt cọc:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = FarmGreenDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Hủy đơn thoải mái miễn phí trước khi phiên đặt trước khóa sổ (18:00 Thứ Năm hàng tuần).\n" +
                                   "• Sau khi khóa sổ, nông hộ đã chốt danh sách gặt cắt, không hỗ trợ hoàn tiền cọc/đơn hàng tự ý trừ trường hợp nông hộ gặp rủi ro thiên tai thiếu hụt sản lượng sẽ được hoàn tiền tự động 100%.",
                            fontSize = 11.sp,
                            color = FarmTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // If PENDING, display QR Code & copy options matching exact layout from image.png
                if (activeAttempt.status == PaymentAttemptStatus.PENDING || activeAttempt.status == PaymentAttemptStatus.VERIFYING) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (activeAttempt.provider == PaymentProviderType.BANK_QR) {
                            TechcombankVietQrCode(
                                paymentCode = activeAttempt.paymentCode,
                                amount = activeAttempt.amount
                            )
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "QUÉT MÃ QR THANH TOÁN AN TOÀN",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FarmGreenDark
                                    )
                                    Text(
                                        text = activeAttempt.provider.labelVi,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FarmTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    StylizedQrCode(
                                        codeText = activeAttempt.providerReferenceCode ?: "BinhMyPay",
                                        title = "Mã QR Giao dịch ${activeAttempt.paymentCode}"
                                    )
                                }
                            }
                        }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Countdown clock
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (timeLeftSeconds < 120) Color(0xFFFEE2E2) else FarmGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = if (timeLeftSeconds < 120) Color.Red else FarmGreenDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hiệu lực còn lại: %02d:%02d".format(timeLeftSeconds / 60, timeLeftSeconds % 60),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (timeLeftSeconds < 120) Color.Red else FarmGreenDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Copy instructions
                            Text(
                                text = "Nội dung chuyển khoản hoặc tham chiếu quét mã:",
                                style = MaterialTheme.typography.labelSmall,
                                color = FarmTextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FarmSurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeAttempt.paymentCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = FarmTextPrimary
                                )
                                Text(
                                    text = "Sao chép",
                                    color = FarmGreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(activeAttempt.paymentCode))
                                            scope.launch {
                                                viewModel.showMessage("Đã sao chép nội dung chuyển khoản!")
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons
                            Button(
                                onClick = { viewModel.verifyTransaction(activeAttempt.paymentCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("btn_verify_payment")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tôi Đã Thanh Toán (Xác Minh Giao Dịch)")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Thanh toán sau")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            viewModel.updatePaymentAttemptStatus(activeAttempt.paymentCode, PaymentAttemptStatus.USER_CANCELLED)
                                            viewModel.releaseReservationsForOrder(activeAttempt.orderId)
                                            viewModel.showMessage("Bạn đã hủy giao dịch thanh toán này.")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hủy giao dịch")
                                }
                            }
                        }
                    }
                }

                // Webhook Sandbox Simulator Control Box (Sự kiên IPN kiểm thử bắt buộc của lập trình viên)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛠️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BỘ ĐỐI SOÁT & GIẢ LẬP WEBHOOK (SANDBOX)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        Text(
                            text = "Khu vực kiểm thử dành cho nhà phát triển để xác minh quy trình IPN/Webhook và quản lý ngoại lệ",
                            fontSize = 11.sp,
                            color = FarmTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Simulators buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Success IPN
                            Button(
                                onClick = { viewModel.triggerSimulatedWebhook(activeAttempt.paymentCode, success = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giả lập IPN Webhook: NHẬN TIỀN THÀNH CÔNG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Failed IPN
                            Button(
                                onClick = { viewModel.triggerSimulatedWebhook(activeAttempt.paymentCode, success = false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giả lập IPN Webhook: GIAO DỊCH THẤT BẠI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Double Payment
                            Button(
                                onClick = { viewModel.triggerSimulatedWebhook(activeAttempt.paymentCode, success = true, isDoublePayment = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giả lập IPN Webhook: KHÁCH THANH TOÁN 2 LẦN (DƯ TIỀN)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 4. Late Payment after cancel
                            Button(
                                onClick = { viewModel.triggerSimulatedWebhook(activeAttempt.paymentCode, success = true, isLatePayment = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giả lập IPN Webhook: TIỀN ĐẾN MUỘN (ĐƠN ĐÃ HỦY/HẾT HẠN)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 5. Expiration manual trigger
                            Button(
                                onClick = { viewModel.simulatePaymentExpiration(activeAttempt.paymentCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giả lập: ĐỒNG HỒ ĐẾM NGƯỢC HẾT HẠN GIAO DỊCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

private data class PaymentUiInfo(
    val bgColor: Color,
    val tintColor: Color,
    val titleText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val desc: String
)

@Composable
fun PaymentStatusCard(status: PaymentAttemptStatus, attempt: PaymentAttemptEntity, order: OrderEntity) {
    val (bgColor, tintColor, titleText, icon, desc) = when (status) {
        PaymentAttemptStatus.PENDING -> PaymentUiInfo(
            FarmGoldContainer,
            FarmHarvestGold,
            "Đang chờ thanh toán trực tuyến",
            Icons.Default.HourglassTop,
            "Vui lòng quét mã QR thanh toán để hoàn tất đăng ký suất đặt trước."
        )
        PaymentAttemptStatus.VERIFYING -> PaymentUiInfo(
            Color(0xFFEFF6FF),
            Color(0xFF3B82F6),
            "Đang xác minh giao dịch...",
            Icons.Default.Sync,
            "Hệ thống đang truy vấn số dư ngân hàng và đối soát webhook. Vui lòng chờ..."
        )
        PaymentAttemptStatus.SUCCESS -> PaymentUiInfo(
            FarmGreenContainer,
            FarmGreenDark,
            "Thanh toán trực tuyến thành công! 🎉",
            Icons.Default.CheckCircle,
            "Đơn hàng của bạn đã được xác nhận trả trước thành công. Hãy chờ nhận rau tươi mát lành!"
        )
        PaymentAttemptStatus.FAILED -> PaymentUiInfo(
            Color(0xFFFEE2E2),
            Color(0xFFEF4444),
            "Thanh toán trực tuyến thất bại",
            Icons.Default.Error,
            "Giao dịch đã bị từ chối từ cổng thanh toán. Quý khách vui lòng thử thanh toán lại hoặc chọn ví khác."
        )
        PaymentAttemptStatus.EXPIRED -> PaymentUiInfo(
            Color(0xFFF3F4F6),
            Color(0xFF4B5563),
            "Phiên thanh toán hết hạn",
            Icons.Default.TimerOff,
            "Thời gian hiệu lực 15 phút của mã thanh toán đã kết thúc. Bạn có thể tạo phiên mới từ Lịch sử đơn hàng."
        )
        PaymentAttemptStatus.USER_CANCELLED -> PaymentUiInfo(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            "Giao dịch đã bị hủy",
            Icons.Default.Cancel,
            "Bạn đã chủ động hủy bỏ phiên thanh toán trực tuyến này. Bạn vẫn có thể thực hiện thanh toán lại sau."
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = tintColor
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PriceDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FarmTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = FarmTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeliveryInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FarmGreenPrimary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = FarmTextSecondary,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = FarmTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
