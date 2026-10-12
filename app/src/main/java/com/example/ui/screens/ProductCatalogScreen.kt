package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.SessionStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDate
import com.example.ui.components.formatVnd
import com.example.ui.components.DirectBookingDialog
import com.example.ui.theme.*

@Composable
fun ProductCatalogScreen(
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>,
    activeSession: PreOrderSessionEntity?,
    onSelectProduct: (ProductEntity) -> Unit,
    onAddToCart: (Long) -> Unit,
    onDirectBooking: (Long, Double, String, String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    val categories = listOf("Tất cả", "Rau ăn lá", "Củ quả quả")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchCat = selectedCategory == "Tất cả" || p.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("product_catalog_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DemoDataBanner()
        }

        // Search text field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input"),
                placeholder = { Text("Tìm kiếm rau muống, bầu, bí...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FarmGreenPrimary,
                    unfocusedBorderColor = FarmOutline
                )
            )
        }

        // Category filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FarmGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_$category")
                    )
                }
            }
        }

        // Session status warning if session is locked
        if (activeSession != null && activeSession.status != SessionStatus.OPEN) {
            item {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phiên hiện tại đã khóa đơn hoặc đang thu hoạch. Các đơn đặt mới sẽ được ghi nhận vào phiên kế tiếp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        // Product items
        if (filteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Không tìm thấy nông sản phù hợp", color = FarmTextSecondary)
                    }
                }
            }
        } else {
            items(filteredProducts) { product ->
                val farmer = farmers.find { it.id == product.farmerId }
                CatalogProductCardItem(
                    product = product,
                    farmer = farmer,
                    activeSession = activeSession,
                    onAddToCart = onAddToCart,
                    onSelectProduct = onSelectProduct,
                    onDirectBooking = onDirectBooking
                )
            }
        }
    }
}

@Composable
fun CatalogProductCardItem(
    product: ProductEntity,
    farmer: FarmerProfileEntity?,
    activeSession: PreOrderSessionEntity?,
    onAddToCart: (Long) -> Unit,
    onSelectProduct: (ProductEntity) -> Unit,
    onDirectBooking: (Long, Double, String, String) -> Unit = { _, _, _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showOrderForm by remember { mutableStateOf(false) }
    val remaining = (product.originalEstimateKg - product.reservedKg).coerceAtLeast(0.0)
    val percent = (product.reservedKg / product.originalEstimateKg).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = tween(300))
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji Box
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FarmGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = product.imageUrl.ifBlank { "🥬" }, fontSize = 38.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (farmer?.hasVietGap == true) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = FarmGreenPrimary
                            ) {
                                Text(
                                    text = "VietGAP",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = farmer?.householdName ?: "Nông hộ Bình Mỹ",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatVnd(product.currentPricePerUnit)} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quota bar
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (percent > 0.85f) Color(0xFFE65100) else FarmGreenPrimary,
                        trackColor = Color(0xFFE0E0E0)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Còn nhận: ${"%.1f".format(remaining)} ${product.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (remaining <= 5) Color.Red else FarmTextSecondary
                        )
                        Text(
                            text = "Đã đặt: ${(percent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Add button
                IconButton(
                    onClick = { onAddToCart(product.id) },
                    enabled = remaining > 0 && activeSession?.status == SessionStatus.OPEN,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (remaining > 0) FarmGreenPrimary else Color.LightGray)
                        .testTag("btn_catalog_add_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Thêm vào giỏ",
                        tint = Color.White
                    )
                }
            }

            // Click hint & toggle bar
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) "Thu gọn chi tiết" else "Chạm để xem thông tin liên hệ nông hộ & mô tả",
                    fontSize = 11.sp,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary else FarmTextSecondary,
                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // EXPANDABLE DETAIL VIEW
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Contact details of farmer
                    if (farmer != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👨‍🌾", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Thông Tin Liên Hệ Nông Hộ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text("• Đại diện: ${farmer.representativeName} (${farmer.householdName})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• Số điện thoại: ${farmer.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("• Vườn tại: ${farmer.addressDetail}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                if (farmer.acreageHa > 0) {
                                    Text("• Quy mô canh tác: ${farmer.acreageHa} ha (${farmer.mainCrops})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                }
                                if (farmer.hasVietGap) {
                                    Text("• Chứng nhận VietGAP: ${farmer.vietGapCertCode} (Hạn đến ${farmer.vietGapValidUntil})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Product storage & cooking guide
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mô Tả & Hướng Dẫn Nông Sản",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = product.storageInstructions,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                lineHeight = 15.sp
                            )
                            Text("• Mã lô kiểm định: ${product.lotCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectProduct(product) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Xem Chi Tiết Lô", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showOrderForm = true },
                            enabled = remaining > 0 && activeSession?.status == SessionStatus.OPEN,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_order_catalog_${product.id}")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đặt hàng", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showOrderForm) {
        DirectBookingDialog(
            product = product,
            activeSession = activeSession,
            onDismiss = { showOrderForm = false },
            onSubmit = { quantity: Double, name: String, phone: String ->
                onDirectBooking(product.id, quantity, name, phone)
                showOrderForm = false
            }
        )
    }
}
