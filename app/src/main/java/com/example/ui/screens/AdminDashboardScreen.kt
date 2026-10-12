package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    sessions: List<PreOrderSessionEntity>,
    activeSession: PreOrderSessionEntity?,
    farmers: List<FarmerProfileEntity>,
    products: List<ProductEntity>,
    lots: List<ProductLotEntity>,
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    popups: List<PopUpLocationEntity>,
    
    // Security and Extended parameters
    users: List<UserEntity>,
    auditLogs: List<AuditLogEntity>,
    currentUser: UserEntity?,
    activeSessions: List<UserSessionEntity>,
    isMfaVerified: Boolean,
    
    // Callbacks
    onLockSession: (Long) -> Unit,
    onAdvanceSession: (Long, SessionStatus) -> Unit,
    onApproveFarmer: (Long, Boolean) -> Unit,
    onResolveShortage: (orderId: Long, action: String, refundAmount: Double) -> Unit,
    onUpdatePrice: (productId: Long, newPrice: Double) -> Unit,
    onSendBroadcast: (title: String, message: String, targetRole: UserRole?) -> Unit,
    
    // SuperAdmin Callbacks
    onLockUnlockUser: (targetUserId: Long, isLocked: Boolean, reason: String?) -> Unit,
    onChangeUserRole: (targetUserId: Long, newRole: UserRole) -> Unit,
    onRevokeSession: (sessionId: String) -> Unit,
    onRevokeAllSessions: () -> Unit,
    onConfirmMfa: (code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // 10 Vietnamese Admin Screens Navigation List
    val tabTitles = listOf(
        "Tổng quan",       // 0
        "Người dùng",      // 1 (SuperAdmin Only)
        "Nông hộ",         // 2
        "Sản phẩm & Lô",   // 3
        "Phiên đặt trước", // 4
        "Đơn hàng",        // 5
        "Pop-up & Gom",    // 6
        "Báo cáo",         // 7
        "Nhật ký",         // 8 (SuperAdmin Only)
        "Cấu hình"         // 9
    )

    // Check MFA first! If the logged in user is admin/super_admin and MFA is enabled, require MFA code (123456)
    val needsMfa = currentUser != null && 
            (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN) && 
            !isMfaVerified

    if (needsMfa) {
        AdminMfaGate(
            currentUser = currentUser,
            onConfirmMfa = onConfirmMfa,
            modifier = modifier
        )
        return
    }

    // Main layout
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBackground)
    ) {
        // Left Column: Navigation Rail for wide screens / standard side-menu
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(FarmSurfaceVariant)
                .border(width = 1.dp, color = FarmOutline.copy(alpha = 0.5f))
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            // Profile Box
            Card(
                colors = CardDefaults.cardColors(containerColor = FarmGreenDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser?.avatarEmoji ?: "👤", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Quản trị viên",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = currentUser?.role?.labelVi ?: "Admin",
                                color = FarmGoldContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Text(
                text = "HỆ THỐNG QUẢN TRỊ SÀN",
                style = MaterialTheme.typography.labelSmall,
                color = FarmTextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            // 10 Tabs Menu
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabTitles.size) { index ->
                    val isSelected = selectedTab == index
                    val isSuperAdminTab = index == 1 || index == 8
                    val hasAccess = !isSuperAdminTab || currentUser?.role == UserRole.SUPER_ADMIN

                    Surface(
                        color = if (isSelected) FarmGreenContainer else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasAccess) { selectedTab = index }
                            .testTag("admin_tab_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = getTabIcon(index),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = tabTitles[index],
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (!hasAccess) Color.Gray 
                                        else if (isSelected) FarmGreenDark 
                                        else FarmTextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (!hasAccess) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Yêu cầu SuperAdmin",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Logout / Terminate
            Button(
                onClick = onRevokeAllSessions,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thu hồi mọi phiên", fontSize = 11.sp)
            }
        }

        // Right Column: Main Content Dashboard View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ViewOverviewDashboard(sessions, activeSession, orders, orderItems, auditLogs, activeSessions, farmers, popups, onNavigateTab = { selectedTab = it })
                1 -> ViewUserManagement(users, currentUser, onChangeUserRole, onLockUnlockUser)
                2 -> ViewFarmerManagement(farmers, onApproveFarmer)
                3 -> ViewProductBatchManagement(products, lots, farmers, onUpdatePrice)
                4 -> ViewPreOrderSessionManagement(sessions, activeSession, popups, onLockSession, onAdvanceSession)
                5 -> ViewOrderManagement(orders, orderItems, onResolveShortage)
                6 -> ViewPopupAggregationManagement(popups, orders, orderItems, products, farmers)
                7 -> ViewReports(orders, orderItems, products)
                8 -> ViewAuditLogs(auditLogs)
                9 -> ViewConfigurationSecrets()
                else -> ViewOverviewDashboard(sessions, activeSession, orders, orderItems, auditLogs, activeSessions, farmers, popups, onNavigateTab = { selectedTab = it })
            }
        }
    }
}

