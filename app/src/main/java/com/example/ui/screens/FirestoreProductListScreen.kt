package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.BinhMyProductFirestore
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestoreProductListScreen(
    products: List<BinhMyProductFirestore>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSyncFromFarms: () -> Unit,
    onBack: () -> Unit,
    onSelectProduct: ((BinhMyProductFirestore) -> Unit)? = null,
    onReserveProduct: ((BinhMyProductFirestore, Double, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var productForReservation by remember { mutableStateOf<BinhMyProductFirestore?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Trigger initial refresh if empty
    LaunchedEffect(Unit) {
        if (products.isEmpty()) {
            onRefresh()
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        products.filter { prod ->
            val matchSearch = searchQuery.isBlank() ||
                    prod.tenSanPham.contains(searchQuery, ignoreCase = true) ||
                    prod.tenNongHo.contains(searchQuery, ignoreCase = true) ||
                    prod.diaChiAp.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "VietGAP" -> prod.tieuChuanChatLuong.contains("VietGAP", ignoreCase = true)
                "Rau muống" -> prod.tenSanPham.contains("rau muống", ignoreCase = true)
                "Bầu & Bí" -> prod.tenSanPham.contains("bầu", ignoreCase = true) || prod.tenSanPham.contains("bí", ignoreCase = true)
                "Dền đỏ" -> prod.tenSanPham.contains("dền", ignoreCase = true)
                else -> true
            }

            matchSearch && matchFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌾", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = "Nông Sản Bình Mỹ (Firestore)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = FarmGreenDark
                            )
                        }
                        Text(
                            text = "Cloud Firestore: Collection 'nong_san_binh_my'",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_firestore_screen")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = FarmGreenDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("btn_refresh_firestore")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới từ Firestore",
                            tint = FarmGreenDark
                        )
                    }
                    IconButton(
                        onClick = onSyncFromFarms,
                        modifier = Modifier.testTag("btn_sync_firestore")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Đồng bộ từ các nông hộ lên Firestore",
                            tint = FarmGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FarmGreenLight.copy(alpha = 0.5f))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FarmBackground)
        ) {
            // Search Input
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm theo tên nông sản, tên hộ nông dân...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = FarmTextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = FarmTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firestore_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FarmGreenDark,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf("Tất cả", "VietGAP", "Rau muống", "Bầu & Bí", "Dền đỏ")
                        items(filterOptions) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }



            // Content Area (Scrollable LazyColumn)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading && products.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = FarmGreenDark, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Đang tải danh sách nông sản từ Cloud Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FarmTextSecondary
                        )
                    }
                } else if (filteredProducts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🥬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa tìm thấy nông sản phù hợp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bấm 'Đồng bộ từ các nông hộ' để nạp danh sách nông sản xã Bình Mỹ lên Cloud Firestore.",
                            fontSize = 12.sp,
                            color = FarmTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSyncFromFarms,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đồng Bộ Nông Sản Lên Firestore")
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("firestore_product_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Đang hiển thị ${filteredProducts.size} loại nông sản",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmTextSecondary
                                )
                                if (isLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = FarmGreenDark)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đang đồng bộ...", fontSize = 11.sp, color = FarmGreenDark)
                                    }
                                }
                            }
                        }

                        items(
                            items = filteredProducts,
                            key = { it.id.ifBlank { "${it.tenSanPham}_${it.tenNongHo}" } }
                        ) { prod ->
                            FirestoreProductCard(
                                product = prod,
                                onOrderClick = { productForReservation = prod },
                                onSelect = {
                                    if (onSelectProduct != null) {
                                        onSelectProduct(prod)
                                    } else {
                                        productForReservation = prod
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🌱", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Cam kết từ các Nông hộ Xã Bình Mỹ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FarmGreenDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Chỉ cắt rau lúc sáng sớm ngày giao hàng theo số lượng khách đã chốt trước trên hệ thống để đảm bảo độ tươi giòn tối đa.",
                                        fontSize = 11.sp,
                                        color = FarmTextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (productForReservation != null) {
        HarvestOrderReservationDialog(
            product = productForReservation!!,
            onDismiss = { productForReservation = null },
            onConfirm = { quantity, notes ->
                val p = productForReservation!!
                productForReservation = null
                onReserveProduct?.invoke(p, quantity, notes)
                scope.launch {
                    val qtyFormatted = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else "%.1f".format(quantity)
                    snackbarHostState.showSnackbar(
                        message = "Đã đặt hàng thành công: $qtyFormatted ${p.donViTinh} ${p.tenSanPham} (${p.tenNongHo}) cho đợt cắt ${p.ngayThuHoachDuKien}!",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

@Composable
fun FirestoreProductCard(
    product: BinhMyProductFirestore,
    onOrderClick: () -> Unit,
    onSelect: () -> Unit = onOrderClick
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.5.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = tween(300))
            .testTag("firestore_product_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Tên sản phẩm & Giá dự kiến
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.tenSanPham,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                }

                Surface(
                    color = FarmGreenContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${"%,.0f".format(product.giaDuKien)} đ/${product.donViTinh}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FarmGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Tên nông hộ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(FarmGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍🌾", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nông hộ: ",
                    fontSize = 12.sp,
                    color = FarmTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = product.tenNongHo,
                    fontSize = 12.sp,
                    color = FarmTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Ngày thu hoạch dự kiến
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEFCBF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📅", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Thu hoạch dự kiến: ",
                    fontSize = 12.sp,
                    color = FarmTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = product.ngayThuHoachDuKien,
                    fontSize = 12.sp,
                    color = Color(0xFFC05621),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Tiêu chuẩn, Sản lượng & Địa chỉ ấp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = FarmGreenLight.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.tieuChuanChatLuong,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (product.sanLuongDuKienKg > 0) {
                    Surface(
                        color = Color(0xFFEDF2F7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Sản lượng: ${"%.0f".format(product.sanLuongDuKienKg)} kg",
                            fontSize = 10.sp,
                            color = FarmTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFEDF2F7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.diaChiAp,
                        fontSize = 10.sp,
                        color = FarmTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Click hint & toggle indicator
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) "Thu gọn chi tiết" else "Chạm để xem thông tin liên hệ nông hộ & mô tả",
                    fontSize = 11.sp,
                    color = if (isExpanded) FarmGreenDark else FarmTextSecondary,
                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = FarmGreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // EXPANDABLE DETAIL VIEW
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Block 1: Farmer Contact Details (Thông tin liên hệ nông hộ)
                    Surface(
                        color = Color(0xFFF7FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📞", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thông Tin Liên Hệ Nông Hộ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                            }

                            val contactName = product.nguoiDaiDien.ifBlank { product.tenNongHo }
                            Row {
                                Text("• Đại diện: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                Text(contactName, fontSize = 11.sp, color = FarmTextPrimary, fontWeight = FontWeight.Medium)
                            }

                            val phone = product.soDienThoaiLienHe.ifBlank { "0908 123 456" }
                            Row {
                                Text("• Số điện thoại: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                Text(phone, fontSize = 11.sp, color = FarmGreenDark, fontWeight = FontWeight.Bold)
                            }

                            Row {
                                Text("• Địa chỉ vườn: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                Text(product.diaChiAp, fontSize = 11.sp, color = FarmTextPrimary)
                            }

                            if (product.dienTichCanhTacHa > 0) {
                                Row {
                                    Text("• Diện tích canh tác: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                    Text("${product.dienTichCanhTacHa} ha đất phù sa ven sông", fontSize = 11.sp, color = FarmTextPrimary)
                                }
                            }
                        }
                    }

                    // Block 2: Detailed Product Description & Farming Method (Mô tả & Phương thức canh tác)
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FarmGreenLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mô Tả & Hướng Dẫn Nông Sản",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                            }

                            if (product.moTa.isNotBlank()) {
                                Text(
                                    text = product.moTa,
                                    fontSize = 11.sp,
                                    color = FarmTextPrimary,
                                    lineHeight = 16.sp
                                )
                            }

                            if (product.phuongThucCanhTac.isNotBlank()) {
                                Row {
                                    Text("• Canh tác: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                                    Text(product.phuongThucCanhTac, fontSize = 11.sp, color = FarmTextPrimary)
                                }
                            }

                            if (product.huongDanBaoQuan.isNotBlank()) {
                                Row {
                                    Text("• Bảo quản: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB7791F))
                                    Text(product.huongDanBaoQuan, fontSize = 11.sp, color = FarmTextPrimary)
                                }
                            }

                            if (product.maLoKiemDinh.isNotBlank()) {
                                Row {
                                    Text("• Mã lô kiểm định: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                    Text(product.maLoKiemDinh, fontSize = 11.sp, color = FarmGreenDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Block 3: Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Contact farmer action
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = FarmGreenDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gọi Nhà Vườn", fontSize = 11.sp, color = FarmGreenDark)
                        }

                        Button(
                            onClick = onOrderClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_order_product_${product.id}")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đặt hàng", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HarvestOrderReservationDialog(
    product: BinhMyProductFirestore,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, notes: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var notesText by remember { mutableStateOf("") }
    val currentQty = quantityText.toDoubleOrNull() ?: 0.0
    val totalCost = currentQty * product.giaDuKien
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📦", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Đặt Hàng Nông Sản Thu Hoạch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = FarmGreenDark
                    )
                    Text(
                        text = "Phiếu giữ chỗ trước ngày nông dân cắt rau",
                        style = MaterialTheme.typography.labelSmall,
                        color = FarmTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Summary Card
                Surface(
                    color = Color(0xFFF1F8F4),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = product.tenSanPham,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FarmGreenDark
                        )
                        Row {
                            Text("👨‍🌾 Nông hộ: ", fontSize = 12.sp, color = FarmTextSecondary, fontWeight = FontWeight.Bold)
                            Text(product.tenNongHo, fontSize = 12.sp, color = FarmTextPrimary)
                        }
                        Row {
                            Text("📅 Thu hoạch dự kiến: ", fontSize = 12.sp, color = FarmTextSecondary, fontWeight = FontWeight.Bold)
                            Text(product.ngayThuHoachDuKien, fontSize = 12.sp, color = Color(0xFFC05621), fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("🏷️ Giá niêm yết: ", fontSize = 12.sp, color = FarmTextSecondary, fontWeight = FontWeight.Bold)
                            Text("${"%,.0f".format(product.giaDuKien)} đ/${product.donViTinh}", fontSize = 12.sp, color = FarmGreenDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quantity Input Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Số lượng muốn đặt trước (${product.donViTinh}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = FarmTextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val q = (quantityText.toDoubleOrNull() ?: 1.0) - 1.0
                                if (q >= 0.5) quantityText = if (q % 1.0 == 0.0) q.toInt().toString() else "%.1f".format(q)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FarmGreenLight.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = FarmGreenDark)
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                errorMessage = null
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_reserve_quantity"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            trailingIcon = {
                                Text(
                                    text = product.donViTinh,
                                    fontSize = 12.sp,
                                    color = FarmTextSecondary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        )

                        IconButton(
                            onClick = {
                                val q = (quantityText.toDoubleOrNull() ?: 0.0) + 1.0
                                quantityText = if (q % 1.0 == 0.0) q.toInt().toString() else "%.1f".format(q)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FarmGreenLight.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tăng", tint = FarmGreenDark)
                        }
                    }

                    // Quick Preset Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(1.0, 2.0, 5.0, 10.0)
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    quantityText = if (preset % 1.0 == 0.0) preset.toInt().toString() else preset.toString()
                                },
                                label = { Text("+${if (preset % 1.0 == 0.0) preset.toInt() else preset} ${product.donViTinh}", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }

                // Delivery Notes Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Ghi chú giao hàng hoặc yêu cầu nhà vườn (tùy chọn):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FarmTextSecondary
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Ví dụ: Giao sáng sớm trước 7h, chọn rau non, để nguyên rễ...", fontSize = 12.sp) },
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reserve_notes")
                    )
                }

                // Total Estimate Section
                Surface(
                    color = Color(0xFFFEFCBF),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tổng tiền ước tính:", fontSize = 12.sp, color = FarmTextSecondary)
                            Text(
                                text = "${quantityText.toDoubleOrNull() ?: 0.0} ${product.donViTinh} x ${"%,.0f".format(product.giaDuKien)} đ",
                                fontSize = 11.sp,
                                color = FarmTextSecondary
                            )
                        }
                        Text(
                            text = "${"%,.0f".format(totalCost)} đ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FarmGreenDark
                        )
                    }
                }

                Text(
                    text = "🌱 Nhà vườn Bình Mỹ chỉ tiến hành cắt rau vào sáng sớm ngày thu hoạch dự kiến để giao đến bạn với độ tươi giòn ngon nhất.",
                    fontSize = 10.sp,
                    color = FarmTextSecondary,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityText.toDoubleOrNull()
                    if (q == null || q <= 0.0) {
                        errorMessage = "Vui lòng nhập số lượng hợp lệ lớn hơn 0"
                        return@Button
                    }
                    onConfirm(q, notesText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                modifier = Modifier.testTag("btn_confirm_reserve_order")
            ) {
                Text("Xác Nhận Đặt Hàng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = FarmTextSecondary)
            }
        }
    )
}
