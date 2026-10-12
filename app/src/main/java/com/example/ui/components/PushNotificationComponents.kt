package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AppNotificationEntity
import com.example.data.model.NotificationType
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Animated Heads-Up Dropdown Push Notification Banner (Top of Screen)
 */
@Composable
fun HeadsUpPushNotificationBanner(
    notification: AppNotificationEntity?,
    onDismiss: () -> Unit,
    onOpenDetails: (AppNotificationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(notification?.id) {
        if (notification != null) {
            progress.snapTo(1f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (notification != null) {
            val (bannerColor, accentColor, typeLabel) = when (notification.type) {
                NotificationType.SESSION_OPENED -> Triple(Color(0xFF1B5E20), FarmGoldContainer, "PHIÊN MỚI")
                NotificationType.ORDER_CONFIRMED -> Triple(Color(0xFF2E7D32), Color.White, "ĐÃ XÁC NHẬN")
                NotificationType.ORDER_PREPARING -> Triple(Color(0xFF00695C), Color(0xFF80CBC4), "ĐANG THU HOẠCH")
                NotificationType.ORDER_ARRIVED_POPUP -> Triple(Color(0xFF4A148C), Color(0xFFE1BEE7), "ĐÃ ĐẾN POP-UP")
                NotificationType.SHORTAGE_ALERT -> Triple(Color(0xFFB71C1C), Color(0xFFFFCDD2), "THIẾU HÀNG")
                NotificationType.ADMIN_BROADCAST -> Triple(Color(0xFFE65100), Color(0xFFFFE082), "THÔNG BÁO TỪ QTV")
                NotificationType.ORDER_DELIVERED -> Triple(Color(0xFF2E7D32), Color.White, "HOÀN TẤT")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable {
                        onOpenDetails(notification)
                        onDismiss()
                    }
                    .testTag("push_notification_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bannerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji icon bubble
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notification.type.iconEmoji,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Vừa xong",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress bar indicator for 5s auto-dismiss
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color.White.copy(alpha = 0.6f),
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

/**
 * Full Notification Center Dialog (Accessible from any screen via TopAppBar bell)
 */
@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotificationEntity>,
    onDismiss: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDeleteNotification: (Long) -> Unit,
    onSelectNotification: (AppNotificationEntity) -> Unit,
    onTestPush: (NotificationType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    val filters = listOf("Tất cả", "Chưa đọc", "Tiến trình đơn", "Thiếu hàng", "Từ Quản trị", "Thử nghiệm")

    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Chưa đọc" -> notifications.filter { !it.isRead }
            "Tiến trình đơn" -> notifications.filter {
                it.type == NotificationType.ORDER_CONFIRMED ||
                        it.type == NotificationType.ORDER_PREPARING ||
                        it.type == NotificationType.ORDER_ARRIVED_POPUP ||
                        it.type == NotificationType.ORDER_DELIVERED
            }
            "Thiếu hàng" -> notifications.filter { it.type == NotificationType.SHORTAGE_ALERT }
            "Từ Quản trị" -> notifications.filter { it.type == NotificationType.ADMIN_BROADCAST }
            else -> notifications
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("notification_center_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FarmGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = FarmGreenDark, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Trung Tâm Thông Báo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = "Cập nhật theo thời gian thực cho mọi vai trò",
                                style = MaterialTheme.typography.labelSmall,
                                color = FarmTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FarmGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Có ${filteredList.size} thông báo",
                        style = MaterialTheme.typography.labelSmall,
                        color = FarmTextSecondary
                    )

                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = onMarkAllAsRead,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Đọc tất cả", fontSize = 11.sp, color = FarmGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = FarmOutline.copy(alpha = 0.4f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Simulator Sandbox section when "Thử nghiệm" is picked
                if (selectedFilter == "Thử nghiệm") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FarmGoldContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🧪 BỘ THỬ NGHIỆM PHÁT THÔNG BÁO ĐẨY TỨC THÌ:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmGoldOnContainer
                            )
                            Text(
                                text = "Bấm bất kỳ nút nào dưới đây để phát thông báo đẩy trực tiếp lên màn hình:",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = FarmGoldOnContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val testTypes = listOf(
                                NotificationType.SESSION_OPENED to "1. Phiên mới mở",
                                NotificationType.ORDER_CONFIRMED to "2. Đơn đã xác nhận",
                                NotificationType.ORDER_PREPARING to "3. Đang thu hoạch",
                                NotificationType.ORDER_ARRIVED_POPUP to "4. Đã đến Pop-up",
                                NotificationType.SHORTAGE_ALERT to "5. Báo thiếu hàng",
                                NotificationType.ADMIN_BROADCAST to "6. Tin từ QTV"
                            )

                            testTypes.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { (type, label) ->
                                        OutlinedButton(
                                            onClick = { onTestPush(type) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Notification list
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Không có thông báo nào trong mục này", color = FarmTextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                onMarkAsRead = { onMarkAsRead(notif.id) },
                                onDelete = { onDeleteNotification(notif.id) },
                                onClick = {
                                    onMarkAsRead(notif.id)
                                    onSelectNotification(notif)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotificationEntity,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (cardBg, borderBrush, typeLabel, typeColor) = when (notification.type) {
        NotificationType.SESSION_OPENED -> Quadruple(FarmGreenContainer.copy(alpha = 0.6f), FarmGreenPrimary, "PHIÊN MỚI", FarmGreenDark)
        NotificationType.ORDER_CONFIRMED -> Quadruple(FarmGreenContainer.copy(alpha = 0.4f), FarmGreenLight, "ĐÃ XÁC NHẬN", FarmGreenDark)
        NotificationType.ORDER_PREPARING -> Quadruple(Color(0xFFE0F2F1), Color(0xFF00695C), "ĐANG THU HOẠCH", Color(0xFF004D40))
        NotificationType.ORDER_ARRIVED_POPUP -> Quadruple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), "ĐÃ ĐẾN POP-UP", Color(0xFF4A148C))
        NotificationType.SHORTAGE_ALERT -> Quadruple(Color(0xFFFFEBEE), Color(0xFFC62828), "THIẾU HÀNG", Color(0xFFB71C1C))
        NotificationType.ADMIN_BROADCAST -> Quadruple(Color(0xFFFFF3E0), Color(0xFFE65100), "TỪ QUẢN TRỊ", Color(0xFFBF360C))
        NotificationType.ORDER_DELIVERED -> Quadruple(FarmGreenContainer.copy(alpha = 0.3f), FarmGreenDark, "HOÀN TẤT", FarmGreenDark)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${notification.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) FarmSurfaceVariant else cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(notification.type.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (notification.priority == "URGENT") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFC62828)
                        ) {
                            Text(
                                text = "KHẨN",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDateTime(notification.timestampEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = FarmTextSecondary
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                color = FarmTextPrimary
            )

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = FarmTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (!notification.isRead) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "● Chưa đọc",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = FarmGreenDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
