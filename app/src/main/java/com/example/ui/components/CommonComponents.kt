package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.OrderStatus
import com.example.data.model.SessionStatus
import com.example.data.model.UserRole
import com.example.security.BankSecurityVault
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DemoDataBanner(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("demo_data_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FarmGoldContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🌾",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "DỮ LIỆU MẪU THỬ NGHIỆM • BÌNH MỸ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmGoldOnContainer
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Xem thông tin bối cảnh",
                    tint = FarmGoldOnContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = FarmGoldOnContainer.copy(alpha = 0.2f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Bối cảnh dự án: Xã Bình Mỹ hiện có 188 hộ sản xuất rau muống (khoảng 208,23 ha canh tác), trong đó 59/188 hộ đã được cấp chứng nhận VietGAP (tính đến 11/2024).\n" +
                                "• Toàn bộ nông hộ, sản phẩm, mã lô và giao dịch trong ứng dụng là dữ liệu mô phỏng nhằm phục vụ kiểm thử trọn luồng vận hành theo phiên đặt trước.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmGoldOnContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadgeHeader(
    currentRole: UserRole,
    currentUserName: String,
    onOpenRoleSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("role_badge_header"),
        color = FarmGreenContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FarmGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentRole) {
                            UserRole.BUYER -> Icons.Default.ShoppingBag
                            UserRole.FARMER -> Icons.Default.Agriculture
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            UserRole.POPUP_STAFF -> Icons.Default.Storefront
                            UserRole.VERIFIER -> Icons.Default.Verified
                            UserRole.SUPER_ADMIN -> Icons.Default.AdminPanelSettings
                        },
                        contentDescription = "Vai trò",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentRole.labelVi,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        )
                        Text(
                            text = " • Đang đăng nhập",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmTextSecondary
                        )
                    }
                    Text(
                        text = currentUserName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenRoleSelector,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(FarmGreenPrimary)),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("btn_switch_role")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Đổi vai trò",
                    modifier = Modifier.size(16.dp),
                    tint = FarmGreenPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Đổi vai trò",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenPrimary
                )
            }
        }
    }
}

@Composable
fun OrderStatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.AWAITING_CONFIRMATION -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Chờ xác nhận")
        OrderStatus.PLACED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Đã đặt trước")
        OrderStatus.LOCKED -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Đã khóa phiên")
        OrderStatus.PREPARING -> Triple(Color(0xFFE1F5FE), Color(0xFF0277BD), "Đang chuẩn bị")
        OrderStatus.READY_FOR_PICKUP_OR_DELIVERING -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "Đã đến Pop-up")
        OrderStatus.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "Hoàn tất")
        OrderStatus.CANCELLED -> Triple(Color(0xFFEEEEEE), Color(0xFF616161), "Đã hủy")
        OrderStatus.ISSUE_PENDING -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Cần xử lý hụt hàng")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SessionStatusBadge(
    status: SessionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        SessionStatus.OPEN -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Đang mở đặt trước")
        SessionStatus.LOCKED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Đã khóa đơn")
        SessionStatus.HARVESTING -> Triple(Color(0xFFE1F5FE), Color(0xFF0277BD), "Đang thu hoạch")
        SessionStatus.DISTRIBUTING -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "Đang điều phối Pop-up")
        SessionStatus.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "Đã hoàn tất")
        SessionStatus.CANCELLED -> Triple(Color(0xFFEEEEEE), Color(0xFF616161), "Phiên đã hủy")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun OrderProgressStepper(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OrderStatus.PLACED to "Đã đặt",
        OrderStatus.LOCKED to "Đã khóa",
        OrderStatus.PREPARING to "Thu hoạch",
        OrderStatus.READY_FOR_PICKUP_OR_DELIVERING to "Đến Pop-up",
        OrderStatus.COMPLETED to "Hoàn tất"
    )

    val currentStepIndex = when (currentStatus) {
        OrderStatus.AWAITING_CONFIRMATION -> 0
        OrderStatus.PLACED -> 0
        OrderStatus.LOCKED -> 1
        OrderStatus.PREPARING -> 2
        OrderStatus.READY_FOR_PICKUP_OR_DELIVERING -> 3
        OrderStatus.COMPLETED -> 4
        OrderStatus.CANCELLED -> -1
        OrderStatus.ISSUE_PENDING -> 2
    }

    if (currentStatus == OrderStatus.CANCELLED) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đơn hàng này đã hủy trước hạn khóa đơn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (stepStatus, stepLabel) ->
                val isCompleted = index < currentStepIndex
                val isCurrent = index == currentStepIndex

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> FarmGreenPrimary
                                isCompleted -> FarmGreenDark
                                else -> Color(0xFFE0E0E0)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (index < currentStepIndex) FarmGreenPrimary else Color(0xFFE0E0E0)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, (_, stepLabel) ->
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (index <= currentStepIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    fontWeight = if (index == currentStepIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(58.dp)
                )
            }
        }
    }
}

