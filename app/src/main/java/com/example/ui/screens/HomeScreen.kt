package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.PopUpLocationEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.SessionStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    activeSession: PreOrderSessionEntity?,
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>,
    popups: List<PopUpLocationEntity>,
    orders: List<OrderEntity> = emptyList(),
    orderItems: List<OrderItemEntity> = emptyList(),
    onNavigateToProducts: () -> Unit,
    onNavigateToSessionDetail: () -> Unit,
    onNavigateToOrders: () -> Unit = {},
    onNavigateToTraceability: () -> Unit = {},
    onSelectProduct: (ProductEntity) -> Unit,
    onAddToCart: (Long) -> Unit,
    onOpenAiChat: () -> Unit = {},
    onDirectBooking: (Long, Double, String, String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var bookingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedFarmerForDetail by remember { mutableStateOf<FarmerProfileEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Wallpaper Banner: Nông Sản Bình Mỹ Củ Chi
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_wallpaper_banner"),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F3D2E), // Deep Forest Green
                                    Color(0xFF1B5E3C), // Vibrant Farm Leaf Green
                                    Color(0xFF246B46)  // Rich agricultural green
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        // Top Pill Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🌿", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Xã Bình Mỹ • Củ Chi • TP.HCM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = FarmHarvestGold
                            ) {
                                Text(
                                    text = "Vựa Rau Nước Lớn Nhất",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // App Title & Tagline
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🥬", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "NÔNG SẢN BÌNH MỸ",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Sàn Đặt Trước • Rau Muống Sạch Cắt Sáng",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4EAD8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Context Highlights Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.22f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("188 Hộ", color = FarmHarvestGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Canh tác", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                                Text("•", color = Color.White.copy(alpha = 0.4f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("208,2 ha", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Diện tích", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                                Text("•", color = Color.White.copy(alpha = 0.4f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("59 VietGAP", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Đã cấp chuẩn", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Embedded Active Session Callout
                        if (activeSession != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToSessionDetail() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SessionStatusBadge(status = activeSession.status)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = activeSession.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "⏰ Hạn chốt: ${formatDateTime(activeSession.closeDateEpochMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FarmHarvestGold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Chi tiết",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick AI Concierge Trigger
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FarmHarvestGold.copy(alpha = 0.95f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAiChat() }
                                .testTag("btn_hero_ask_ai")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🤖", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hỏi Trợ Lý AI: Tư Vấn Rau Muống & Phiên Thu Hoạch",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Featured Fresh Produce Header - DISPLAYED IMMEDIATELY IN FIRST VIEWPORT!
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌱", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Nông Sản Bình Mỹ Đang Đặt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Text(
                            text = "Rau sạch cắt sáng sớm • Chốt theo đơn đặt",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                    }
                }
                TextButton(
                    onClick = onNavigateToProducts,
                    modifier = Modifier.testTag("btn_view_all_products")
                ) {
                    Text("Xem tất cả (${products.size})", color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Horizontal Product Highlights
        item {
            if (products.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FarmSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = FarmGreenPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Đang tải danh sách nông sản Bình Mỹ...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FarmTextSecondary
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(products.take(6)) { product ->
                    val farmer = farmers.find { it.id == product.farmerId }
                    val remaining = (product.originalEstimateKg - product.reservedKg).coerceAtLeast(0.0)

                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .clickable { onSelectProduct(product) }
                            .testTag("featured_product_${product.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Emoji / Image Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FarmGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = product.imageUrl.ifBlank { "🥬" },
                                    fontSize = 44.sp
                                )
                                if (farmer?.hasVietGap == true) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FarmGreenPrimary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "VietGAP",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = farmer?.householdName ?: "Nông hộ Bình Mỹ",
                                style = MaterialTheme.typography.bodySmall,
                                color = FarmTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatVnd(product.currentPricePerUnit)} / ${product.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quota remaining progress
                            val percent = (product.reservedKg / product.originalEstimateKg).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = if (percent > 0.85f) Color(0xFFE65100) else FarmGreenPrimary,
                                trackColor = Color(0xFFE0E0E0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Còn: ${"%.1f".format(remaining)} ${product.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (remaining <= 5) Color.Red else FarmTextSecondary
                                )
                                Text(
                                    text = "${(percent * 100).toInt()}% đã đặt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = FarmTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { bookingProduct = product },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("btn_quick_add_${product.id}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                enabled = remaining > 0 && activeSession?.status == SessionStatus.OPEN
                            ) {
                                Text(
                                    text = if (remaining > 0) "+ Đặt trước" else "Hết lượng nhận",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // 5. 5-Step Operational Pre-Order Model Section
        item {
            BinhMyWorkflowSection(
                activeSession = activeSession,
                orders = orders,
                orderItems = orderItems,
                farmers = farmers,
                popups = popups,
                defaultSelectedStep = 3,
                onNavigateToProducts = onNavigateToProducts,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
                onNavigateToOrders = onNavigateToOrders,
                onNavigateToTraceability = onNavigateToTraceability
            )
        }

        // 5.5. Outstanding Binh My Farmers Carousel with Trust Guarantee
        item {
            Column {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = FarmGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HỒ SƠ NÔNG HỘ TIÊU BIỂU XÃ BÌNH MỸ 👨‍🌾",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kết nối trực tiếp 188 hộ canh tác rau truyền thống ven sông Sài Gòn • 100% minh bạch chứng nhận VietGAP, mã số lô trồng & số điện thoại liên hệ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(farmers) { farmer ->
                        val farmerProds = products.filter { it.farmerId == farmer.id }

                        Card(
                            modifier = Modifier
                                .width(290.dp)
                                .clickable { selectedFarmerForDetail = farmer }
                                .testTag("farmer_card_${farmer.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Custom visual farmer badge
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(FarmGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (farmer.representativeName.contains("Nữ") || farmer.householdName.contains("Cô") || farmer.householdName.contains("Chị")) "👩‍🌾" else "👨‍🌾",
                                            fontSize = 28.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = farmer.householdName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FarmGreenDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Đại diện: ${farmer.representativeName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FarmTextPrimary,
                                            fontSize = 11.sp
                                        )
                                        if (farmer.hasVietGap) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = FarmGreenPrimary.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = "VietGAP: ${farmer.vietGapCertCode}",
                                                    color = FarmGreenDark,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(
                                    text = farmer.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = FarmOutline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 ${farmer.addressDetail}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = FarmTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FarmGoldContainer
                                    ) {
                                        Text(
                                            text = "${farmer.acreageHa} Ha",
                                            color = FarmGoldOnContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { selectedFarmerForDetail = farmer },
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("btn_view_farmer_${farmer.id}"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Xem Hồ Sơ & Nông Sản (${farmerProds.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Pop-up Points Section
        item {
            Column {
                Text(
                    text = "Mạng Lưới Điểm Nhận Pop-up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FarmTextPrimary
                )
                Text(
                    text = "Gom hàng tập trung giảm chi phí • Có hỗ trợ giao tận nhà",
                    style = MaterialTheme.typography.bodySmall,
                    color = FarmTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                popups.forEach { popup ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FarmEarthSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = popup.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = popup.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Giờ mở: ${popup.operatingHours}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Context & Demo Data Disclaimer
        item {
            DemoDataBanner()
        }
    }

    if (bookingProduct != null) {
        DirectBookingDialog(
            product = bookingProduct!!,
            activeSession = activeSession,
            onDismiss = { bookingProduct = null },
            onSubmit = { quantity: Double, name: String, phone: String ->
                onDirectBooking(bookingProduct!!.id, quantity, name, phone)
                bookingProduct = null
            }
        )
    }

    if (selectedFarmerForDetail != null) {
        val farmerProds = products.filter { it.farmerId == selectedFarmerForDetail!!.id }
        com.example.ui.components.FarmerProfileDetailDialog(
            farmer = selectedFarmerForDetail!!,
            farmerProducts = farmerProds,
            activeSession = activeSession,
            onDismiss = { selectedFarmerForDetail = null },
            onSelectProduct = onSelectProduct,
            onAddToCart = onAddToCart,
            onDirectBooking = onDirectBooking
        )
    }
}