// Icon mapper for 10 tabs
fun getTabIcon(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "📊" // Overview
        1 -> "👥" // Users
        2 -> "👨‍🌾" // Farmers
        3 -> "🌿" // Products
        4 -> "📅" // Sessions
        5 -> "📦" // Orders
        6 -> "🚚" // Pop-up & Aggregation
        7 -> "📈" // Reports
        8 -> "📜" // Audit logs
        9 -> "⚙️" // Settings
        else -> "📌"
    }
}

// 1. TỔNG QUAN VIEW
@Composable
fun ViewOverviewDashboard(
    sessions: List<PreOrderSessionEntity>,
    activeSession: PreOrderSessionEntity?,
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    auditLogs: List<AuditLogEntity>,
    activeSessions: List<UserSessionEntity>,
    farmers: List<FarmerProfileEntity> = emptyList(),
    popups: List<PopUpLocationEntity> = emptyList(),
    onNavigateTab: (Int) -> Unit = {}
) {
    val totalOrdersCount = orders.count { it.status != OrderStatus.CANCELLED }
    val totalVolumeKg = orderItems.filter { itm -> 
        orders.any { it.id == itm.orderId && it.status != OrderStatus.CANCELLED } 
    }.sumOf { it.quantity }
    val estimatedRevenue = orders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.totalAmount }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Bảng Điều Khiển Tổng Quan Sàn",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Bold,
                color = FarmTextPrimary
            )
            Text(
                text = "Tóm tắt hiệu suất hoạt động thu hoạch và giao dịch nông sản tại xã Bình Mỹ.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = FarmTextSecondary
            )
        }

        // Executive Quick Stats Row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Metric 1: Revenue
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = FarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ước tính doanh thu", fontSize = 13.sp, color = FarmTextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(formatVnd(estimatedRevenue), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                        Text("Từ $totalOrdersCount đơn chốt", fontSize = 11.sp, color = FarmTextSecondary)
                    }
                }

                // Metric 2: Volume
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = FarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sản lượng thu hoạch", fontSize = 13.sp, color = FarmTextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${"%.1f".format(totalVolumeKg)} kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                        Text("Đã đăng ký cắt rau", fontSize = 11.sp, color = FarmTextSecondary)
                    }
                }

                // Metric 3: Active Pre-order Session
                Card(
                    modifier = Modifier.weight(1.5f),
                    colors = CardDefaults.cardColors(containerColor = FarmSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đợt mở đặt trước nông sản", fontSize = 13.sp, color = FarmTextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(activeSession?.title ?: "Không có phiên mở", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary, maxLines = 1)
                        Text("Hạn đặt: ${activeSession?.let { formatDateTime(it.closeDateEpochMs) } ?: "Chưa thiết lập"}", fontSize = 11.sp, color = FarmTextSecondary)
                    }
                }
            }
        }

        // 5-Step Operational Pre-Order Model
        item {
            BinhMyWorkflowSection(
                activeSession = activeSession,
                orders = orders,
                orderItems = orderItems,
                farmers = farmers,
                popups = popups,
                defaultSelectedStep = 3,
                onNavigateToSessionDetail = { onNavigateTab(4) },
                onNavigateToOrders = { onNavigateTab(5) },
                onNavigateToTraceability = { onNavigateTab(3) }
            )
        }

        // Live Audit Activity logs (Dynamic feed instead of static technical list)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FarmSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nhật Ký Hoạt Động & Sự Kiện Hệ Thống",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FarmTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val recentLogs = auditLogs.take(5)
                    if (recentLogs.isEmpty()) {
                        Text("Không có hoạt động gần đây.", fontSize = 13.sp, color = FarmTextSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentLogs.forEachIndexed { index, log ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (log.isAnomaly) "⚠️" else "📝",
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.details,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (log.isAnomaly) StatusIssue else FarmTextPrimary
                                        )
                                        Text(
                                            text = "Thực hiện bởi: ${log.actorName} • ${formatDateTime(log.timestamp)}",
                                            fontSize = 11.sp,
                                            color = FarmTextSecondary
                                        )
                                    }
                                }
                                if (index < recentLogs.size - 1) {
                                    HorizontalDivider(thickness = 1.dp, color = FarmOutline.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. NGƯỜI DÙNG VIEW (SuperAdmin Only)
@Composable
fun ViewUserManagement(
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onChangeUserRole: (Long, UserRole) -> Unit,
    onLockUnlockUser: (Long, Boolean, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var showLockDialogUser by remember { mutableStateOf<UserEntity?>(null) }
    var lockReasonInput by remember { mutableStateOf("Nhập sai mật khẩu quá 5 lần hoặc vi phạm quy chế") }

    val filteredUsers = users.filter {
        (searchQuery.isBlank() || it.fullName.lowercase().contains(searchQuery.lowercase()) || it.username.contains(searchQuery)) &&
        (selectedRoleFilter == null || it.role == selectedRoleFilter)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Quản Lý Người Dùng & Phân Quyền", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Chức năng chỉ cấp cho Siêu quản trị viên (SUPER_ADMIN) để kiểm soát tài khoản.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        // Search & Filter Box
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên, tên đăng nhập...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                )

                // Dropdown Filter simulation
                Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                    UserRole.values().forEach { r ->
                        FilterChip(
                            selected = selectedRoleFilter == r,
                            onClick = { selectedRoleFilter = if (selectedRoleFilter == r) null else r },
                            label = { Text(r.name, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }

        // Users List Table
        items(filteredUsers) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.avatarEmoji, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Email: ${user.email} • Username: ${user.username}", fontSize = 11.sp, color = FarmTextSecondary)
                            }
                        }

                        // Locked status badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (user.isLocked) Color(0xFFFFEBEE) else FarmGreenContainer
                        ) {
                            Text(
                                text = if (user.isLocked) "ĐÃ KHÓA" else "HOẠT ĐỘNG",
                                color = if (user.isLocked) Color.Red else FarmGreenDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = FarmOutline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Role switching option
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vai trò:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            // Cycle through roles dynamically for demo
                            AssistChip(
                                onClick = {
                                    val nextRole = when (user.role) {
                                        UserRole.BUYER -> UserRole.FARMER
                                        UserRole.FARMER -> UserRole.ADMIN
                                        UserRole.ADMIN -> UserRole.POPUP_STAFF
                                        UserRole.POPUP_STAFF -> UserRole.VERIFIER
                                        UserRole.VERIFIER -> UserRole.SUPER_ADMIN
                                        UserRole.SUPER_ADMIN -> UserRole.BUYER
                                    }
                                    onChangeUserRole(user.id, nextRole)
                                },
                                label = { Text(user.role.labelVi, fontSize = 11.sp) }
                            )
                        }

                        // Lock / Unlock button
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (user.isLocked) {
                                Button(
                                    onClick = { onLockUnlockUser(user.id, false, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Mở khóa", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { showLockDialogUser = user },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Khóa tài khoản", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Lock Reason Dialog
    if (showLockDialogUser != null) {
        val u = showLockDialogUser!!
        AlertDialog(
            onDismissRequest = { showLockDialogUser = null },
            title = { Text("Khóa Tài Khoản Quản Trị") },
            text = {
                Column {
                    Text("Vui lòng nhập lý do khóa tài khoản cho ${u.fullName}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lockReasonInput,
                        onValueChange = { lockReasonInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLockUnlockUser(u.id, true, lockReasonInput)
                        showLockDialogUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Đồng ý Khóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialogUser = null }) { Text("Hủy") }
            }
        )
    }
}

// 3. NÔNG HỘ VIEW
@Composable
fun ViewFarmerManagement(
    farmers: List<FarmerProfileEntity>,
    onApproveFarmer: (Long, Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Quản Lý & Xét Duyệt Nông Hộ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Theo dõi chứng nhận VietGAP, diện tích đất canh tác xã Bình Mỹ.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        items(farmers) { farmer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(farmer.householdName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Người đại diện: ${farmer.representativeName} • ĐT: ${farmer.phone}", fontSize = 12.sp, color = FarmTextSecondary)
                            Text("Vùng đất: ${farmer.addressDetail}", fontSize = 12.sp, color = FarmTextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (farmer.isApprovedByAdmin) FarmGreenContainer else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = if (farmer.isApprovedByAdmin) "ĐÃ DUYỆT" else "CHỜ DUYỆT",
                                color = if (farmer.isApprovedByAdmin) FarmGreenDark else Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Diện tích: ${farmer.acreageHa} Ha • VietGAP: ${farmer.vietGapCertCode} • Hạn: ${farmer.vietGapValidUntil}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenDark
                    )
                    Text(text = "Rau trồng chính: ${farmer.mainCrops}", fontSize = 11.sp, color = FarmTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = FarmOutline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!farmer.isApprovedByAdmin) {
                            Button(
                                onClick = { onApproveFarmer(farmer.id, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Phê Duyệt Hồ Sơ", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onApproveFarmer(farmer.id, false) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Hủy Phê Duyệt", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. SẢN PHẨM & LÔ VIEW
@Composable
fun ViewProductBatchManagement(
    products: List<ProductEntity>,
    lots: List<ProductLotEntity>,
    farmers: List<FarmerProfileEntity>,
    onUpdatePrice: (Long, Double) -> Unit
) {
    var editingProductPrice by remember { mutableStateOf<ProductEntity?>(null) }
    var priceInputText by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Quản Lý Giá & Nhật Ký Lô Sản Phẩm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Điều chỉnh giá công khai, liên kết kiểm định VietGAP theo quy chế sàn.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        item {
            Text("Bảng Sản Phẩm Đang Mở Đặt", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FarmTextPrimary)
        }

        items(products) { prod ->
            val farmer = farmers.find { it.id == prod.farmerId }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Mã loại: ${prod.category} • Hộ trồng: ${farmer?.representativeName ?: "Bình Mỹ"}", fontSize = 11.sp, color = FarmTextSecondary)
                        Text(
                            text = "Giá hiện tại: ${formatVnd(prod.currentPricePerUnit)} / ${prod.unit} • Gom chốt tối đa: ${prod.originalEstimateKg} kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }

                    Button(
                        onClick = {
                            editingProductPrice = prod
                            priceInputText = prod.currentPricePerUnit.toInt().toString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Sửa giá", fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hồ Sơ Lô & Lịch Sử Xác Minh", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FarmTextPrimary)
        }

        items(lots) { lot ->
            val isVerified = lot.verificationStatus == com.example.data.model.VerificationStatus.VERIFIED
            Card(
                colors = CardDefaults.cardColors(containerColor = FarmSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lô: ${lot.lotCode}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FarmGreenDark)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isVerified) FarmGreenContainer else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (isVerified) "ĐÃ KIỂM ĐỊNH" else "CHỜ DUYỆT",
                                color = if (isVerified) FarmGreenDark else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("Nhật ký ruộng: ${lot.publicNotes.ifBlank { "Đang cập nhật" }}", fontSize = 11.sp)
                    Text("KS kiểm định: ${lot.verifierName ?: "Chưa ký"} • Ghi chú: ${lot.publicNotes.ifBlank { "Chưa có" }}", fontSize = 11.sp, color = FarmTextSecondary)
                    
                    if (lot.isShortage) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "⚠️ Cảnh báo hụt: Thiếu ${lot.shortageKg} kg - Lý do: ${lot.shortageNote ?: "Không rõ"}",
                                color = Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingProductPrice != null) {
        val p = editingProductPrice!!
        AlertDialog(
            onDismissRequest = { editingProductPrice = null },
            title = { Text("Cập Nhật Đơn Giá") },
            text = {
                Column {
                    Text("Cập nhật giá bán mới công khai cho ${p.name}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceInputText,
                        onValueChange = { priceInputText = it },
                        placeholder = { Text("Nhập số tiền VNĐ...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dPrice = priceInputText.toDoubleOrNull()
                        if (dPrice != null && dPrice > 0) {
                            onUpdatePrice(p.id, dPrice)
                        }
                        editingProductPrice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark)
                ) {
                    Text("Đồng Ý Sửa Giá")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProductPrice = null }) { Text("Hủy") }
            }
        )
    }
}

// 5. PHIÊN ĐẶT TRƯỚC VIEW
@Composable
fun ViewPreOrderSessionManagement(
    sessions: List<PreOrderSessionEntity>,
    activeSession: PreOrderSessionEntity?,
    popups: List<PopUpLocationEntity>,
    onLockSession: (Long) -> Unit,
    onAdvanceSession: (Long, SessionStatus) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Quản Lý Phiên Đặt Trước Nông Sản", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Tạo mới, chốt đặt trước, điều phối lịch nhận tại các trạm Pop-up.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        items(sessions) { s ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(s.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        SessionStatusBadge(status = s.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Hạn khóa đơn: ${formatDateTime(s.closeDateEpochMs)}", fontSize = 12.sp, color = FarmTextSecondary)
                    Text("Ngày nhận rau: ${formatDate(s.deliveryDateEpochMs)}", fontSize = 12.sp, color = FarmGreenDark, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = FarmOutline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (s.status == SessionStatus.OPEN) {
                            Button(
                                onClick = { onLockSession(s.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Khóa phiên", fontSize = 12.sp)
                            }
                        } else if (s.status == SessionStatus.LOCKED) {
                            Button(
                                onClick = { onAdvanceSession(s.id, SessionStatus.HARVESTING) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sang Thu Hoạch", fontSize = 12.sp)
                            }
                        } else if (s.status == SessionStatus.HARVESTING) {
                            Button(
                                onClick = { onAdvanceSession(s.id, SessionStatus.DISTRIBUTING) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Chuyển đến Pop-up", fontSize = 12.sp)
                            }
                        } else if (s.status == SessionStatus.DISTRIBUTING) {
                            Button(
                                onClick = { onAdvanceSession(s.id, SessionStatus.COMPLETED) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hoàn tất phiên", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. ĐƠN HÀNG VIEW
@Composable
fun ViewOrderManagement(
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    onResolveShortage: (Long, String, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOrders = orders.filter {
        searchQuery.isBlank() || it.orderCode.contains(searchQuery) || it.receiverName.lowercase().contains(searchQuery.lowercase())
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Giám Sát Đơn Đặt Trước", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Tra cứu danh sách đơn hàng, xử lý hụt hụt hoặc điều chỉnh đơn khi cần thiết.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Nhập mã đơn hàng hoặc tên khách hàng...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        items(filteredOrders) { order ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(order.orderCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        OrderStatusChip(status = order.status)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Người nhận: ${order.receiverName} • ĐT: ${order.receiverPhone}", fontSize = 12.sp)
                    Text("Địa chỉ: ${order.homeAddress ?: "Nhận tại điểm Pop-up"}", fontSize = 12.sp, color = FarmTextSecondary)
                    Text("Tổng giá trị: ${formatVnd(order.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)

                    if (order.shortageCompensationAction != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "Bồi hoàn hụt hàng: ${order.shortageCompensationAction}",
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Quick Shortage solver action for demo
                    if (order.status == OrderStatus.ISSUE_PENDING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    onResolveShortage(
                                        order.id,
                                        "Đã chuyển khoản hoàn tiền 18,000đ cho phần rau thiếu hụt",
                                        18000.0
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Giải quyết hoàn tiền 18k", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. POP-UP & GOM HÀNG VIEW
@Composable
fun ViewPopupAggregationManagement(
    popups: List<PopUpLocationEntity>,
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    products: List<ProductEntity>,
    farmers: List<FarmerProfileEntity>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Điều Phối Trạm Pop-up & Gom Hàng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Theo dõi số lượng hàng hóa và gán nhân viên phụ trách nhận hàng của bà con.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        items(popups) { p ->
            val pOrders = orders.filter { it.popupLocationId == p.id && it.status != OrderStatus.CANCELLED }
            val totalValue = pOrders.sumOf { it.totalAmount }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(color = FarmGreenContainer, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "TRẠM HOẠT ĐỘNG",
                                color = FarmGreenDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text("Địa chỉ trạm: ${p.address}", fontSize = 12.sp, color = FarmTextSecondary)
                    Text("Số đơn phân phối chốt: ${pOrders.size} đơn • Tổng tiền: ${formatVnd(totalValue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Danh sách nông sản gom về trạm này:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                    
                    products.forEach { prod ->
                        val itemQuantity = orderItems.filter { it.productId == prod.id && pOrders.any { o -> o.id == it.orderId } }.sumOf { it.quantity }
                        if (itemQuantity > 0) {
                            Text("• ${prod.name}: ${"%.1f".format(itemQuantity)} ${prod.unit}", fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

// 8. BÁO CÁO VIEW
@Composable
fun ViewReports(
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    products: List<ProductEntity>
) {
    val completedOrders = orders.filter { it.status == OrderStatus.COMPLETED }
    val totalRevenue = completedOrders.sumOf { it.totalAmount }
    val cancelledCount = orders.count { it.status == OrderStatus.CANCELLED }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Báo Cáo Hoạt Động & Đặt Trước Nông Sản", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FarmGreenDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Doanh Thu Hoàn Tất Thực Tế", color = FarmGoldContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatVnd(totalRevenue), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Text("HTX thu hộ và trả trực tiếp cho nông dân sau khi khấu trừ 3% phí vận hành.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tỉ Lệ Đã Giao", fontSize = 11.sp, color = FarmTextSecondary)
                        Text("${completedOrders.size} đơn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                        Text("Thành công 100%", fontSize = 10.sp, color = FarmTextSecondary)
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Số Đơn Bị Hủy", fontSize = 11.sp, color = FarmTextSecondary)
                        Text("$cancelledCount đơn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text("Bao gồm hủy do thời tiết", fontSize = 10.sp, color = FarmTextSecondary)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sản Lượng Nông Sản Đã Giao", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    products.forEach { prod ->
                        val completedQty = orderItems.filter { it.productId == prod.id && completedOrders.any { o -> o.id == it.orderId } }.sumOf { it.quantity }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(prod.name, fontSize = 12.sp)
                            Text("${"%.1f".format(completedQty)} ${prod.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                        }
                    }
                }
            }
        }
    }
}

// 9. NHẬT KÝ KIỂM TOÁN VIEW (SuperAdmin Only)
@Composable
fun ViewAuditLogs(auditLogs: List<AuditLogEntity>) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Nhật Ký Bảo Mật & Hệ Thống", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Bản ghi bất biến của mọi thao tác nhạy cảm, nâng quyền hoặc khóa tài khoản.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        if (auditLogs.isEmpty()) {
            item {
                Text("Không có nhật ký bảo mật nào.", fontSize = 12.sp, color = FarmTextSecondary)
            }
        } else {
            items(auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.isAnomaly) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (log.isAnomaly) "🚨" else "📜", modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = log.action,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (log.isAnomaly) Color.Red else FarmGreenDark
                                )
                            }
                            Text(
                                text = df.format(Date(log.timestamp)),
                                fontSize = 10.sp,
                                color = FarmTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Người thực hiện: ${log.actorName} (${log.actorRole})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Mô tả: ${log.details}", fontSize = 11.sp)
                        Text(
                            text = "Kết quả: ${log.result}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (log.result == "SUCCESS") FarmGreenPrimary else Color.Red
                        )
                    }
                }
            }
        }
    }
}

// 10. CẤU HÌNH BẢO MẬT & SECRETS MASKING
@Composable
fun ViewConfigurationSecrets() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Cấu Hình Sàn & Masking Secrets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FarmGreenDark)
            Text("Quản lý thông số sàn Bình Mỹ và che giấu toàn bộ API Keys/Credentials.", style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông Số Hệ Thống Che Giấu (Secret Masking)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    val secrets = listOf(
                        "GOOGLE_CLIENT_ID" to "99668376187-****************.apps.googleusercontent.com",
                        "GOOGLE_CLIENT_SECRET" to "gsec_*******************************",
                        "FIREBASE_PROJECT_ID" to "binhmy-farm-************",
                        "DATABASE_PASSPHRASE" to "pass_*****************************"
                    )

                    secrets.forEach { (key, valStr) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(key, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FarmTextSecondary)
                            Text(valStr, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = FarmTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Chính Sách & Quy Tắc Vận Hành Sàn", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FarmGreenDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("• Thời gian khóa đặt trước: Chậm nhất 24 giờ trước thời điểm thu hoạch.", fontSize = 12.sp)
                    Text("• Điều kiện duyệt nông hộ: Phải có đất đai không nhiễm kim loại nặng, đạt kiểm định VietGAP.", fontSize = 12.sp)
                    Text("• Chính sách thiếu hụt: 100% bồi hoàn công khai bằng chuyển khoản, hoàn tiền mặt trực tiếp.", fontSize = 12.sp)
                    Text("• Phí sàn: 3% dùng để bảo trì điểm Pop-up và duy trì ứng dụng.", fontSize = 12.sp)
                }
            }
        }
    }
}

// Admin MFA Entrance Gate UI Component
@Composable
fun AdminMfaGate(
    currentUser: UserEntity?,
    onConfirmMfa: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var codeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FarmBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🛡️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Xác Thực Hai Lớp (MFA)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenDark
                )
                Text(
                    text = "Xin chào ${currentUser?.fullName}! Bạn đang truy cập tài khoản quản trị. Vui lòng nhập mã bảo mật OTP từ thiết bị để tiếp tục.",
                    fontSize = 12.sp,
                    color = FarmTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 6) codeInput = it },
                    label = { Text("Mã xác thực OTP (6 chữ số)") },
                    placeholder = { Text("Ví dụ: 123456") },
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mfa_code"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (codeInput.isNotBlank()) {
                            onConfirmMfa(codeInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_mfa_confirm")
                ) {
                    Text("Xác nhận danh tính", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Để phục vụ demo, hãy dùng mã 123456 hoặc 888888 để vượt qua lớp bảo mật MFA.",
                    fontSize = 11.sp,
                    color = FarmTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