/**
 * Stylized visual QR simulation for traceability and pickup checking
 */
@Composable
fun StylizedQrCode(
    codeText: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .border(2.dp, FarmGreenPrimary, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Represent QR matrix pattern
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    repeat(7) { rowIdx ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(7) { colIdx ->
                                val isCorner = (rowIdx < 2 && (colIdx < 2 || colIdx > 4)) ||
                                        (rowIdx > 4 && colIdx < 2)
                                val isFill = isCorner || ((rowIdx * 3 + colIdx * 5 + codeText.hashCode()) % 2 == 0)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if (isFill) FarmGreenDark else Color(0xFFF1F5ED),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = FarmTextSecondary
            )
            Text(
                text = codeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FarmGreenDark
            )
        }
    }
}

fun formatVnd(amount: Double): String {
    return "%,.0f đ".format(amount)
}

fun formatDate(epochMs: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMs))
}

fun formatDateTime(epochMs: Long): String {
    return SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(epochMs))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectBookingDialog(
    product: ProductEntity,
    activeSession: PreOrderSessionEntity?,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Double, name: String, phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val remaining = (product.originalEstimateKg - product.reservedKg).coerceAtLeast(0.0)
    val price = product.currentPricePerUnit
    val quantity = quantityText.toDoubleOrNull() ?: 1.0
    val totalEstimated = quantity * price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📝", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text("Phiếu Đặt Hàng Nhanh", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Thu hoạch trực tiếp từ nông hộ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Product Summary Card (using fully adaptive primary container)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp)
                        Text("Giá bán dự kiến: ${formatVnd(price)} / ${product.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Còn nhận đặt: ${"%.1f".format(remaining)} ${product.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        if (activeSession != null) {
                            Text("Ngày thu hoạch: ${formatDate(activeSession.deliveryDateEpochMs)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmHarvestGold)
                        }
                    }
                }

                // Input Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; errorMessage = null },
                    label = { Text("Tên người đặt *") },
                    placeholder = { Text("Ví dụ: Anh Tuấn") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_booking_name"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                )

                // Input Phone
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it; errorMessage = null },
                    label = { Text("Số điện thoại liên hệ *") },
                    placeholder = { Text("Ví dụ: 0908123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_booking_phone"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                )

                // Input Quantity
                Text("Số lượng cần đặt (${product.unit}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val q = (quantityText.toDoubleOrNull() ?: 1.0) - 1.0
                            if (q >= 0.5) quantityText = if (q % 1.0 == 0.0) q.toInt().toString() else "%.1f".format(q)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Giảm")
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            errorMessage = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_booking_qty"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(
                        onClick = {
                            val q = (quantityText.toDoubleOrNull() ?: 0.0) + 1.0
                            if (q <= remaining) quantityText = if (q % 1.0 == 0.0) q.toInt().toString() else "%.1f".format(q)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tăng")
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                // Estimated Cost summary (using fully adaptive secondary container)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thành tiền ước tính:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(formatVnd(totalEstimated), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isBlank()) {
                        errorMessage = "Vui lòng nhập tên người đặt."
                        return@Button
                    }
                    if (phoneInput.isBlank() || phoneInput.length < 9) {
                        errorMessage = "Vui lòng nhập số điện thoại hợp lệ."
                        return@Button
                    }
                    val q = quantityText.toDoubleOrNull()
                    if (q == null || q <= 0.0) {
                        errorMessage = "Vui lòng nhập số lượng hợp lệ lớn hơn 0."
                        return@Button
                    }
                    if (q > remaining) {
                        errorMessage = "Vui lòng không vượt quá sản lượng còn lại ($remaining)."
                        return@Button
                    }
                    onSubmit(q, nameInput.trim(), phoneInput.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                modifier = Modifier.testTag("btn_confirm_direct_booking")
            ) {
                Text("Xác Nhận Đặt Trước")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bỏ")
            }
        }
    )
}

