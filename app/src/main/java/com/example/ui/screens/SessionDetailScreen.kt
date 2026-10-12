package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.PopUpLocationEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.model.SessionStatus
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: PreOrderSessionEntity?,
    popups: List<PopUpLocationEntity>,
    currentUserRole: UserRole,
    onBack: () -> Unit,
    onLockSession: (Long) -> Unit,
    onAdvanceSession: (Long, SessionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi Tiết Phiên Đặt Trước", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_session")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmBackground,
                    titleContentColor = FarmGreenDark
                )
            )
        }
    ) { paddingValues ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy thông tin phiên đặt trước.")
            }
            return@Scaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoDataBanner()

            // Header Session Card
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
                        SessionStatusBadge(status = session.status)
                        Text(
                            text = session.sessionCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = session.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mở nhận đơn:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(formatDateTime(session.openDateEpochMs), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hạn khóa đơn (Cutoff):", color = FarmGoldContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(formatDateTime(session.closeDateEpochMs), color = FarmGoldContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Thời gian nhận hàng:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(formatDate(session.deliveryDateEpochMs) + " (Sáng 06:30 - 11:30)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 5-Step Operational Pre-Order Model
            val currentStage = when (session.status) {
                SessionStatus.OPEN -> 2
                SessionStatus.LOCKED -> 3
                SessionStatus.HARVESTING -> 4
                SessionStatus.DISTRIBUTING, SessionStatus.COMPLETED -> 5
                else -> 1
            }
            BinhMyWorkflowSection(
                activeSession = session,
                defaultSelectedStep = currentStage,
                onNavigateToSessionDetail = { /* already here */ },
                onNavigateToOrders = onBack
            )

            // Cancellation and Policy Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = FarmEarthSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chính Sách Khóa Đơn & Đổi Hủy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = session.cancellationPolicy,
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Sau hạn khóa đơn, hệ thống tự động tổng hợp số kg rau muống và nông sản theo từng hộ để chốt danh sách thu hoạch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FarmTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Pop-up Pickup Locations Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = FarmGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Danh Sách Điểm Nhận Pop-up Cho Phiên",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    popups.forEachIndexed { index, p ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FarmSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(p.address, style = MaterialTheme.typography.bodySmall, color = FarmTextSecondary)
                                Text("Giờ hoạt động: ${p.operatingHours}", style = MaterialTheme.typography.labelSmall, color = FarmGreenDark)
                                if (p.supportsHomeDelivery) {
                                    Text("Hỗ trợ giao tận nhà: Phí ${formatVnd(p.baseDeliveryFee)}", style = MaterialTheme.typography.labelSmall, color = FarmEarthSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Admin Operations Control Panel (Visible for Admin)
            if (currentUserRole == UserRole.ADMIN) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Điều Khiển Vận Hành Phiên (Dành Cho Quản Trị)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Trạng thái hiện tại: ${session.status.labelVi}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (session.status == SessionStatus.OPEN) {
                            Button(
                                onClick = { onLockSession(session.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_admin_lock_session")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Khóa Đơn Ngay (Chốt Kg Thu Hoạch)", fontWeight = FontWeight.Bold)
                            }
                        } else if (session.status == SessionStatus.LOCKED) {
                            Button(
                                onClick = { onAdvanceSession(session.id, SessionStatus.HARVESTING) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_admin_harvest_session")
                            ) {
                                Icon(Icons.Default.Agriculture, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chuyển Sang: Đang Thu Hoạch", fontWeight = FontWeight.Bold)
                            }
                        } else if (session.status == SessionStatus.HARVESTING) {
                            Button(
                                onClick = { onAdvanceSession(session.id, SessionStatus.DISTRIBUTING) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_admin_distribute_session")
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chuyển Sang: Chuyển Đến Pop-up", fontWeight = FontWeight.Bold)
                            }
                        } else if (session.status == SessionStatus.DISTRIBUTING) {
                            Button(
                                onClick = { onAdvanceSession(session.id, SessionStatus.COMPLETED) },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_admin_complete_session")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hoàn Tất Toàn Bộ Phiên", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
