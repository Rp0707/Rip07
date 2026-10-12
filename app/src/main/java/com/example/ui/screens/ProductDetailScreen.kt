package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.VerificationStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDate
import com.example.ui.components.formatVnd
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: ProductEntity,
    farmer: FarmerProfileEntity?,
    lot: ProductLotEntity?,
    sessions: List<PreOrderSessionEntity> = emptyList(),
    reviews: List<ProductReviewEntity> = emptyList(),
    onBack: () -> Unit,
    onAddToCart: (Double, Long) -> Unit,
    onNavigateToTraceability: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var quantity by remember { mutableStateOf(1.0) }
    var selectedSessionId by remember(sessions) { mutableStateOf(sessions.firstOrNull()?.id ?: 1L) }
    val remaining = (product.originalEstimateKg - product.reservedKg).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi Tiết Nông Sản", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_detail")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmBackground,
                    titleContentColor = FarmGreenDark
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tạm tính:",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmTextSecondary
                        )
                        Text(
                            text = formatVnd(product.currentPricePerUnit * quantity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }

                    Button(
                        onClick = { onAddToCart(quantity, selectedSessionId) },
                        enabled = remaining > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_add_to_cart_detail")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (remaining > 0) "Thêm Vào Giỏ Đặt Trước" else "Hết Suất Đặt",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("product_detail_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoDataBanner()

            // Header Hero Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FarmGreenContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = product.imageUrl.ifBlank { "🥬" },
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FarmTextPrimary
                    )
                    Text(
                        text = "Danh mục: ${product.category} • Đơn vị: ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatVnd(product.currentPricePerUnit)} / ${product.unit}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                }
            }

            // Quota and Quantity Selector
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
                            text = "Lượng nhận đặt trước:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Còn ${"%.1f".format(remaining)} / ${"%.0f".format(product.originalEstimateKg)} ${product.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining > 5) FarmGreenPrimary else Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val percent = (product.reservedKg / product.originalEstimateKg).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = FarmGreenPrimary,
                        trackColor = Color(0xFFE0E0E0)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = FarmOutline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chọn số lượng (${product.unit}):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedIconButton(
                                onClick = { if (quantity > 1.0) quantity -= 1.0 },
                                enabled = quantity > 1.0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Giảm", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = "%.0f".format(quantity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            OutlinedIconButton(
                                onClick = { if (quantity < remaining) quantity += 1.0 },
                                enabled = quantity < remaining,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tăng", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Harvest Session Selection Box
            if (sessions.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chọn Đợt Thu Hoạch & Giao Hàng",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nông sản được gom đơn, chốt số lượng và cắt tươi trực tiếp theo từng đợt phiên khác nhau dưới đây:",
                            fontSize = 11.sp,
                            color = FarmTextSecondary,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        sessions.forEach { s ->
                            val isSelected = selectedSessionId == s.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) FarmGreenContainer else FarmSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedSessionId = s.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedSessionId = s.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) FarmGreenDark else FarmTextPrimary
                                        )
                                        Text(
                                            text = s.description,
                                            fontSize = 11.sp,
                                            color = FarmTextSecondary,
                                            lineHeight = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Traceability & Lot Certification
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hồ Sơ Lô & Nguồn Gốc",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (lot?.verificationStatus == VerificationStatus.VERIFIED) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đã xác minh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FarmGreenDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Mã lô thu hoạch: ${product.lotCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Thông tin canh tác: ${product.originInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "• Điều kiện bảo quản: ${product.storageInstructions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onNavigateToTraceability(product.lotCode) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tra Cứu Mã QR & Niêm Phong SHA-256")
                    }
                }
            }

            // Farmer Profile Card
            if (farmer != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmEarthContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Agriculture, contentDescription = null, tint = FarmEarthSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nông Hộ Canh Tác",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmEarthOnContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = farmer.householdName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Đại diện: ${farmer.representativeName} • ĐT: ${farmer.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                        Text(
                            text = "Địa chỉ: ${farmer.addressDetail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                        Text(
                            text = "Quy mô: ${farmer.acreageHa} ha • Chứng nhận: ${if (farmer.hasVietGap) "VietGAP (${farmer.vietGapCertCode})" else "Đang hoàn thiện hồ sơ"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FarmGreenDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = farmer.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 4. Product Reviews Section
            val productReviews = remember(reviews, product) {
                reviews.filter { it.productId == product.id }
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FarmSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = FarmHarvestGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đánh Giá Từ Khách Hàng",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                        }
                        
                        val avgRating = if (productReviews.isNotEmpty()) {
                            productReviews.map { it.rating }.average()
                        } else {
                            5.0
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FarmGoldContainer
                        ) {
                            Text(
                                text = "⭐ %.1f/5".format(avgRating),
                                color = FarmGoldOnContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (productReviews.isEmpty()) {
                        Text(
                            text = "Chưa có đánh giá nào cho sản phẩm này. Hãy ủng hộ nông sản và để lại nhận xét đầu tiên nhé!",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        productReviews.forEachIndexed { index, review ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.buyerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FarmTextPrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = if (star <= review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = if (star <= review.rating) FarmHarvestGold else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = review.comment,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = formatDate(review.createdAtEpochMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = FarmTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                if (index < productReviews.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = FarmOutline.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
