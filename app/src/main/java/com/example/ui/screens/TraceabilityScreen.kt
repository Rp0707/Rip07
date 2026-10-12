package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductLotEntity
import com.example.data.model.VerificationStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun TraceabilityScreen(
    lots: List<ProductLotEntity>,
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>,
    initialLotCode: String? = null,
    modifier: Modifier = Modifier
) {
    var searchLotCode by remember(initialLotCode) { mutableStateOf(initialLotCode ?: "") }
    var selectedLot by remember(lots, searchLotCode) {
        mutableStateOf(
            if (searchLotCode.isNotBlank()) {
                lots.find { it.lotCode.equals(searchLotCode.trim(), ignoreCase = true) }
            } else {
                lots.firstOrNull()
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("traceability_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        item {
            Text(
                text = "Truy Xuất Nguồn Gốc & Hồ Sơ Lô",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FarmTextPrimary
            )
            Text(
                text = "Minh bạch nhật ký đồng ruộng • Niêm phong mã băm SHA-256 chống làm giả",
                style = MaterialTheme.typography.bodySmall,
                color = FarmTextSecondary
            )
        }

        // Search input
        item {
            OutlinedTextField(
                value = searchLotCode,
                onValueChange = {
                    searchLotCode = it
                    val matched = lots.find { l -> l.lotCode.contains(it.trim(), ignoreCase = true) }
                    if (matched != null) selectedLot = matched
                },
                placeholder = { Text("Nhập hoặc quét mã lô: LOT-BM-2026-RM01") },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                trailingIcon = {
                    if (searchLotCode.isNotEmpty()) {
                        IconButton(onClick = { searchLotCode = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("traceability_search_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Quick Lot Selector Chips
        item {
            Text("Các lô thu hoạch đang mở tra cứu:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lots.take(4).forEach { lot ->
                    FilterChip(
                        selected = selectedLot?.lotCode == lot.lotCode,
                        onClick = {
                            selectedLot = lot
                            searchLotCode = lot.lotCode
                        },
                        label = { Text(lot.lotCode, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Lot detail display
        if (selectedLot != null) {
            val lot = selectedLot!!
            val product = products.find { it.id == lot.productId }
            val farmer = farmers.find { it.id == lot.farmerId }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = lot.lotCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                                Text(
                                    text = product?.name ?: "Nông sản Bình Mỹ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (lot.verificationStatus) {
                                    VerificationStatus.VERIFIED -> FarmGreenContainer
                                    VerificationStatus.PENDING -> Color(0xFFFFF3E0)
                                    VerificationStatus.REJECTED -> Color(0xFFFFEBEE)
                                }
                            ) {
                                Text(
                                    text = lot.verificationStatus.labelVi,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (lot.verificationStatus) {
                                        VerificationStatus.VERIFIED -> FarmGreenDark
                                        VerificationStatus.PENDING -> Color(0xFFE65100)
                                        VerificationStatus.REJECTED -> Color(0xFFC62828)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // QR Graphic
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            StylizedQrCode(
                                codeText = "https://binhmy.farm/trace/${lot.lotCode}",
                                title = "Mã QR Truy Xuất Nguồn Gốc Công Khai"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = FarmOutline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Farm details
                        Text(
                            text = "Thông Tin Canh Tác:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Nông hộ: ${farmer?.householdName ?: "Nông hộ Bình Mỹ"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Người đại diện: ${farmer?.representativeName ?: ""} • SĐT: ${farmer?.phone ?: ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Địa chỉ vùng trồng: ${farmer?.addressDetail ?: ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Chứng nhận: ${if (farmer?.hasVietGap == true) "VietGAP Mã số ${farmer.vietGapCertCode} (Hiệu lực: ${farmer.vietGapValidUntil})" else "Hồ sơ mở rộng đang chuẩn hóa"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmGreenDark
                        )
                        Text(
                            text = "• Ngày dự kiến thu hoạch: ${formatDate(lot.harvestDateEpochMs)} (04:30 sáng)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Sản lượng dự kiến: ${"%.0f".format(lot.estimatedYieldKg)} kg",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = FarmOutline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Verifier & SHA-256 seal info
                        Text(
                            text = "Niêm Phong Dữ Liệu Kiểm Định (SHA-256):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (lot.verificationStatus == VerificationStatus.VERIFIED && lot.sha256Hash != null) {
                            Text(
                                text = "• Kiểm định viên: ${lot.verifierName ?: "Ban kỹ thuật HTX Bình Mỹ"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Thời gian xác minh: ${lot.verificationTimeEpochMs?.let { formatDateTime(it) } ?: "Đã xác nhận"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FarmSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Mã băm toàn vẹn (SHA-256):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FarmTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lot.sha256Hash,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = FarmGreenDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🔒 Dữ liệu đã được niêm phong kỹ thuật số. Mọi thay đổi về sau trên bản ghi gốc sẽ làm sai lệch mã băm này.",
                                style = MaterialTheme.typography.labelSmall,
                                color = FarmTextSecondary
                            )
                        } else {
                            Text(
                                text = "Lô hàng đang trong quá trình thẩm tra thực địa của kiểm định viên trước giờ thu hoạch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FarmEarthSecondary
                            )
                        }

                        if (lot.publicNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ghi chú công khai: ${lot.publicNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FarmTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
