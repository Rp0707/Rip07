package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.zIndex
import com.example.data.local.entity.AppNotificationEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    PRODUCTS,
    ORDERS,
    TRACEABILITY,
    OPERATIONS,
    CART,
    PRODUCT_DETAIL,
    SESSION_DETAIL,
    AI_CHAT,
    FIRESTORE_PRODUCTS,
    ORDER_TRACKING,
    PAYMENT_PORTAL
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BinhMyPreOrderApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinhMyPreOrderApp(viewModel: AppViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var selectedLotCodeForTrace by remember { mutableStateOf<String?>(null) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val farmers by viewModel.allFarmers.collectAsStateWithLifecycle()
    val lots by viewModel.allLots.collectAsStateWithLifecycle()
    val popups by viewModel.allPopups.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()

    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotifCount.collectAsStateWithLifecycle()
    val activePushNotification by viewModel.activePushNotification.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val selectedAiModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()
    val selectedAiRole by viewModel.selectedAiRole.collectAsStateWithLifecycle()
    val isGoogleMapsEnabled by viewModel.isGoogleMapsEnabled.collectAsStateWithLifecycle()
    val isMediaGenerating by viewModel.isMediaGenerating.collectAsStateWithLifecycle()
    val generatedMedia by viewModel.generatedMedia.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveVoiceTranscript.collectAsStateWithLifecycle()
    val firebaseUserState by viewModel.firebaseUserState.collectAsStateWithLifecycle()
    val firestoreProducts by viewModel.firestoreProducts.collectAsStateWithLifecycle()
    val isFirestoreLoading by viewModel.isFirestoreLoading.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()
    val allPaymentAttempts by viewModel.allPaymentAttempts.collectAsStateWithLifecycle()
    
    // Security states collection
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val isMfaVerified by viewModel.isMfaVerified.collectAsStateWithLifecycle()
    val activeSessions by viewModel.activeSessionsForCurrentUser.collectAsStateWithLifecycle()
    val reauthRequiredAction by viewModel.reauthRequiredAction.collectAsStateWithLifecycle()

    val recurringSchedules by remember(currentUser) {
        if (currentUser != null) {
            viewModel.repository.getRecurringSchedules(currentUser!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(emptyList())

    val userOrders by remember(currentUser, allOrders) {
        derivedStateOf {
            if (currentUser != null) {
                allOrders.filter { it.buyerId == currentUser!!.id }
            } else {
                emptyList()
            }
        }
    }

    var selectedOrderIdForTracking by remember { mutableStateOf<Long?>(null) }

    val cartItemCount = remember(cart) {
        cart.values.sum().toInt()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(
                message = it.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    // Role switcher dialog
    if (showRoleDialog) {
        RoleSwitcherDialog(
            currentUser = currentUser,
            users = allUsers,
            onSelectUser = { viewModel.switchUser(it) },
            onDismiss = { showRoleDialog = false },
            onLoginPassword = { username, password -> viewModel.loginWithPassword(username, password) },
            onLoginGoogle = { email, name, sub, confirmPass -> viewModel.loginWithGoogle(email, name, sub, confirmPass) },
            uiMessage = uiMessage?.message
        )
    }

    // In-app Notifications Dialog (Notification Center)
    if (showNotificationsDialog) {
        NotificationCenterDialog(
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false },
            onMarkAsRead = { viewModel.markNotificationAsRead(it) },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
            onDeleteNotification = { viewModel.deleteNotification(it) },
            onSelectNotification = { notif ->
                when (notif.type) {
                    NotificationType.SESSION_OPENED -> currentScreen = AppScreen.HOME
                    NotificationType.ORDER_CONFIRMED,
                    NotificationType.ORDER_PREPARING,
                    NotificationType.ORDER_ARRIVED_POPUP,
                    NotificationType.SHORTAGE_ALERT,
                    NotificationType.ORDER_DELIVERED -> {
                        if (currentUser?.role == UserRole.BUYER) currentScreen = AppScreen.ORDERS
                        else currentScreen = AppScreen.OPERATIONS
                    }
                    NotificationType.ADMIN_BROADCAST -> currentScreen = AppScreen.HOME
                }
                showNotificationsDialog = false
            },
            onTestPush = { type -> viewModel.triggerTestNotification(type) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentScreen == AppScreen.HOME || currentScreen == AppScreen.PRODUCTS) {
                ExtendedFloatingActionButton(
                    onClick = { currentScreen = AppScreen.AI_CHAT },
                    containerColor = FarmGreenDark,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag("fab_ai_assistant")
                ) {
                    Text("🤖", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trợ lý AI", fontWeight = FontWeight.Bold)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌾", fontSize = 22.sp, modifier = Modifier.padding(end = 6.dp))
                        Column {
                            Text(
                                text = "Nông Sản Bình Mỹ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = "Sàn Đặt Trước Thu Hoạch",
                                style = MaterialTheme.typography.labelSmall,
                                color = FarmTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Cart button
                    IconButton(
                        onClick = { currentScreen = AppScreen.CART },
                        modifier = Modifier.testTag("btn_top_cart")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge(containerColor = FarmGreenDark) {
                                        Text("$cartItemCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Giỏ hàng",
                                tint = FarmGreenDark
                            )
                        }
                    }

                    // Firestore Products button
                    IconButton(
                        onClick = { currentScreen = AppScreen.FIRESTORE_PRODUCTS },
                        modifier = Modifier.testTag("btn_top_firestore_products")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEBF8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "☁️", fontSize = 16.sp)
                        }
                    }

                    // AI Assistant chatbot button
                    IconButton(
                        onClick = { currentScreen = AppScreen.AI_CHAT },
                        modifier = Modifier.testTag("btn_top_ai_assistant")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FarmGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🤖", fontSize = 16.sp)
                        }
                    }

                    // Notification bell
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("btn_top_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = Color.Red) {
                                        Text("$unreadNotifCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = FarmGreenDark
                            )
                        }
                    }

                    // Role switch avatar
                    IconButton(
                        onClick = { showRoleDialog = true },
                        modifier = Modifier.testTag("btn_top_role_switch")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FarmGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentUser?.avatarEmoji ?: "👤", fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FarmBackground,
                    titleContentColor = FarmGreenDark
                )
            )
        },
        bottomBar = {
            val role = currentUser?.role ?: UserRole.BUYER
            val opsTitle = when (role) {
                UserRole.BUYER -> "Lịch định kỳ"
                UserRole.FARMER -> "Nông hộ"
                UserRole.ADMIN -> "Quản trị"
                UserRole.POPUP_STAFF -> "Pop-up"
                UserRole.VERIFIER -> "Kiểm định"
                UserRole.SUPER_ADMIN -> "Quản trị"
            }
            val opsIcon = when (role) {
                UserRole.BUYER -> Icons.Default.EventRepeat
                UserRole.FARMER -> Icons.Default.Agriculture
                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                UserRole.POPUP_STAFF -> Icons.Default.Storefront
                UserRole.VERIFIER -> Icons.Default.Verified
                UserRole.SUPER_ADMIN -> Icons.Default.AdminPanelSettings
            }

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { currentScreen = AppScreen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Khám phá") },
                    label = { Text("Khám phá", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FarmGreenDark,
                        indicatorColor = FarmGreenPrimary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.PRODUCTS,
                    onClick = { currentScreen = AppScreen.PRODUCTS },
                    icon = { Icon(Icons.Default.Eco, contentDescription = "Nông sản") },
                    label = { Text("Nông sản", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FarmGreenDark,
                        indicatorColor = FarmGreenPrimary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.ORDERS,
                    onClick = { currentScreen = AppScreen.ORDERS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (userOrders.isNotEmpty()) {
                                    Badge(containerColor = FarmGreenPrimary) {
                                        Text("${userOrders.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Đơn hàng")
                        }
                    },
                    label = { Text("Đơn hàng", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FarmGreenDark,
                        indicatorColor = FarmGreenPrimary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.TRACEABILITY,
                    onClick = { currentScreen = AppScreen.TRACEABILITY },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Truy xuất QR") },
                    label = { Text("Truy xuất", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FarmGreenDark,
                        indicatorColor = FarmGreenPrimary
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.OPERATIONS,
                    onClick = { currentScreen = AppScreen.OPERATIONS },
                    icon = { Icon(opsIcon, contentDescription = opsTitle) },
                    label = { Text(opsTitle, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FarmGreenDark,
                        indicatorColor = FarmGreenPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        activeSession = activeSession,
                        products = products,
                        farmers = farmers,
                        popups = popups,
                        orders = allOrders,
                        orderItems = allOrderItems,
                        onNavigateToProducts = { currentScreen = AppScreen.PRODUCTS },
                        onNavigateToSessionDetail = { currentScreen = AppScreen.SESSION_DETAIL },
                        onNavigateToOrders = { currentScreen = AppScreen.ORDERS },
                        onNavigateToTraceability = { currentScreen = AppScreen.TRACEABILITY },
                        onSelectProduct = { prod ->
                            viewModel.selectProduct(prod)
                            currentScreen = AppScreen.PRODUCT_DETAIL
                        },
                        onAddToCart = { prodId ->
                            viewModel.addToCart(prodId, activeSession?.id ?: 1L, 1.0)
                        },
                        onOpenAiChat = { currentScreen = AppScreen.AI_CHAT },
                        onDirectBooking = { productId, qty, name, phone ->
                            viewModel.submitDirectBooking(productId, qty, name, phone) {
                                currentScreen = AppScreen.ORDERS
                            }
                        }
                    )
                }

                AppScreen.PRODUCTS -> {
                    ProductCatalogScreen(
                        products = products,
                        farmers = farmers,
                        activeSession = activeSession,
                        onSelectProduct = { prod ->
                            viewModel.selectProduct(prod)
                            currentScreen = AppScreen.PRODUCT_DETAIL
                        },
                        onAddToCart = { prodId ->
                            viewModel.addToCart(prodId, activeSession?.id ?: 1L, 1.0)
                        },
                        onDirectBooking = { productId, qty, name, phone ->
                            viewModel.submitDirectBooking(productId, qty, name, phone) {
                                currentScreen = AppScreen.ORDERS
                            }
                        }
                    )
                }

                AppScreen.ORDERS -> {
                    OrderHistoryScreen(
                        orders = userOrders,
                        orderItems = allOrderItems,
                        sessions = allSessions,
                        popups = popups,
                        reviews = allReviews,
                        paymentAttempts = allPaymentAttempts,
                        onCancelOrder = { oId -> viewModel.cancelOrder(oId) },
                        onNavigateToProducts = { currentScreen = AppScreen.PRODUCTS },
                        onSubmitReview = { pId, oId, rating, comment ->
                            viewModel.addProductReview(pId, oId, rating, comment)
                        },
                        onPayOnline = { oId, provider, isDeposit ->
                            viewModel.initiateOnlinePayment(oId, provider, isDeposit)
                            currentScreen = AppScreen.PAYMENT_PORTAL
                        },
                        onViewPayment = { pCode ->
                            viewModel.setActivePaymentAttempt(pCode)
                            currentScreen = AppScreen.PAYMENT_PORTAL
                        },
                        onTrackOrder = { oId ->
                            selectedOrderIdForTracking = oId
                            currentScreen = AppScreen.ORDER_TRACKING
                        }
                    )
                }

                AppScreen.TRACEABILITY -> {
                    TraceabilityScreen(
                        lots = lots,
                        products = products,
                        farmers = farmers,
                        initialLotCode = selectedLotCodeForTrace
                    )
                }

                AppScreen.OPERATIONS -> {
                    when (currentUser?.role) {
                        UserRole.BUYER -> {
                            RecurringOrderScreen(
                                schedules = recurringSchedules,
                                products = products,
                                popups = popups,
                                currentUser = currentUser,
                                onToggleStatus = { id, st -> viewModel.toggleRecurring(id, st) },
                                onCreateSchedule = { sc -> viewModel.addRecurringSchedule(sc) }
                            )
                        }

                        UserRole.FARMER -> {
                            val farmerProfile = farmers.find { it.id == currentUser?.farmerProfileId }
                            FarmerDashboardScreen(
                                farmerUser = currentUser,
                                farmerProfile = farmerProfile,
                                products = products,
                                lots = lots,
                                allOrders = allOrders,
                                allOrderItems = allOrderItems,
                                activeSession = activeSession,
                                onReportShortage = { lotCode, kg, reason ->
                                    viewModel.reportShortage(lotCode, kg, reason)
                                }
                            )
                        }

                        UserRole.ADMIN, UserRole.SUPER_ADMIN -> {
                            AdminDashboardScreen(
                                sessions = allSessions,
                                activeSession = activeSession,
                                farmers = farmers,
                                products = products,
                                lots = lots,
                                orders = allOrders,
                                orderItems = allOrderItems,
                                popups = popups,
                                users = allUsers,
                                auditLogs = auditLogs,
                                currentUser = currentUser,
                                activeSessions = activeSessions,
                                isMfaVerified = isMfaVerified,
                                onLockSession = { sId -> viewModel.lockSession(sId) },
                                onAdvanceSession = { sId, st -> viewModel.advanceSession(sId, st) },
                                onApproveFarmer = { fId, ok -> viewModel.approveFarmer(fId, ok) },
                                onResolveShortage = { oId, act, ref -> viewModel.resolveShortage(oId, act, ref) },
                                onUpdatePrice = { pId, pr -> viewModel.updatePrice(pId, pr) },
                                onSendBroadcast = { title, msg, targetRole -> viewModel.sendAdminBroadcast(title, msg, targetRole) },
                                onLockUnlockUser = { targetId, lock, reason -> viewModel.superAdminLockUnlockUser(targetId, lock, reason) },
                                onChangeUserRole = { targetId, role -> viewModel.superAdminChangeUserRole(targetId, role) },
                                onRevokeSession = { sId -> viewModel.revokeSession(sId) },
                                onRevokeAllSessions = { viewModel.revokeAllSessions() },
                                onConfirmMfa = { code -> viewModel.verifyAdminMfa(code) }
                            )
                        }

                        UserRole.POPUP_STAFF -> {
                            val assignedPopup = popups.find { it.id == currentUser?.assignedPopupLocationId }
                            PopupStaffScreen(
                                staffUser = currentUser,
                                assignedPopup = assignedPopup,
                                orders = allOrders,
                                orderItems = allOrderItems,
                                onCheckInCode = { code -> viewModel.checkInPickup(code) }
                            )
                        }

                        UserRole.VERIFIER -> {
                            VerifierScreen(
                                verifierUser = currentUser,
                                lots = lots,
                                products = products,
                                farmers = farmers,
                                onVerifyLot = { lotCode, verifier, notes ->
                                    viewModel.verifyLot(lotCode, verifier, notes)
                                }
                            )
                        }

                        null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Vui lòng chọn tài khoản đăng nhập")
                            }
                        }
                    }
                }

                AppScreen.CART -> {
                    BackHandler { currentScreen = AppScreen.PRODUCTS }
                    CartCheckoutScreen(
                        cart = cart,
                        products = products,
                        farmers = farmers,
                        popups = popups,
                        activeSession = activeSession,
                        sessions = allSessions,
                        currentUser = currentUser,
                        onUpdateQty = { pId, sId, q -> viewModel.updateCartQty(pId, sId, q) },
                        onRemoveItem = { pId, sId -> viewModel.removeFromCart(pId, sId) },
                        onClearCart = { viewModel.clearCart() },
                        onSubmitOrder = { sId, dType, pId, hAddr, rName, rPhone, note, onSuccess ->
                            viewModel.submitPreOrder(sId, dType, pId, hAddr, rName, rPhone, note, onSuccess)
                        },
                        onNavigateToProducts = { currentScreen = AppScreen.PRODUCTS },
                        onOrderSuccess = { order ->
                            currentScreen = AppScreen.ORDERS
                        },
                        onInitOnlinePayment = { oId, provider, isDeposit ->
                            viewModel.initiateOnlinePayment(oId, provider, isDeposit)
                            currentScreen = AppScreen.PAYMENT_PORTAL
                        }
                    )
                }

                AppScreen.PRODUCT_DETAIL -> {
                    BackHandler { currentScreen = AppScreen.PRODUCTS }
                    val prod = selectedProduct
                    if (prod != null) {
                        val farmer = farmers.find { it.id == prod.farmerId }
                        val lot = lots.find { it.lotCode == prod.lotCode }
                        ProductDetailScreen(
                            product = prod,
                            farmer = farmer,
                            lot = lot,
                            sessions = allSessions.filter { it.status == com.example.data.model.SessionStatus.OPEN },
                            reviews = allReviews,
                            onBack = { currentScreen = AppScreen.PRODUCTS },
                            onAddToCart = { qty, sId ->
                                viewModel.addToCart(prod.id, sId, qty)
                                currentScreen = AppScreen.CART
                            },
                            onNavigateToTraceability = { lotCode ->
                                selectedLotCodeForTrace = lotCode
                                currentScreen = AppScreen.TRACEABILITY
                            }
                        )
                    } else {
                        currentScreen = AppScreen.PRODUCTS
                    }
                }

                AppScreen.SESSION_DETAIL -> {
                    BackHandler { currentScreen = AppScreen.HOME }
                    SessionDetailScreen(
                        session = activeSession,
                        popups = popups,
                        currentUserRole = currentUser?.role ?: UserRole.BUYER,
                        onBack = { currentScreen = AppScreen.HOME },
                        onLockSession = { sId -> viewModel.lockSession(sId) },
                        onAdvanceSession = { sId, st -> viewModel.advanceSession(sId, st) }
                    )
                }

                AppScreen.AI_CHAT -> {
                    BackHandler { currentScreen = AppScreen.HOME }
                    AiAssistantScreen(
                        messages = chatMessages,
                        isLoading = isAiLoading,
                        onSendMessage = { prompt -> viewModel.sendAiMessage(prompt) },
                        onBack = { currentScreen = AppScreen.HOME },
                        onClearChat = { viewModel.clearAiChat() },
                        selectedModel = selectedAiModel,
                        onSelectModel = { viewModel.setAiModel(it) },
                        selectedRole = selectedAiRole,
                        onSelectRole = { viewModel.setAiRole(it) },
                        isGoogleMapsEnabled = isGoogleMapsEnabled,
                        onToggleGoogleMaps = { viewModel.toggleGoogleMaps(it) },
                        onTranscribeSpeech = { onDone -> viewModel.transcribeSpeech(onDone) },
                        liveTranscript = liveTranscript,
                        onLiveVoice = { viewModel.startLiveConversation(it) }
                    )
                }

                AppScreen.FIRESTORE_PRODUCTS -> {
                    FirestoreProductListScreen(
                        products = firestoreProducts,
                        isLoading = isFirestoreLoading,
                        onRefresh = { viewModel.refreshFirestoreProducts() },
                        onSyncFromFarms = { viewModel.syncBinhMyProductsToFirestore() },
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                }

                AppScreen.ORDER_TRACKING -> {
                    OrderTrackingScreen(
                        orders = userOrders,
                        orderItems = allOrderItems,
                        sessions = allSessions,
                        popups = popups,
                        farmers = farmers,
                        lots = lots,
                        initialOrderId = selectedOrderIdForTracking,
                        onBack = { currentScreen = AppScreen.ORDERS },
                        onNavigateToAiChat = { initialPrompt ->
                            viewModel.sendAiMessage(initialPrompt)
                            currentScreen = AppScreen.AI_CHAT
                        },
                        onNavigateToTrace = { lotCode ->
                            selectedLotCodeForTrace = lotCode
                            currentScreen = AppScreen.TRACEABILITY
                        }
                    )
                }

                AppScreen.PAYMENT_PORTAL -> {
                    PaymentPortalScreen(
                        paymentAttempts = viewModel.allPaymentAttempts,
                        orders = viewModel.allOrders,
                        orderItems = viewModel.allOrderItems,
                        refunds = viewModel.allRefunds,
                        onBack = { currentScreen = AppScreen.ORDERS },
                        onSubmitReview = { pId, oId, rating, comment -> viewModel.addProductReview(pId, oId, rating, comment) },
                        viewModel = viewModel
                    )
                }
            }

            // Heads-Up Push Notification Banner (Visible across every screen)
            HeadsUpPushNotificationBanner(
                notification = activePushNotification,
                onDismiss = { viewModel.dismissPushNotification() },
                onOpenDetails = { notif ->
                    when (notif.type) {
                        NotificationType.SESSION_OPENED -> currentScreen = AppScreen.HOME
                        NotificationType.ORDER_CONFIRMED,
                        NotificationType.ORDER_PREPARING,
                        NotificationType.ORDER_ARRIVED_POPUP,
                        NotificationType.SHORTAGE_ALERT,
                        NotificationType.ORDER_DELIVERED -> {
                            if (currentUser?.role == UserRole.BUYER) currentScreen = AppScreen.ORDERS
                            else currentScreen = AppScreen.OPERATIONS
                        }
                        NotificationType.ADMIN_BROADCAST -> currentScreen = AppScreen.HOME
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
            )
        }
    }
}
