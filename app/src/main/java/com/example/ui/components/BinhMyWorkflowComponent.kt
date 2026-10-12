package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
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
import com.example.data.local.entity.*
import com.example.data.model.OrderStatus
import com.example.data.model.SessionStatus
import com.example.ui.theme.*

// Exact Chevron / Arrow Shape as shown in the design specification
val WorkflowChevronShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val d = (h * 0.36f).coerceAtMost(36f) // depth of notch and point

    moveTo(0f, 0f)
    lineTo(w - d, 0f)
    lineTo(w, h / 2f)
    lineTo(w - d, h)
    lineTo(0f, h)
    lineTo(d, h / 2f)
    close()
}

data class WorkflowStepData(
    val stepNumber: String,
    val title: String,
    val description: String,
    val stageTag: String
)

val BinhMy5Steps = listOf(
    WorkflowStepData(
        stepNumber = "01",
        title = "MỞ PHIÊN",
        description = "Nông hộ đăng lô; quản trị đặt hạn khóa và ngày nhận.",
        stageTag = "OPEN"
    ),
    WorkflowStepData(
        stepNumber = "02",
        title = "KHÁCH ĐẶT THEO LỊCH",
        description = "Chọn số kg, Pop-up hoặc giao tận nhà; sửa trước hạn.",
        stageTag = "ORDERING"
    ),
    WorkflowStepData(
        stepNumber = "03",
        title = "CHỐT NHU CẦU",
        description = "Hệ thống tạo đơn từng kỳ và tổng hợp kg theo nông hộ.",
        stageTag = "LOCKED"
    ),
    WorkflowStepData(
        stepNumber = "04",
        title = "XÁC MINH VÀ GOM HÀNG",
        description = "Kiểm tra hồ sơ lô, gắn QR và vận chuyển trung chuyển.",
        stageTag = "HARVESTING"
    ),
    WorkflowStepData(
        stepNumber = "05",
        title = "NHẬN HÀNG, ĐO KẾT QUẢ",
        description = "Quét mã nhận; theo dõi số đơn, hủy và tỷ lệ đặt lại.",
        stageTag = "DISTRIBUTING"
    )
)

/**
 * 5-Step Operational Pre-order Chevron Workflow Component
 * Faithfully reproduces the exact visual arrows and operational logic of Sàn Nông Sản Bình Mỹ.
 */
