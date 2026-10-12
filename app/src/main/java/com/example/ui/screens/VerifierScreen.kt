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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductLotEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.VerificationStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDate
import com.example.ui.components.formatDateTime
import com.example.ui.theme.*

@Composable
fun VerifierScreen(
    verifierUser: UserEntity?,
    lots: List<ProductLotEntity>,
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>,
    onVerifyLot: (lotCode: String, verifierName: String, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLotToVerify by remember { mutableStateOf<ProductLotEntity?>(null) }
    var verifierNotes by remember { mutableStateOf("Đã kiểm tra nhật ký tưới tiêu và dư lượng bảo vệ thực vật đạt chuẩn.") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("verifier_screen"),
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
                        text = "BỘ PHẬN KIỂM ĐỊNH & XÁC MINH NGUỒN GỐC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmGoldContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Thẩm Tra Hồ Sơ Lô & Niêm Phong SHA-256",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Kiểm định viên: ${verifierUser?.fullName ?: "KS. Trần Quốc Đạt"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Nhiệm vụ: Rà soát nhật ký ruộng, chứng nhận VietGAP và cấp chứng nhận số trước khi công bố ra thị trường.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Danh Sách Lô Hàng Thu Hoạch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FarmTextPrimary
            )
        }

        items(lots) { lot ->
            val product = products.find { it.id == lot.productId }
            val farmer = farmers.find { it.id == lot.farmerId }
            val isVerified = lot.verificationStatus == VerificationStatus.VERIFIED

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(lot.lotCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = FarmGreenDark)
                            Text(product?.name ?: "", style = MaterialTheme.typography.bodyMedium)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isVerified) FarmGreenContainer else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = lot.verificationStatus.labelVi,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVerified) FarmGreenDark else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nông hộ: ${farmer?.householdName ?: ""}", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
                    Text("Chứng nhận: ${if (farmer?.hasVietGap == true) "VietGAP ${farmer.vietGapCertCode}" else "Đang mở rộng"}", style = MaterialTheme.typography.bodySmall, color = FarmGreenDark)
                    Text("Ngày dự kiến cắt: ${formatDate(lot.harvestDateEpochMs)}", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)

                    if (isVerified && lot.sha256Hash != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FarmSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Mã băm SHA-256 niêm phong:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                Text(lot.sha256Hash, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = FarmGreenDark)
                                Text("Người ký: ${lot.verifierName} (${lot.verificationTimeEpochMs?.let { formatDateTime(it) } ?: ""})", fontSize = 10.sp, color = FarmTextSecondary)
                            }
                        }
                    }

                    if (!isVerified) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { selectedLotToVerify = lot },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thẩm Tra & Ký Duyệt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Sign & Verify Lot
    if (selectedLotToVerify != null) {
        val lot = selectedLotToVerify!!
        AlertDialog(
            onDismissRequest = { selectedLotToVerify = null },
            title = { Text("Ký Duyệt Xác Minh Lô ${lot.lotCode}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Kiểm tra thông tin đồng ruộng trước khi xác nhận. Sau khi ký duyệt, hệ thống sẽ tự động tạo mã băm SHA-256 để bảo vệ tính toàn vẹn của hồ sơ lô.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary
                    )

                    OutlinedTextField(
                        value = verifierNotes,
                        onValueChange = { verifierNotes = it },
                        label = { Text("Ghi chú kết quả kiểm tra *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verifier = verifierUser?.fullName ?: "KS. Trần Quốc Đạt"
                        onVerifyLot(lot.lotCode, verifier, verifierNotes)
                        selectedLotToVerify = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Ký Duyệt & Niêm Phong")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLotToVerify = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