@Composable
fun TechcombankVietQrCode(
    paymentCode: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    val bankTitle = BankSecurityVault.getBankTitle()
    val accountNumber = BankSecurityVault.getAccountNumber()
    val accountName = BankSecurityVault.getAccountName()
    val vietQrImageUrl = BankSecurityVault.buildVietQrUrl(paymentCode, amount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header matching user's uploaded image.png
            Text(
                text = "QUÉT MÃ QR THANH TOÁN AN TOÀN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Chuyển khoản QR Ngân hàng",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TECHCOMBANK Logo Header matching image.png
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bankTitle,
                    color = Color(0xFFE31B23), // Techcombank Red
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "❮❯",
                    color = Color(0xFFE31B23),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real VietQR Image with Canvas Fallback
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.SubcomposeAsyncImage(
                    model = vietQrImageUrl,
                    contentDescription = "Mã VietQR Ngân hàng",
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFE31B23),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    error = {
                        // High-contrast sharp QR Canvas Fallback
                        HighResQrCanvas(paymentCode = paymentCode)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VIETQR and napas 247 logos footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // VIETQR Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "V", color = Color(0xFFE31B23), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(text = "IET", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "QR", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // napas 247 Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "napas",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "247", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Bank Transfer Details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đơn vị thụ hưởng: ${accountName.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = "Số tài khoản: $accountNumber ($bankTitle)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Nội dung chuyển khoản: $paymentCode",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
                Text(
                    text = "Số tiền thanh toán: ${formatVnd(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE31B23),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Security & Anti-Tamper Protection Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mã Nguồn Ngân Hàng Được Mã Hóa & Khóa Bảo Mật",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Thông tin tài khoản nhận tiền HTX Bình Mỹ đã được mã hóa XOR & kiểm tra toàn vẹn SHA-256 trực tiếp trong mã nguồn. Hacker không thể can thiệp hoặc thay đổi tài khoản nhận tiền.",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D),
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighResQrCanvas(paymentCode: String) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val sizePx = size.width
        val gridCount = 25
        val cellSize = sizePx / gridCount

        // Background
        drawRect(Color.White)

        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                // Top-Left Finder
                val isTopLeft = (r < 7 && c < 7)
                // Top-Right Finder
                val isTopRight = (r < 7 && c >= gridCount - 7)
                // Bottom-Left Finder
                val isBottomLeft = (r >= gridCount - 7 && c < 7)

                val isFinder = isTopLeft || isTopRight || isBottomLeft

                val shouldFill = if (isFinder) {
                    val localR = when {
                        isTopLeft -> r
                        isTopRight -> r
                        else -> r - (gridCount - 7)
                    }
                    val localC = when {
                        isTopLeft -> c
                        isTopRight -> c - (gridCount - 7)
                        else -> c
                    }
                    (localR == 0 || localR == 6 || localC == 0 || localC == 6) ||
                            (localR in 2..4 && localC in 2..4)
                } else {
                    val hashVal = (r * 13 + c * 17 + paymentCode.hashCode()).hashCode()
                    (hashVal % 3 == 0) || ((r + c) % 5 == 0)
                }

                if (shouldFill) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                    )
                }
            }
        }
    }
}

