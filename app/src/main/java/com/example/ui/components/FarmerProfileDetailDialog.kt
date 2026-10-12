package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.SessionStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerProfileDetailDialog(
    farmer: FarmerProfileEntity,
    farmerProducts: List<ProductEntity>,
    activeSession: PreOrderSessionEntity?,
    onDismiss: () -> Unit,
    onSelectProduct: (ProductEntity) -> Unit,
    onAddToCart: (Long) -> Unit,
    onDirectBooking: (Long, Double, String, String) -> Unit = { _, _, _, _ -> }
) {
    var selectedProductForBooking by remember { mutableStateOf<ProductEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("farmer_profile_detail_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                TopAppBar(
                    title = {
                        Text(
                            text = "Hồ Sơ Nông Hộ Sạch VietGAP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = FarmGreenDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FarmSurface)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header profile card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = FarmGreenContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (farmer.representativeName.contains("Nữ") || farmer.householdName.contains("Cô") || farmer.householdName.contains("Chị")) "👩‍🌾" else "👨‍🌾",
                                            fontSize = 36.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = farmer.householdName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FarmGreenDark
                                        )
                                        Text(
                                            text = "Đại diện: ${farmer.representativeName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FarmTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "📞 Hotline: ${farmer.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FarmTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = FarmGreenDark
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Chứng nhận VietGAP",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = FarmGoldContainer
                                    ) {
                                        Text(
                                            text = "Quy mô: ${farmer.acreageHa} Héc-ta",
                                            color = FarmGoldOnContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // VietGAP Verification details
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = FarmGreenPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Thông Tin Chứng Nhận An Toàn Thực Phẩm",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FarmGreenDark
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = FarmOutline)

                                ProfileDetailRow(icon = Icons.Default.QrCode, label = "Mã số VietGAP:", value = farmer.vietGapCertCode)
                                ProfileDetailRow(icon = Icons.Default.CalendarMonth, label = "Hiệu lực đến:", value = farmer.vietGapValidUntil)
                                ProfileDetailRow(icon = Icons.Default.Place, label = "Địa chỉ vùng trồng:", value = farmer.addressDetail)
                                ProfileDetailRow(icon = Icons.Default.Eco, label = "Cây trồng chủ lực:", value = farmer.mainCrops)
                                ProfileDetailRow(icon = Icons.Default.Approval, label = "Xác minh thực địa:", value = if (farmer.isApprovedByAdmin) "Đã kiểm tra & phê duyệt bởi BQL Sàn Bình Mỹ" else "Đang kiểm định")
                            }
                        }
                    }

                    // Story / Bio section
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📜", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Câu Chuyện Canh Tác & Cam Kết Nông Dân",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FarmGreenDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = farmer.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextPrimary,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quality Badges
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QualityChip(text = "🚫 Khống chế phân bón hóa học")
                                    QualityChip(text = "💧 Nước tưới lắng lọc")
                                }
                            }
                        }
                    }

                    // Products list from this farmer
                    item {
                        Text(
                            text = "Nông Sản Đang Cung Cấp Tới Phiên Thu Hoạch 🥬",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }

                    if (farmerProducts.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FarmSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hiện nông hộ này chưa đăng ký nông sản mới cho phiên tuần này.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(farmerProducts) { product ->
                            val remaining = (product.originalEstimateKg - product.reservedKg).coerceAtLeast(0.0)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectProduct(product)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(FarmGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(product.imageUrl.ifBlank { "🥬" }, fontSize = 28.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${formatVnd(product.currentPricePerUnit)} / ${product.unit}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Còn khả dụng: ${"%.1f".format(remaining)} ${product.unit}",
                                            color = if (remaining <= 5) Color.Red else FarmTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            selectedProductForBooking = product
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        enabled = remaining > 0 && activeSession?.status == SessionStatus.OPEN
                                    ) {
                                        Text(if (remaining > 0) "+ Đặt trước" else "Hết lượng", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (selectedProductForBooking != null) {
        DirectBookingDialog(
            product = selectedProductForBooking!!,
            activeSession = activeSession,
            onDismiss = { selectedProductForBooking = null },
            onSubmit = { qty, name, phone ->
                onDirectBooking(selectedProductForBooking!!.id, qty, name, phone)
                selectedProductForBooking = null
                onDismiss()
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
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
            fontSize = 12.sp,
            color = FarmTextSecondary,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = FarmTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QualityChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = FarmGreenContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = FarmGreenDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
