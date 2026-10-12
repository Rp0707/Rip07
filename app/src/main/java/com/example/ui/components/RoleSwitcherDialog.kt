package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSwitcherDialog(
    currentUser: UserEntity?,
    users: List<UserEntity>,
    onSelectUser: (UserEntity) -> Unit,
    onDismiss: () -> Unit,
    
    // Security flows binding
    onLoginPassword: (username: String, passwordRaw: String) -> Unit,
    onLoginGoogle: (email: String, name: String, sub: String, confirmPass: String?) -> Unit,
    uiMessage: String? = null
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Demo Quick Switch, 1 = Secure Login
    
    // Password state
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    
    // Google Sign-In state simulation
    var showGoogleMockPopup by remember { mutableStateOf(false) }
    var selectedGoogleAccountEmail by remember { mutableStateOf("") }
    var selectedGoogleAccountName by remember { mutableStateOf("") }
    var googleConfirmPasswordLink by remember { mutableStateOf("") }

    val emailLinkRequired = uiMessage != null && uiMessage.startsWith("EMAIL_MATCH_LINK_REQUIRED")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("role_switcher_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Xác Thực & Tài Khoản",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                        Text(
                            text = "Đăng nhập Google, mật khẩu & chuyển demo",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = FarmSurfaceVariant,
                    contentColor = FarmGreenPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Chuyển nhanh (Demo)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🔒 Đăng nhập bảo mật", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeTab == 0) {
                    // TAB 0: Demo Quick Switch (Original screen)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(users) { user ->
                            val isSelected = currentUser?.id == user.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectUser(user)
                                        onDismiss()
                                    }
                                    .testTag("role_option_${user.role.name.lowercase()}"),
                                color = if (isSelected) FarmGreenContainer else FarmSurfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) FarmGreenPrimary else Color.LightGray.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = user.avatarEmoji, fontSize = 18.sp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = FarmTextPrimary
                                        )
                                        Text(
                                            text = "Vai trò: ${user.role.labelVi} ${if (user.isLocked) " (ĐÃ KHÓA)" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (user.isLocked) Color.Red 
                                                    else if (isSelected) FarmGreenPrimary 
                                                    else FarmEarthSecondary
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Đang chọn",
                                            tint = FarmGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = FarmGoldContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Thử nghiệm nhanh: Bạn có thể chọn trực tiếp tài khoản bên trên để đổi nhanh vai trò mà không cần nhập mật khẩu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FarmGoldOnContainer,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 14.sp,
                            fontSize = 10.sp
                        )
                    }

                } else {
                    // TAB 1: Secure Login Form (Layer 2 & Google Sign-in OIDC simulation)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (emailLinkRequired) {
                            // Linking account workflow
                            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🔗 YÊU CẦU LIÊN KẾT TÀI KHOẢN", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 11.sp)
                                    Text("Hệ thống phát hiện email $selectedGoogleAccountEmail đã tồn tại. Để bảo mật tránh giả mạo, hãy nhập mật khẩu cũ để liên kết Google account.", fontSize = 10.sp)
                                }
                            }

                            OutlinedTextField(
                                value = googleConfirmPasswordLink,
                                onValueChange = { googleConfirmPasswordLink = it },
                                label = { Text("Mật khẩu cũ của tài khoản") },
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                            )

                            Button(
                                onClick = {
                                    onLoginGoogle(selectedGoogleAccountEmail, selectedGoogleAccountName, "gsub_" + selectedGoogleAccountEmail.hashCode(), googleConfirmPasswordLink)
                                    googleConfirmPasswordLink = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Liên kết và Đăng nhập", fontWeight = FontWeight.Bold)
                            }

                        } else {
                            // Standard form
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Tên đăng nhập") },
                                placeholder = { Text("Ví dụ: admin_demo") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_username"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Mật khẩu") },
                                placeholder = { Text("Ví dụ: admin123") },
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_password"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FarmGreenPrimary)
                            )

                            Button(
                                onClick = {
                                    if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        onLoginPassword(usernameInput.trim(), passwordInput.trim())
                                        usernameInput = ""
                                        passwordInput = ""
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_submit_login")
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đăng nhập mật khẩu", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("--- HOẶC TIẾP TỤC VỚI GOOGLE ---", fontSize = 10.sp, color = FarmTextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(modifier = Modifier.height(4.dp))

                            // "Tiếp tục với Google" Button
                            OutlinedButton(
                                onClick = { showGoogleMockPopup = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_google_sign_in"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FarmTextPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.LightGray))
                            ) {
                                Text("🌐 Tiếp tục với Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Google GIS login account picker simulator modal (Layer 2)
    if (showGoogleMockPopup) {
        Dialog(onDismissRequest = { showGoogleMockPopup = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("Google Accounts (OIDC)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("Chọn tài khoản liên kết Google Identity Services:", fontSize = 11.sp, color = FarmTextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    val accounts = listOf(
                        "thutrang.buyer@binhmy.vn" to "Chị Lê Thu Trang",
                        "chunambinhmy@binhmy.vn" to "Chú Nguyễn Văn Năm",
                        "dieuphoi@htxbinhmy.gov.vn" to "Ban Điều Phối HTX Bình Mỹ",
                        "moi.user@gmail.com" to "Khách Hàng Mới Chưa Có Tài Khoản"
                    )

                    accounts.forEach { (email, name) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FarmSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedGoogleAccountEmail = email
                                    selectedGoogleAccountName = name
                                    showGoogleMockPopup = false
                                    // Trigger Google sign-in workflow on ViewModel
                                    onLoginGoogle(email, name, "google_sub_" + email.hashCode().toString(), null)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(email, fontSize = 10.sp, color = FarmTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showGoogleMockPopup = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Hủy bỏ")
                    }
                }
            }
        }
    }
}