@Composable
fun BinhMyWorkflowSection(
    activeSession: PreOrderSessionEntity? = null,
    orders: List<OrderEntity> = emptyList(),
    orderItems: List<OrderItemEntity> = emptyList(),
    farmers: List<FarmerProfileEntity> = emptyList(),
    popups: List<PopUpLocationEntity> = emptyList(),
    defaultSelectedStep: Int = 3, // Step 03 highlighted by default as in user's image
    onNavigateToProducts: () -> Unit = {},
    onNavigateToSessionDetail: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToTraceability: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStepIndex by remember { mutableIntStateOf(defaultSelectedStep) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("binh_my_5_step_workflow"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FarmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌾", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mô Hình Vận Hành Đặt Trước 5 Bước",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmTextPrimary
                        )
                        Text(
                            text = "Quy trình cắt sáng tươi ngon & truy xuất nguồn gốc xã Bình Mỹ",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FarmGreenContainer
                ) {
                    Text(
                        text = "Chuẩn VietGAP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Chevrons Stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BinhMy5Steps.forEachIndexed { index, step ->
                    val stepNum = index + 1
                    val isSelected = selectedStepIndex == stepNum

                    WorkflowChevronItem(
                        step = step,
                        isSelected = isSelected,
                        onClick = { selectedStepIndex = stepNum }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Dynamic Step Detail Card
            WorkflowStepDetailPanel(
                selectedStep = selectedStepIndex,
                activeSession = activeSession,
                orders = orders,
                orderItems = orderItems,
                farmers = farmers,
                popups = popups,
                onNavigateToProducts = onNavigateToProducts,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
                onNavigateToOrders = onNavigateToOrders,
                onNavigateToTraceability = onNavigateToTraceability
            )
        }
    }
}

/**
 * Single Chevron Arrow Component
 */
@Composable
fun WorkflowChevronItem(
    step: WorkflowStepData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Exact visual matching:
    // When selected: Purple boundary rectangle (Color(0xFF6D28D9) / Color(0xFF7C3AED)), exactly as in photo
    val outerModifier = if (isSelected) {
        Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = Color(0xFF6D28D9), shape = RoundedCornerShape(4.dp))
            .padding(2.dp)
            .clickable { onClick() }
    } else {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    }

    Box(
        modifier = outerModifier.testTag("workflow_step_${step.stepNumber}")
    ) {
        // Deep Green Chevron Shape matching #1E5D3A / #246B45
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(WorkflowChevronShape)
                .background(Color(0xFF1E5D3A))
                .padding(start = 28.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Number: 01, 02, 03, 04, 05
                Text(
                    text = step.stepNumber,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.width(42.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Title and Subtitle Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF6D28D9)
                    ) {
                        Text(
                            text = "ĐANG CHỌN",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Deep Dive Panel for the Selected Workflow Step
 */
@Composable
fun WorkflowStepDetailPanel(
    selectedStep: Int,
    activeSession: PreOrderSessionEntity?,
    orders: List<OrderEntity>,
    orderItems: List<OrderItemEntity>,
    farmers: List<FarmerProfileEntity>,
    popups: List<PopUpLocationEntity>,
    onNavigateToProducts: () -> Unit,
    onNavigateToSessionDetail: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToTraceability: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FarmGreenContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (selectedStep) {
                1 -> {
                    // 01 MỞ PHIÊN
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết Bước 01: Mở Phiên Đặt Trước",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nông hộ canh tác tại xã Bình Mỹ đăng ký số kg dự kiến thu hoạch theo luống VietGAP. Ban Quản trị thiết lập phiên và công bố thời hạn chốt đơn công khai.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Phiên hiện tại: ${activeSession?.title ?: "Đang chuẩn bị phiên mới"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmTextPrimary
                    )
                    Text(
                        text = "• Hạn khóa đơn: ${activeSession?.let { formatDateTime(it.closeDateEpochMs) } ?: "N/A"}",
                        fontSize = 12.sp,
                        color = FarmTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSessionDetail,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Xem Chi Tiết Phiên", fontSize = 12.sp)
                    }
                }

                2 -> {
                    // 02 KHÁCH ĐẶT THEO LỊCH
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛒", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết Bước 02: Khách Đặt Theo Lịch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Khách hàng chọn số kg rau muống nước, rau củ và chọn nhận tại điểm Pop-up gần nhà hoặc giao tận nơi. Có thể đặt định kỳ hàng tuần và tự do sửa/hủy đơn trước giờ khóa phiên.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Mạng lưới điểm nhận: ${popups.size} trạm Pop-up tại Bình Mỹ và khu dân cư.",
                        fontSize = 12.sp,
                        color = FarmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onNavigateToProducts,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Khám Phá Nông Sản", fontSize = 12.sp)
                        }
                    }
                }

                3 -> {
                    // 03 CHỐT NHU CẦU (The one in the image!)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚖️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết Bước 03: Chốt Nhu Cầu & Tổng Hợp Sản Lượng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF6D28D9)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Khi phiên đến hạn khóa (Cutoff), hệ thống tự động khóa tính năng hủy/sửa và tổng hợp chính xác số kg rau muống thực tế theo từng hộ nông dân để gặt sáng sớm, tránh lãng phí và đảm bảo rau luôn tươi mới 100%.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Demand Summary Table
                    val totalKg = orderItems.sumOf { it.quantity }
                    val validOrders = orders.filter { it.status != OrderStatus.CANCELLED }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FarmOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tổng đơn hàng chốt:", fontSize = 12.sp, color = FarmTextSecondary)
                                Text("${validOrders.size} đơn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tổng sản lượng cần cắt:", fontSize = 12.sp, color = FarmTextSecondary)
                                Text("${"%.1f".format(totalKg)} kg", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = FarmOutline)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Phân bổ dự kiến theo nông hộ Bình Mỹ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextPrimary)
                            farmers.take(3).forEach { farmer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${farmer.householdName} (${farmer.addressDetail})", fontSize = 11.sp, color = FarmTextSecondary)
                                    Text(if (farmer.hasVietGap) "Đạt VietGAP" else "Đang theo dõi", fontSize = 11.sp, color = FarmGreenPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSessionDetail,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Xem Điều Khiển Chốt Phiên", fontSize = 12.sp)
                    }
                }

                4 -> {
                    // 04 XÁC MINH VÀ GOM HÀNG
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết Bước 04: Xác Minh Và Gom Hàng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tổ kiểm định đối chiếu nhật ký nước tưới và phân bón sinh học tại ruộng, gắn mã QR lô nông sản và vận chuyển trung chuyển về trạm gom.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Mã QR truy xuất gắn liền từng bó rau giúp người tiêu dùng kiểm tra ngày cắt và người trồng.",
                        fontSize = 12.sp,
                        color = FarmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToTraceability,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Truy Xuất Nguồn Gốc QR", fontSize = 12.sp)
                    }
                }

                5 -> {
                    // 05 NHẬN HÀNG, ĐO KẾT QUẢ
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chi tiết Bước 05: Nhận Hàng & Đo Lường Kết Quả",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FarmTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Người mua đưa mã nhận (PU-xxx) hoặc mã QR tại trạm Pop-up để nhận rau sạch. Hệ thống tự động ghi nhận tỷ lệ hủy đơn, xử lý đền bù nếu hụt sản lượng và theo dõi tỷ lệ khách hàng đặt lại cho các đợt tiếp theo.",
                        fontSize = 12.sp,
                        color = FarmTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Mục tiêu: Tỷ lệ đặt lại > 85%, độ tươi giòn đạt 100% trong ngày.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FarmGreenPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToOrders,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Xem Đơn Đặt & Mã Nhận", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
