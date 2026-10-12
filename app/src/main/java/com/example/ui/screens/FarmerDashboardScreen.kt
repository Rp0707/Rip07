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
import com.example.data.model.SessionStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatVnd
import com.example.ui.theme.*

@Composable
fun FarmerDashboardScreen(
    farmerUser: UserEntity?,
    farmerProfile: FarmerProfileEntity?,
    products: List<ProductEntity>,
    lots: List<ProductLotEntity>,
    allOrders: List<OrderEntity>,
    allOrderItems: List<OrderItemEntity>,
    activeSession: PreOrderSessionEntity?,
    onReportShortage: (lotCode: String, shortageKg: Double, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLotForShortage by remember { mutableStateOf<ProductLotEntity?>(null) }
    var shortageKgInput by remember { mutableStateOf("5.0") }
    var shortageReasonInput by remember { mutableStateOf("Mưa to ngập triều cường ảnh hưởng 1 luống cắt sáng") }

    val myProducts = products.filter { it.farmerId == farmerProfile?.id }
    val myLots = lots.filter { it.farmerId == farmerProfile?.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("farmer_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        // Farmer Header Info
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FarmGreenDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BẢNG ĐIỀU KHIỂN NÔNG HỘ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmGoldContainer
                        )
                        if (farmerProfile?.hasVietGap == true) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FarmGreenPrimary
                            ) {
                                Text(
                                    text = "VietGAP: ${farmerProfile.vietGapCertCode}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = farmerProfile?.householdName ?: "Nông hộ Bình Mỹ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Đại diện: ${farmerProfile?.representativeName ?: ""} • SĐT: ${farmerProfile?.phone ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Vùng canh tác: ${farmerProfile?.addressDetail ?: ""} (${farmerProfile?.acreageHa ?: 1.0} ha)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Closed Demand Aggregation Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tổng Hợp Nhu Cầu Đã Đặt (Số Kg)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeSession?.status == SessionStatus.LOCKED) FarmGoldContainer else FarmGreenContainer
                        ) {
                            Text(
                                text = if (activeSession?.status == SessionStatus.LOCKED) "ĐÃ KHÓA SỐ CHỐT" else "ĐANG NHẬN ĐẶT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeSession?.status == SessionStatus.LOCKED) FarmGoldOnContainer else FarmGreenDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nông hộ căn cứ vào số kg dưới đây để chuẩn bị nhân lực thu hoạch đúng số lượng, tránh cắt dư hoặc thiếu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    myProducts.forEach { product ->
                        val myItems = allOrderItems.filter { it.productId == product.id }
                        val activeItemOrders = allOrders.filter { o ->
                            o.status != OrderStatus.CANCELLED && myItems.any { it.orderId == o.id }
                        }
                        val totalOrderedKg = myItems
                            .filter { itm -> activeItemOrders.any { it.id == itm.orderId } }
                            .sumOf { it.quantity }
                        val remainingCapacity = (product.originalEstimateKg - totalOrderedKg).coerceAtLeast(0.0)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FarmSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Định mức dự kiến: ${"%.0f".format(product.originalEstimateKg)} ${product.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FarmTextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${"%.1f".format(totalOrderedKg)} ${product.unit}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FarmGreenDark
                                    )
                                    Text(
                                        text = "Cần thu hoạch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FarmTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lots & Shortage Reporting
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quản Lý Lô Thu Hoạch & Báo Thiếu Hàng",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nếu có sự cố thời tiết ngập úng hoặc sâu bệnh làm hụt sản lượng, vui lòng báo thiếu công khai để Quản trị viên xử lý hoàn tiền cho khách.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    myLots.forEach { lot ->
                        val product = products.find { it.id == lot.productId }
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lot.isShortage) Color(0xFFFFEBEE) else FarmSurfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${lot.lotCode} (${product?.name ?: ""})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (lot.isShortage) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFC62828)
                                        ) {
                                            Text(
                                                text = "Hụt ${lot.shortageKg} kg",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (lot.isShortage && lot.shortageNote != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Lý do: ${lot.shortageNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB71C1C)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedLotForShortage = lot },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE65100))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Báo Hụt Sản Lượng", fontSize = 11.sp, color = Color(0xFFE65100))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Report Shortage
    if (selectedLotForShortage != null) {
        val lot = selectedLotForShortage!!
        AlertDialog(
            onDismissRequest = { selectedLotForShortage = null },
            title = { Text("Báo Thiếu Sản Lượng Lô ${lot.lotCode}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Nhập số kg thực tế bị thiếu so với đơn đã chốt và lý do để ban điều phối xử lý minh bạch:",
                        fontSize = 12.sp,
                        color = FarmTextSecondary
                    )

                    OutlinedTextField(
                        value = shortageKgInput,
                        onValueChange = { shortageKgInput = it },
                        label = { Text("Số kg bị hụt *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = shortageReasonInput,
                        onValueChange = { shortageReasonInput = it },
                        label = { Text("Lý do khách quan (mưa, ngập, sâu bệnh) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = shortageKgInput.toDoubleOrNull() ?: 0.0
                        onReportShortage(lot.lotCode, kg, shortageReasonInput)
                        selectedLotForShortage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Gửi Báo Thiếu Hàng")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLotForShortage = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
