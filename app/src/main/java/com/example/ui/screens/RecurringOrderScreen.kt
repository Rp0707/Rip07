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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PopUpLocationEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.RecurringScheduleEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DeliveryType
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringStatus
import com.example.ui.components.DemoDataBanner
import com.example.ui.components.formatDate
import com.example.ui.components.formatVnd
import com.example.ui.theme.*

@Composable
fun RecurringOrderScreen(
    schedules: List<RecurringScheduleEntity>,
    products: List<ProductEntity>,
    popups: List<PopUpLocationEntity>,
    currentUser: UserEntity?,
    onToggleStatus: (Long, RecurringStatus) -> Unit,
    onCreateSchedule: (RecurringScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recurring_order_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DemoDataBanner()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Đặt Lịch Định Kỳ Rau Sạch",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tự động lên đơn mỗi tuần • Yên tâm có rau sạch ăn đều",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo lịch mới", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (schedules.isEmpty()) {
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
                        Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Chưa có lịch đặt định kỳ nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Đăng ký nhận rau muống và rau củ hằng tuần để không lo hết suất đặt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                val product = products.find { it.id == schedule.productId }
                val popup = popups.find { it.id == schedule.popupLocationId }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product?.name ?: "Nông sản định kỳ",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (schedule.status) {
                                    RecurringStatus.ACTIVE -> FarmGreenContainer
                                    RecurringStatus.PAUSED -> Color(0xFFFFF3E0)
                                    RecurringStatus.SKIPPED_ONCE -> Color(0xFFEEEEEE)
                                }
                            ) {
                                Text(
                                    text = schedule.status.labelVi,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (schedule.status) {
                                        RecurringStatus.ACTIVE -> FarmGreenPrimary
                                        RecurringStatus.PAUSED -> Color(0xFFD84315)
                                        RecurringStatus.SKIPPED_ONCE -> Color.DarkGray
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚖️ Số lượng: ${"%.0f".format(schedule.quantity)} ${product?.unit ?: "kg"}  •  🔄 Chu kỳ: ${schedule.frequency.labelVi}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📅 Kỳ nhận kế tiếp: ${formatDate(schedule.nextDeliveryDateEpochMs)}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (schedule.deliveryType == DeliveryType.POPUP_PICKUP) {
                                "📍 Điểm nhận: ${popup?.name ?: "Điểm Pop-up Bình Mỹ"}"
                            } else {
                                "🏠 Giao tận nhà: ${schedule.homeAddress ?: ""}"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (schedule.status == RecurringStatus.ACTIVE) {
                                OutlinedButton(
                                    onClick = { onToggleStatus(schedule.id, RecurringStatus.SKIPPED_ONCE) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Bỏ qua 1 kỳ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = { onToggleStatus(schedule.id, RecurringStatus.PAUSED) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Tạm dừng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onToggleStatus(schedule.id, RecurringStatus.ACTIVE) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tiếp tục đặt", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Create Schedule
    if (showAddDialog) {
        var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 1L) }
        var quantity by remember { mutableStateOf(2.0) }
        var frequency by remember { mutableStateOf(RecurringFrequency.WEEKLY) }
        var selectedPopupId by remember { mutableStateOf(popups.firstOrNull()?.id ?: 1L) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tạo Lịch Đặt Nông Sản Định Kỳ") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Chọn nông sản:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    products.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedProductId == p.id,
                                onClick = { selectedProductId = p.id }
                            )
                            Text(p.name, fontSize = 12.sp)
                        }
                    }

                    Text("Tần suất nhận rau:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurringFrequency.values().forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.labelVi, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Số lượng mỗi kỳ (kg):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (quantity > 1.0) quantity -= 1.0 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("%.0f".format(quantity), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { quantity += 1.0 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val schedule = RecurringScheduleEntity(
                            buyerId = currentUser?.id ?: 1L,
                            productId = selectedProductId,
                            quantity = quantity,
                            frequency = frequency,
                            deliveryType = DeliveryType.POPUP_PICKUP,
                            popupLocationId = selectedPopupId,
                            homeAddress = null,
                            receiverName = currentUser?.fullName ?: "Người mua",
                            receiverPhone = currentUser?.phone ?: "0900000000",
                            nextDeliveryDateEpochMs = System.currentTimeMillis() + (7 * 24 * 3600 * 1000L)
                        )
                        onCreateSchedule(schedule)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Lưu lịch định kỳ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}
