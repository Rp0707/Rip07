package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.data.local.entity.*
import com.example.data.model.DeliveryType
import com.example.data.model.SessionStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatVnd
import com.example.ui.theme.*

@Composable
fun CartCheckoutScreen(
    cart: Map<String, Double>, // "productId_sessionId" -> quantity
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>,
    popups: List<PopUpLocationEntity>,
    activeSession: PreOrderSessionEntity?,
    sessions: List<PreOrderSessionEntity> = emptyList(),
    currentUser: UserEntity?,
    onUpdateQty: (Long, Long, Double) -> Unit, // (productId, sessionId, qty)
    onRemoveItem: (Long, Long) -> Unit,        // (productId, sessionId)
    onClearCart: () -> Unit,
    onSubmitOrder: (
        sessionId: Long,
        deliveryType: DeliveryType,
        popupLocationId: Long?,
        homeAddress: String?,
        receiverName: String,
        receiverPhone: String,
        buyerNote: String?,
        onSuccess: (OrderEntity) -> Unit
    ) -> Unit,
    onNavigateToProducts: () -> Unit,
    onOrderSuccess: (OrderEntity) -> Unit,
    onInitOnlinePayment: (Long, PaymentProviderType, Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var deliveryType by remember { mutableStateOf(DeliveryType.POPUP_PICKUP) }
    var selectedPopupId by remember(popups) { mutableStateOf(popups.firstOrNull()?.id ?: 1L) }
    var homeAddress by remember { mutableStateOf("") }
    var receiverName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "") }
    var receiverPhone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var buyerNote by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Payment states
    var paymentMethod by remember { mutableStateOf("COD") } // "COD" or "ONLINE"
    var onlineProvider by remember { mutableStateOf(PaymentProviderType.VNPAY) }
    var isDeposit by remember { mutableStateOf(false) } // true: deposit 30%, false: full 100%

    // Map of sessionEntity -> List of (ProductEntity, qty)
    val groupedCartItems = remember(cart, products, sessions, activeSession) {
        val result = mutableMapOf<PreOrderSessionEntity, MutableList<Pair<ProductEntity, Double>>>()
        cart.forEach { (compositeKey, qty) ->
            val parts = compositeKey.split("_")
            val pId = parts.getOrNull(0)?.toLongOrNull() ?: return@forEach
            val sId = parts.getOrNull(1)?.toLongOrNull() ?: activeSession?.id ?: 1L
            
            val product = products.find { it.id == pId } ?: return@forEach
            val session = sessions.find { it.id == sId } ?: activeSession ?: return@forEach
            
            result.getOrPut(session) { mutableListOf() }.add(product to qty)
        }
        result
    }

    val goodsSubtotal = remember(groupedCartItems) {
        groupedCartItems.values.flatten().sumOf { (product, qty) -> product.currentPricePerUnit * qty }
    }

    val selectedPopup = popups.find { it.id == selectedPopupId }
    val deliveryFee = if (deliveryType == DeliveryType.HOME_DELIVERY) {
        selectedPopup?.baseDeliveryFee ?: 20000.0
    } else {
        0.0
    }
    val grandTotal = goodsSubtotal + deliveryFee

    val hasItems = groupedCartItems.isNotEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_checkout_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        if (!hasItems) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Giỏ hàng pre-order đang trống",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Text(
                            text = "Hãy chọn thêm nông sản từ các đợt thu hoạch khác nhau.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToProducts,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_cart_browse_products")
                        ) {
                            Text("Xem Danh Sách Nông Sản")
                        }
                    }
                }
            }
        } else {
            // Cart header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giỏ Hàng Liên Phiên (${groupedCartItems.values.flatten().size} sản phẩm)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                    TextButton(onClick = onClearCart) {
                        Text("Xóa tất cả", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }

            // Render cart grouped by session
            groupedCartItems.forEach { (session, items) ->
                item {
                    Surface(
                        color = FarmGreenContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("📅", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = session.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FarmGreenDark
                                    )
                                    Text(
                                        text = "Nhận hàng dự kiến: ${formatDateTime(session.deliveryDateEpochMs)}",
                                        fontSize = 10.sp,
                                        color = FarmTextSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FarmGreenPrimary
                            ) {
                                Text(
                                    text = "${items.size} món",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                items(items) { (product, qty) ->
                    val farmer = farmers.find { it.id == product.farmerId }
                    val lineTotal = product.currentPricePerUnit * qty

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FarmGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = product.imageUrl.ifBlank { "🥬" }, fontSize = 28.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trồng bởi: ${farmer?.householdName ?: "Nông hộ Bình Mỹ"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FarmTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${formatVnd(product.currentPricePerUnit)} / ${product.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FarmGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUpdateQty(product.id, session.id, qty - 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Giảm", modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "%.0f".format(qty),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = { onUpdateQty(product.id, session.id, qty + 1.0) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tăng", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Delivery method selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Phương Thức Nhận Hàng",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Radio 1: Pop-up
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (deliveryType == DeliveryType.POPUP_PICKUP) FarmGreenContainer else Color.Transparent)
                                .padding(8.dp)
                                .clickable { deliveryType = DeliveryType.POPUP_PICKUP }
                        ) {
                            RadioButton(
                                selected = deliveryType == DeliveryType.POPUP_PICKUP,
                                onClick = { deliveryType = DeliveryType.POPUP_PICKUP },
                                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Nhận tại điểm Pop-up (Miễn phí gom hàng)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Nông sản chuyển thẳng từ ruộng đến điểm nhận mát", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Radio 2: Home Delivery
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (deliveryType == DeliveryType.HOME_DELIVERY) FarmGreenContainer else Color.Transparent)
                                .padding(8.dp)
                                .clickable { deliveryType = DeliveryType.HOME_DELIVERY }
                        ) {
                            RadioButton(
                                selected = deliveryType == DeliveryType.HOME_DELIVERY,
                                onClick = { deliveryType = DeliveryType.HOME_DELIVERY },
                                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Giao tận nhà thử nghiệm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Giao từ điểm Pop-up gần nhất trong bán kính hỗ trợ", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Select Pop-up Point
                        Text("Chọn trạm Pop-up liên kết:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        popups.forEach { popup ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedPopupId == popup.id) FarmGreenContainer else FarmSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedPopupId = popup.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPopupId == popup.id,
                                        onClick = { selectedPopupId = popup.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                                    )
                                    Column(modifier = Modifier.padding(start = 6.dp)) {
                                        Text(popup.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(popup.address, style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary, fontSize = 11.sp)
                                        if (deliveryType == DeliveryType.HOME_DELIVERY) {
                                            Text("Phí giao thử nghiệm: ${formatVnd(popup.baseDeliveryFee)}", style = MaterialTheme.typography.labelSmall, color = FarmEarthSecondary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (deliveryType == DeliveryType.HOME_DELIVERY) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = homeAddress,
                                onValueChange = { homeAddress = it },
                                label = { Text("Địa chỉ nhận hàng chi tiết *") },
                                placeholder = { Text("Số nhà, đường, ấp, xã...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_home_address"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Contact info section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Thông Tin Người Nhận",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = receiverName,
                            onValueChange = { receiverName = it },
                            label = { Text("Họ và tên người nhận") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_receiver_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = receiverPhone,
                            onValueChange = { receiverPhone = it },
                            label = { Text("Số điện thoại liên hệ") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_receiver_phone"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = buyerNote,
                            onValueChange = { buyerNote = it },
                            label = { Text("Ghi chú cho các phiên thu hoạch (không bắt buộc)") },
                            placeholder = { Text("Ví dụ: Nhận sớm trước 09:00 sáng") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_buyer_note"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Payment Method Selection Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Phương Thức Thanh Toán",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // COD Choice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (paymentMethod == "COD") FarmGreenContainer else Color.Transparent)
                                .clickable { paymentMethod = "COD" }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = paymentMethod == "COD",
                                onClick = { paymentMethod = "COD" },
                                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Thanh toán tiền mặt (COD)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Thanh toán khi nhận hàng rau tươi mát lành", fontSize = 11.sp, color = FarmTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ONLINE Choice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (paymentMethod == "ONLINE") FarmGreenContainer else Color.Transparent)
                                .clickable { paymentMethod = "ONLINE" }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = paymentMethod == "ONLINE",
                                onClick = { paymentMethod = "ONLINE" },
                                colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Thanh toán trực tuyến an toàn", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FarmGreenPrimary)
                                Text("Chuyển khoản QR ngân hàng hoặc ví điện tử an toàn", fontSize = 11.sp, color = FarmTextSecondary)
                            }
                        }

                        // Options if ONLINE selected
                        AnimatedVisibility(visible = paymentMethod == "ONLINE") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 8.dp)
                                    .border(1.dp, FarmOutline, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("1. Chọn phương thức đặt trước:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isDeposit) FarmGreenContainer else FarmSurfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isDeposit = false }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Trả Thẳng 100%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (!isDeposit) FarmGreenDark else Color.DarkGray)
                                            Text("Trả toàn bộ đơn", fontSize = 10.sp, color = FarmTextSecondary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDeposit) FarmGoldContainer else FarmSurfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isDeposit = true }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Đặt Cọc 30%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDeposit) FarmGoldOnContainer else Color.DarkGray)
                                            Text("Giữ suất thu hoạch", fontSize = 10.sp, color = FarmTextSecondary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("2. Chọn cổng thanh toán:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                PaymentProviderType.values().forEach { provider ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (onlineProvider == provider) FarmGreenContainer.copy(alpha = 0.6f) else Color.Transparent)
                                            .clickable { onlineProvider = provider }
                                            .padding(6.dp)
                                    ) {
                                        RadioButton(
                                            selected = onlineProvider == provider,
                                            onClick = { onlineProvider = provider },
                                            colors = RadioButtonDefaults.colors(selectedColor = FarmGreenPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(provider.labelVi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Price Summary Card & Confirmation
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Chi Phí Đặt Trước",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tiền nông sản:", style = MaterialTheme.typography.bodyMedium)
                            Text(formatVnd(goodsSubtotal), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Phí giao hàng:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (deliveryFee == 0.0) "Miễn phí (Pop-up)" else formatVnd(deliveryFee),
                                fontWeight = FontWeight.SemiBold,
                                color = if (deliveryFee == 0.0) FarmGreenPrimary else FarmTextPrimary
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = FarmOutline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng thanh toán:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                formatVnd(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Hệ thống sẽ tự động tách giỏ thành các đơn đặt hàng riêng cho từng phiên tương ứng.\n" +
                                    "• Giá nông sản được giữ nguyên cố định.\n" +
                                    "• Thanh toán tiền mặt khi nhận rau tươi mát lành.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val firstSessionId = groupedCartItems.keys.firstOrNull()?.id ?: activeSession?.id ?: 1L
                                isSubmitting = true
                                onSubmitOrder(
                                    firstSessionId,
                                    deliveryType,
                                    if (deliveryType == DeliveryType.POPUP_PICKUP) selectedPopupId else null,
                                    if (deliveryType == DeliveryType.HOME_DELIVERY) homeAddress else null,
                                    receiverName,
                                    receiverPhone,
                                    buyerNote
                                ) { order ->
                                    isSubmitting = false
                                    if (paymentMethod == "ONLINE") {
                                        onInitOnlinePayment(order.id, onlineProvider, isDeposit)
                                    } else {
                                        onOrderSuccess(order)
                                    }
                                }
                            },
                            enabled = !isSubmitting &&
                                    (deliveryType != DeliveryType.HOME_DELIVERY || homeAddress.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_confirm_preorder")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (paymentMethod == "ONLINE") "Xác Nhận & Thanh Toán Online" else "Xác Nhận Đặt Trước Liên Phiên",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
