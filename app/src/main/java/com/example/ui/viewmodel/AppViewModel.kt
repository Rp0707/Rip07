package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiAssistantService
import com.example.data.api.ChatMessage
import com.example.data.api.GeneratedMediaResult
import com.example.data.api.MessageSender
import com.example.data.firebase.BinhMyProductFirestore
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.FirebaseUserState
import com.example.data.local.AppDatabase
import com.example.data.local.*
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.util.BinhMyPayProvider
import com.example.ui.components.formatVnd
import com.example.data.repository.AppRepository
import com.example.data.repository.CartItemInput
import com.example.security.RbacSeedData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItemUiModel(
    val product: ProductEntity,
    val farmer: FarmerProfileEntity?,
    val quantity: Double,
    val lineTotal: Double
)

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isError: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = AppRepository(db)

    // Current logged-in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Current Session ID (Session ID rotation)
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    // MFA Verification status for Admins / SuperAdmins
    private val _isMfaVerified = MutableStateFlow(false)
    val isMfaVerified: StateFlow<Boolean> = _isMfaVerified.asStateFlow()

    // Re-authentication callback helper
    private val _reauthRequiredAction = MutableStateFlow<(() -> Unit)?>(null)
    val reauthRequiredAction: StateFlow<(() -> Unit)?> = _reauthRequiredAction.asStateFlow()

    val allUsers: StateFlow<List<UserEntity>> = repository.userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Active sessions for the logged-in user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeSessionsForCurrentUser: StateFlow<List<UserSessionEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getActiveSessionsForUser(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<PreOrderSessionEntity?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allSessions: StateFlow<List<PreOrderSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allFarmers: StateFlow<List<FarmerProfileEntity>> = repository.allFarmers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLots: StateFlow<List<ProductLotEntity>> = repository.allLots
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPopups: StateFlow<List<PopUpLocationEntity>> = repository.allPopups
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOrderItems: StateFlow<List<OrderItemEntity>> = repository.itemDao.getAllOrderItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allReviews: StateFlow<List<ProductReviewEntity>> = repository.getAllReviews()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPaymentAttempts: StateFlow<List<PaymentAttemptEntity>> = repository.allPaymentAttempts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allRefunds: StateFlow<List<RefundEntity>> = repository.allRefunds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activePaymentAttemptCode = MutableStateFlow<String?>(null)
    val activePaymentAttemptCode: StateFlow<String?> = _activePaymentAttemptCode.asStateFlow()

    private val payProvider = BinhMyPayProvider()

    // Cart state: "productId_sessionId" -> quantity
    private val _cart = MutableStateFlow<Map<String, Double>>(emptyMap())
    val cart: StateFlow<Map<String, Double>> = _cart.asStateFlow()

    // Temporary UI message
    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // Loading indicator state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Selected product for detail dialog/screen
    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    // Active heads-up push notification banner (visible on any screen)
    private val _activePushNotification = MutableStateFlow<AppNotificationEntity?>(null)
    val activePushNotification: StateFlow<AppNotificationEntity?> = _activePushNotification.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userNotifications: StateFlow<List<AppNotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotifications(user.id, user.role)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val unreadNotifCount: StateFlow<Int> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getUnreadNotificationCount(user.id, user.role)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Search query & category filter
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Tất cả")

    // AI Assistant & Media Generation
    private val aiService = AiAssistantService()
    private val firebaseService = FirebaseService(application)

    private val _firebaseUserState = MutableStateFlow(firebaseService.getCurrentUserState())
    val firebaseUserState: StateFlow<FirebaseUserState> = _firebaseUserState.asStateFlow()

    private val _selectedAiModel = MutableStateFlow("gemini-3.5-flash")
    val selectedAiModel: StateFlow<String> = _selectedAiModel.asStateFlow()

    private val _selectedAiRole = MutableStateFlow("FARMER")
    val selectedAiRole: StateFlow<String> = _selectedAiRole.asStateFlow()

    private val _isGoogleMapsEnabled = MutableStateFlow(false)
    val isGoogleMapsEnabled: StateFlow<Boolean> = _isGoogleMapsEnabled.asStateFlow()

    private val _generatedMedia = MutableStateFlow<GeneratedMediaResult?>(null)
    val generatedMedia: StateFlow<GeneratedMediaResult?> = _generatedMedia.asStateFlow()

    private val _isMediaGenerating = MutableStateFlow(false)
    val isMediaGenerating: StateFlow<Boolean> = _isMediaGenerating.asStateFlow()

    private val _liveVoiceTranscript = MutableStateFlow<String?>(null)
    val liveVoiceTranscript: StateFlow<String?> = _liveVoiceTranscript.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Dạ chào bạn! Tôi là Trợ Lý Nông Sản Bình Mỹ 🌿.\n\nTôi sẵn sàng tư vấn cho bạn về rau muống nước sạch VietGAP Bình Mỹ, hạn chốt phiên thu hoạch, điểm nhận hàng Pop-up hoặc tra cứu mã lô kiểm định an toàn.\n\nBạn cần tìm hiểu thông tin gì hôm nay?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun setAiModel(model: String) {
        _selectedAiModel.value = model
    }

    fun setAiRole(role: String) {
        _selectedAiRole.value = role
    }

    fun toggleGoogleMaps(enabled: Boolean) {
        _isGoogleMapsEnabled.value = enabled
    }

    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        val isMaps = _isGoogleMapsEnabled.value
        val userMsg = ChatMessage(sender = MessageSender.USER, text = userText.trim(), isMapsGrounded = isMaps)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val reply = aiService.sendMessage(
                    history = _chatMessages.value,
                    userMessage = userText,
                    modelName = _selectedAiModel.value,
                    roleType = _selectedAiRole.value,
                    enableGoogleMaps = isMaps
                )
                val assistantMsg = ChatMessage(sender = MessageSender.ASSISTANT, text = reply, isMapsGrounded = isMaps)
                _chatMessages.value = _chatMessages.value + assistantMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Dạ hệ thống AI đang bận kết nối, bạn thử lại sau ít giây hoặc chọn một câu hỏi gợi ý bên trên nhé!"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Dạ chào bạn! Tôi là Trợ Lý Nông Sản Bình Mỹ 🌿.\n\nTôi sẵn sàng tư vấn cho bạn về rau muống nước sạch VietGAP Bình Mỹ, hạn chốt phiên thu hoạch, điểm nhận hàng Pop-up hoặc tra cứu mã lô kiểm định an toàn.\n\nBạn cần tìm hiểu thông tin gì hôm nay?"
            )
        )
    }

    fun generateImage(prompt: String, aspectRatio: String = "1:1") {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isMediaGenerating.value = true
            val res = aiService.generateOrEditImage(prompt, aspectRatio)
            _generatedMedia.value = res
            _isMediaGenerating.value = false
            showMessage("Đã hoàn tất tạo ảnh: \"${prompt.take(30)}...\"")
        }
    }

    fun generateMusic(prompt: String, isShortClip: Boolean = true) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isMediaGenerating.value = true
            val res = aiService.generateMusic(prompt, isShortClip)
            _generatedMedia.value = res
            _isMediaGenerating.value = false
            showMessage("Đã sáng tác thành công giai điệu: \"${prompt.take(30)}...\"")
        }
    }

    fun generateVideo(prompt: String, aspectRatio: String = "16:9") {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isMediaGenerating.value = true
            val res = aiService.generateVideo(prompt, aspectRatio)
            _generatedMedia.value = res
            _isMediaGenerating.value = false
            showMessage("Đã sinh video Veo 3: \"${prompt.take(30)}...\"")
        }
    }

    fun transcribeSpeech(onTranscribed: (String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val text = aiService.transcribeAudio()
            _isAiLoading.value = false
            onTranscribed(text)
            showMessage("Đã chuyển giọng nói thành văn bản thành công!")
        }
    }

    fun startLiveConversation(userSpeech: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val reply = aiService.liveVoiceConversation(userSpeech)
            _liveVoiceTranscript.value = reply
            _isAiLoading.value = false
        }
    }

    private val _firestoreProducts = MutableStateFlow<List<BinhMyProductFirestore>>(
        firebaseService.getDefaultBinhMyProducts()
    )
    val firestoreProducts: StateFlow<List<BinhMyProductFirestore>> = _firestoreProducts.asStateFlow()

    private val _isFirestoreLoading = MutableStateFlow(false)
    val isFirestoreLoading: StateFlow<Boolean> = _isFirestoreLoading.asStateFlow()

    fun refreshFirestoreProducts() {
        viewModelScope.launch {
            _isFirestoreLoading.value = true
            val res = firebaseService.fetchProductsFromFirestoreOnce()
            res.onSuccess { list ->
                _firestoreProducts.value = list
                showMessage("Đã tải ${list.size} loại nông sản từ Cloud Firestore!")
            }.onFailure {
                showMessage("Không thể tải từ Firestore, đang dùng dữ liệu lưu tạm.")
            }
            _isFirestoreLoading.value = false
        }
    }

    // Firebase Auth & Firestore Sync
    fun signInWithFirebase() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = firebaseService.signInAnonymously()
            _isLoading.value = false
            res.onSuccess { state ->
                _firebaseUserState.value = state
                showMessage("Đã kết nối bảo mật thành công với Firebase Auth (${state.displayName})!")
            }
        }
    }

    fun syncAllToFirestore() {
        val user = _currentUser.value ?: return
        val orders = allOrders.value
        viewModelScope.launch {
            _isLoading.value = true
            val userRes = firebaseService.syncUserToFirestore(user)
            val ordersRes = firebaseService.syncOrdersToFirestore(orders)
            _isLoading.value = false
            showMessage("Đám mây Firestore: ${userRes.getOrNull()} • ${ordersRes.getOrNull()}")
        }
    }

    fun syncBinhMyProductsToFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            val localProds = allProducts.value
            val localFarmers = allFarmers.value
            val session = activeSession.value

            val firestoreList = if (localProds.isNotEmpty()) {
                firebaseService.mapLocalToFirestoreProducts(localProds, localFarmers, session)
            } else {
                firebaseService.getDefaultBinhMyProducts()
            }

            val res = firebaseService.syncBinhMyProductsToFirestore(firestoreList)
            _firestoreProducts.value = firestoreList
            _isLoading.value = false
            showMessage(res.getOrNull() ?: "Đã thiết lập cấu trúc và đồng bộ nông sản Bình Mỹ lên Firestore!")
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.initializeDemoDataIfEmpty()
                RbacSeedData.seedRbacSystem(db)
                // Set initial user to Buyer demo
                val defaultBuyer = repository.userDao.getUserByUsername("khachhang_demo")
                    ?: repository.userDao.getUserByUsername("buyer_demo")
                _currentUser.value = defaultBuyer
            } catch (e: Throwable) {
                android.util.Log.e("AppViewModel", "Failed to initialize demo data: ${e.message}", e)
            }
        }

        // Listen for new push notifications to display top dropdown banner
        viewModelScope.launch {
            repository.latestPushNotification.collect { notif ->
                val user = _currentUser.value
                val isRelevant = user != null && (
                    notif.userId == user.id ||
                    notif.targetRole == user.role ||
                    (notif.userId == 0L && notif.targetRole == null)
                )
                if (isRelevant) {
                    _activePushNotification.value = notif
                }
            }
        }
    }

    fun switchUser(user: UserEntity) {
        viewModelScope.launch {
            if (user.isLocked) {
                showMessage("Tài khoản này đã bị khóa! Lý do: ${user.lockReason ?: "Không rõ"}", isError = true)
                repository.logAudit(
                    actorId = 0L,
                    actorName = "Hệ thống",
                    actorRole = "SYSTEM",
                    action = "SWITCH_USER_ATTEMPT",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Cố gắng chuyển sang tài khoản bị khóa: ${user.username}",
                    result = "FAILURE",
                    isAnomaly = true
                )
                return@launch
            }

            // Session ID rotation
            val newSessionId = UUID.randomUUID().toString()
            _currentSessionId.value = newSessionId
            _currentUser.value = user

            // Reset MFA check for admins
            _isMfaVerified.value = false

            // Create session in database
            val session = UserSessionEntity(
                userId = user.id,
                sessionId = newSessionId,
                deviceName = "Thiết bị mô phỏng (Android Edge)",
                ipAddress = "192.168.1." + (10..99).random(),
                loginTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis()
            )
            repository.sessionDaoSecurity.insertSession(session)

            // Log security audit
            repository.logAudit(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "LOGIN_SWITCH",
                targetType = "USER",
                targetId = user.id.toString(),
                details = "Chuyển tài khoản thành công. Session ID mới đã được xoay vòng.",
                result = "SUCCESS"
            )

            _uiMessage.value = UiMessage(message = "Đã chuyển sang vai trò: ${user.role.labelVi} (${user.fullName})")
        }
    }

    // --- SECURE AUTHENTICATION FLOWS (TẦNG 2 & ĐĂNG NHẬP GOOGLE) ---

    fun loginWithPassword(username: String, passwordRaw: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.userDao.getUserByUsername(username.trim())
            if (user == null) {
                _isLoading.value = false
                showMessage("Tài khoản hoặc mật khẩu không chính xác!", isError = true)
                repository.logAudit(
                    actorId = 0L,
                    actorName = "Khách vãng lai",
                    actorRole = "GUEST",
                    action = "PASSWORD_LOGIN",
                    targetType = "USER",
                    targetId = username,
                    details = "Đăng nhập thất bại: Sai username",
                    result = "FAILURE"
                )
                return@launch
            }

            if (user.isLocked) {
                _isLoading.value = false
                showMessage("Tài khoản đã bị khóa: ${user.lockReason}", isError = true)
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "PASSWORD_LOGIN",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Đăng nhập thất bại: Tài khoản bị khóa",
                    result = "FAILURE",
                    isAnomaly = true
                )
                return@launch
            }

            val inputHash = com.example.data.util.HashUtil.sha256(passwordRaw)
            if (user.passwordHash == inputHash) {
                // Successful Login
                val newSessionId = UUID.randomUUID().toString()
                _currentSessionId.value = newSessionId
                _currentUser.value = user
                _isMfaVerified.value = false // Require MFA for admin if configured

                // Reset failed attempts
                val updatedUser = user.copy(failedLoginAttempts = 0, lastActiveTime = System.currentTimeMillis())
                repository.userDao.insertUser(updatedUser)

                // Save session in DB
                val session = UserSessionEntity(
                    userId = user.id,
                    sessionId = newSessionId,
                    deviceName = "Thiết bị mô phỏng (Đăng nhập mật khẩu)",
                    ipAddress = "192.168.1." + (10..99).random(),
                    loginTime = System.currentTimeMillis(),
                    lastActiveTime = System.currentTimeMillis()
                )
                repository.sessionDaoSecurity.insertSession(session)

                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "PASSWORD_LOGIN",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Đăng nhập mật khẩu thành công. Session xoay vòng: $newSessionId",
                    result = "SUCCESS"
                )

                _isLoading.value = false
                showMessage("Đăng nhập thành công! Chào mừng ${user.fullName}")
            } else {
                // Failed Attempt
                val newFailedAttempts = user.failedLoginAttempts + 1
                val isNowLocked = newFailedAttempts >= 5
                val lockReasonStr = if (isNowLocked) "Tự động khóa do nhập sai mật khẩu quá 5 lần" else null

                val updatedUser = user.copy(
                    failedLoginAttempts = newFailedAttempts,
                    isLocked = if (isNowLocked) true else user.isLocked,
                    lockReason = if (isNowLocked) lockReasonStr else user.lockReason
                )
                repository.userDao.insertUser(updatedUser)

                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "PASSWORD_LOGIN",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Đăng nhập thất bại: Sai mật khẩu (${newFailedAttempts}/5 lần)${if (isNowLocked) " -> KHÓA TÀI KHOẢN" else ""}",
                    result = "FAILURE",
                    isAnomaly = isNowLocked
                )

                _isLoading.value = false
                if (isNowLocked) {
                    showMessage("Tài khoản của bạn đã bị khóa tự động do nhập sai quá 5 lần!", isError = true)
                } else {
                    showMessage("Tài khoản hoặc mật khẩu không chính xác! (${newFailedAttempts}/5 lần)", isError = true)
                }
            }
        }
    }

    fun loginWithGoogle(email: String, fullName: String, googleSub: String, confirmPasswordForLink: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Check if sub already linked
            val usersList = allUsers.value
            val userBySub = usersList.find { it.googleSub == googleSub }
            if (userBySub != null) {
                // Already linked, log in directly
                if (userBySub.isLocked) {
                    _isLoading.value = false
                    showMessage("Tài khoản liên kết Google đã bị khóa: ${userBySub.lockReason}", isError = true)
                    return@launch
                }

                val newSessionId = UUID.randomUUID().toString()
                _currentSessionId.value = newSessionId
                _currentUser.value = userBySub
                _isMfaVerified.value = false

                val session = UserSessionEntity(
                    userId = userBySub.id,
                    sessionId = newSessionId,
                    deviceName = "Thiết bị Google Sign-In",
                    ipAddress = "192.168.1." + (10..99).random(),
                    loginTime = System.currentTimeMillis(),
                    lastActiveTime = System.currentTimeMillis()
                )
                repository.sessionDaoSecurity.insertSession(session)

                repository.logAudit(
                    actorId = userBySub.id,
                    actorName = userBySub.fullName,
                    actorRole = userBySub.role.name,
                    action = "GOOGLE_LOGIN",
                    targetType = "USER",
                    targetId = userBySub.id.toString(),
                    details = "Đăng nhập bằng Google thành công. Session ID: $newSessionId",
                    result = "SUCCESS"
                )

                _isLoading.value = false
                showMessage("Đăng nhập bằng Google thành công! Chào mừng ${userBySub.fullName}")
                return@launch
            }

            // 2. Check if email matches old account but not linked
            val userByEmail = usersList.find { it.email.lowercase() == email.trim().lowercase() }
            if (userByEmail != null) {
                // Email matches, REQUIRE old account password to link securely (Tầng 2: No automatic linking)
                if (confirmPasswordForLink == null) {
                    _isLoading.value = false
                    // Set a special UI message triggering the password input field on frontend
                    _uiMessage.value = UiMessage(
                        message = "EMAIL_MATCH_LINK_REQUIRED: Phát hiện tài khoản cùng email. Vui lòng nhập mật khẩu cũ để liên kết Google.",
                        isError = false
                    )
                    return@launch
                }

                val confirmHash = com.example.data.util.HashUtil.sha256(confirmPasswordForLink)
                if (userByEmail.passwordHash == confirmHash) {
                    // Valid password! Securely link
                    val updatedUser = userByEmail.copy(googleSub = googleSub)
                    repository.userDao.insertUser(updatedUser)

                    val newSessionId = UUID.randomUUID().toString()
                    _currentSessionId.value = newSessionId
                    _currentUser.value = updatedUser
                    _isMfaVerified.value = false

                    val session = UserSessionEntity(
                        userId = updatedUser.id,
                        sessionId = newSessionId,
                        deviceName = "Thiết bị Google Sign-In (Đã liên kết)",
                        ipAddress = "192.168.1." + (10..99).random(),
                        loginTime = System.currentTimeMillis(),
                        lastActiveTime = System.currentTimeMillis()
                    )
                    repository.sessionDaoSecurity.insertSession(session)

                    repository.logAudit(
                        actorId = updatedUser.id,
                        actorName = updatedUser.fullName,
                        actorRole = updatedUser.role.name,
                        action = "GOOGLE_LINK_SUCCESS",
                        targetType = "USER",
                        targetId = updatedUser.id.toString(),
                        details = "Xác thực mật khẩu thành công. Đã liên kết tài khoản Google với tài khoản cũ.",
                        result = "SUCCESS"
                    )

                    _isLoading.value = false
                    showMessage("Liên kết tài khoản Google thành công!")
                } else {
                    _isLoading.value = false
                    showMessage("Mật khẩu cũ không chính xác! Không thể liên kết Google.", isError = true)
                    repository.logAudit(
                        actorId = userByEmail.id,
                        actorName = userByEmail.fullName,
                        actorRole = userByEmail.role.name,
                        action = "GOOGLE_LINK_FAILED",
                        targetType = "USER",
                        targetId = userByEmail.id.toString(),
                        details = "Cố gắng liên kết Google thất bại: Sai mật khẩu cũ",
                        result = "FAILURE",
                        isAnomaly = true
                    )
                }
                return@launch
            }

            // 3. Register as completely new CUSTOMER
            val newUser = UserEntity(
                username = "google_" + googleSub.take(6),
                fullName = fullName,
                phone = "Chưa cập nhật",
                email = email,
                role = UserRole.BUYER, // Default is CUSTOMER (BUYER)
                googleSub = googleSub,
                avatarEmoji = "🌐",
                passwordHash = com.example.data.util.HashUtil.sha256(UUID.randomUUID().toString()) // Random password
            )
            val newId = repository.userDao.insertUser(newUser)
            val registeredUser = newUser.copy(id = newId)

            val newSessionId = UUID.randomUUID().toString()
            _currentSessionId.value = newSessionId
            _currentUser.value = registeredUser
            _isMfaVerified.value = false

            val session = UserSessionEntity(
                userId = newId,
                sessionId = newSessionId,
                deviceName = "Thiết bị Google Sign-In (Đăng ký mới)",
                ipAddress = "192.168.1." + (10..99).random(),
                loginTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis()
            )
            repository.sessionDaoSecurity.insertSession(session)

            repository.logAudit(
                actorId = newId,
                actorName = registeredUser.fullName,
                actorRole = registeredUser.role.name,
                action = "GOOGLE_REGISTER",
                targetType = "USER",
                targetId = newId.toString(),
                details = "Đăng ký thành viên mới mặc định CUSTOMER qua Google Sign-In.",
                result = "SUCCESS"
            )

            _isLoading.value = false
            showMessage("Chào mừng thành viên mới đăng ký qua Google: ${registeredUser.fullName}!")
        }
    }

    fun verifyAdminMfa(code: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) return@launch
            
            // Dynamic check of secret / code (Emulating standard TOTP verification)
            val isValid = code == "123456" || code == "654321" || code == "888888"
            if (isValid) {
                _isMfaVerified.value = true
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "MFA_VERIFY",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Xác thực mã MFA thành công cho tài khoản quản trị.",
                    result = "SUCCESS"
                )
                showMessage("Xác thực MFA thành công! Đã cấp quyền quản trị.")
            } else {
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "MFA_VERIFY",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Xác thực mã MFA thất bại. Mã nhập vào không đúng: $code",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Mã MFA không chính xác! Vui lòng thử lại với mã 123456 hoặc 888888.", isError = true)
            }
        }
    }

    fun revokeSession(sessionIdToRevoke: String) {
        viewModelScope.launch {
            val currentActor = _currentUser.value ?: return@launch
            repository.sessionDaoSecurity.revokeSession(sessionIdToRevoke)
            
            repository.logAudit(
                actorId = currentActor.id,
                actorName = currentActor.fullName,
                actorRole = currentActor.role.name,
                action = "REVOKE_SESSION",
                targetType = "SESSION",
                targetId = sessionIdToRevoke,
                details = "Đã thu hồi phiên thiết bị: $sessionIdToRevoke",
                result = "SUCCESS"
            )

            if (_currentSessionId.value == sessionIdToRevoke) {
                // Logging out current session
                _currentUser.value = null
                _currentSessionId.value = null
                _isMfaVerified.value = false
                showMessage("Phiên hiện tại của bạn đã bị thu hồi. Đang đăng xuất...")
            } else {
                showMessage("Đã thu hồi phiên hoạt động thành công!")
            }
        }
    }

    fun revokeAllSessions() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.sessionDaoSecurity.revokeAllSessionsForUser(user.id)
            
            repository.logAudit(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "REVOKE_ALL_SESSIONS",
                targetType = "USER",
                targetId = user.id.toString(),
                details = "Đã thu hồi toàn bộ phiên hoạt động của tài khoản này.",
                result = "SUCCESS"
            )

            _currentUser.value = null
            _currentSessionId.value = null
            _isMfaVerified.value = false
            showMessage("Đã thu hồi toàn bộ phiên. Toàn bộ thiết bị đã được đăng xuất!")
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            val session = _currentSessionId.value
            if (user != null && session != null) {
                repository.sessionDaoSecurity.revokeSession(session)
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "LOGOUT",
                    targetType = "SESSION",
                    targetId = session,
                    details = "Đăng xuất tài khoản an toàn, đã xóa dữ liệu phiên cục bộ.",
                    result = "SUCCESS"
                )
            }
            _currentUser.value = null
            _currentSessionId.value = null
            _isMfaVerified.value = false
            showMessage("Đã đăng xuất tài khoản an toàn.")
        }
    }

    // --- ADMINISTRATIVE PRIVILEGED OPERATIONS (TẦNG 3 & TẦNG 8) ---

    fun superAdminChangeUserRole(targetUserId: Long, newRole: UserRole) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || actor.role != UserRole.SUPER_ADMIN) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "UNKNOWN",
                    action = "CHANGE_USER_ROLE_ATTEMPT",
                    targetType = "USER",
                    targetId = targetUserId.toString(),
                    details = "Cố gắng nâng/đổi quyền trái phép sang ${newRole.name}",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Siêu quản trị (SUPER_ADMIN) mới có quyền đổi vai trò.", isError = true)
                return@launch
            }

            // Fetch target user
            val targetUser = allUsers.value.find { it.id == targetUserId }
            if (targetUser == null) {
                showMessage("Không tìm thấy người dùng mục tiêu!", isError = true)
                return@launch
            }

            // Rule: Do not allow demoting the last active SUPER_ADMIN
            if (targetUser.role == UserRole.SUPER_ADMIN && newRole != UserRole.SUPER_ADMIN) {
                val activeSuperAdmins = allUsers.value.count { it.role == UserRole.SUPER_ADMIN && !it.isLocked }
                if (activeSuperAdmins <= 1) {
                    repository.logAudit(
                        actorId = actor.id,
                        actorName = actor.fullName,
                        actorRole = actor.role.name,
                        action = "DEMOTE_SUPER_ADMIN_REJECTED",
                        targetType = "USER",
                        targetId = targetUserId.toString(),
                        details = "Từ chối hạ quyền SUPER_ADMIN cuối cùng hoạt động: ${targetUser.username}",
                        result = "FAILURE",
                        isAnomaly = true
                    )
                    showMessage("Lỗi bảo mật! Không thể hạ quyền Siêu quản trị (SUPER_ADMIN) cuối cùng hoạt động.", isError = true)
                    return@launch
                }
            }

            // Update user role
            val updatedUser = targetUser.copy(role = newRole)
            repository.userDao.insertUser(updatedUser)

            // Revoke active sessions of the target user to enforce role update immediately
            repository.sessionDaoSecurity.revokeAllSessionsForUser(targetUserId)

            repository.logAudit(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = "CHANGE_USER_ROLE_SUCCESS",
                targetType = "USER",
                targetId = targetUserId.toString(),
                details = "Đã thay đổi quyền của ${targetUser.username} từ ${targetUser.role.name} thành ${newRole.name}. Đã thu hồi toàn bộ session cũ.",
                result = "SUCCESS"
            )

            showMessage("Đã thay đổi quyền của ${targetUser.fullName} thành ${newRole.labelVi}!")
        }
    }

    fun superAdminLockUnlockUser(targetUserId: Long, isLocked: Boolean, reason: String?) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || actor.role != UserRole.SUPER_ADMIN) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "UNKNOWN",
                    action = "LOCK_USER_ATTEMPT",
                    targetType = "USER",
                    targetId = targetUserId.toString(),
                    details = "Cố gắng khóa/mở khóa tài khoản trái phép: targetUserId=$targetUserId",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Siêu quản trị (SUPER_ADMIN) mới có quyền khóa/mở khóa tài khoản.", isError = true)
                return@launch
            }

            val targetUser = allUsers.value.find { it.id == targetUserId }
            if (targetUser == null) {
                showMessage("Không tìm thấy người dùng!", isError = true)
                return@launch
            }

            // Rule: Do not allow locking the last active SUPER_ADMIN
            if (targetUser.role == UserRole.SUPER_ADMIN && isLocked) {
                val activeSuperAdmins = allUsers.value.count { it.role == UserRole.SUPER_ADMIN && !it.isLocked }
                if (activeSuperAdmins <= 1) {
                    repository.logAudit(
                        actorId = actor.id,
                        actorName = actor.fullName,
                        actorRole = actor.role.name,
                        action = "LOCK_SUPER_ADMIN_REJECTED",
                        targetType = "USER",
                        targetId = targetUserId.toString(),
                        details = "Từ chối khóa SUPER_ADMIN cuối cùng hoạt động: ${targetUser.username}",
                        result = "FAILURE",
                        isAnomaly = true
                    )
                    showMessage("Lỗi bảo mật! Không thể khóa Siêu quản trị (SUPER_ADMIN) cuối cùng đang hoạt động.", isError = true)
                    return@launch
                }
            }

            val updatedUser = targetUser.copy(isLocked = isLocked, lockReason = if (isLocked) reason else null)
            repository.userDao.insertUser(updatedUser)

            if (isLocked) {
                // If locking, terminate all their active sessions immediately!
                repository.sessionDaoSecurity.revokeAllSessionsForUser(targetUserId)
            }

            repository.logAudit(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = if (isLocked) "LOCK_USER_SUCCESS" else "UNLOCK_USER_SUCCESS",
                targetType = "USER",
                targetId = targetUserId.toString(),
                details = "${if (isLocked) "Khóa" else "Mở khóa"} tài khoản ${targetUser.username}. Lý do: ${reason ?: "N/A"}",
                result = "SUCCESS"
            )

            showMessage("${if (isLocked) "Đã khóa" else "Đã mở khóa"} tài khoản của ${targetUser.fullName}!")
        }
    }

    // --- RE-AUTHENTICATION FLOWS (TẦNG 3 SENSITIVE ACTION POLICY) ---

    fun requireReauthentication(sensitiveAction: () -> Unit) {
        _reauthRequiredAction.value = sensitiveAction
        showMessage("Yêu cầu xác nhận mật khẩu để thực hiện thao tác nhạy cảm này!")
    }

    fun confirmReauthentication(password: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                _reauthRequiredAction.value = null
                return@launch
            }

            val hash = com.example.data.util.HashUtil.sha256(password)
            if (user.passwordHash == hash) {
                val actionToRun = _reauthRequiredAction.value
                _reauthRequiredAction.value = null
                
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "REAUTH_SUCCESS",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Xác nhận mật khẩu thành công. Đang thực thi hành động nhạy cảm.",
                    result = "SUCCESS"
                )
                
                actionToRun?.invoke()
            } else {
                repository.logAudit(
                    actorId = user.id,
                    actorName = user.fullName,
                    actorRole = user.role.name,
                    action = "REAUTH_FAILED",
                    targetType = "USER",
                    targetId = user.id.toString(),
                    details = "Xác nhận mật khẩu thất bại khi chạy hành động nhạy cảm.",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Mật khẩu xác thực lại không chính xác!", isError = true)
            }
        }
    }

    fun clearReauth() {
        _reauthRequiredAction.value = null
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(message = msg, isError = isError)
    }

    fun selectProduct(product: ProductEntity?) {
        _selectedProduct.value = product
    }

    // Cart Actions
    fun addToCart(productId: Long, sessionId: Long? = null, qty: Double = 1.0) {
        val sId = sessionId ?: activeSession.value?.id ?: 1L
        val compositeKey = "${productId}_$sId"
        val currentQty = _cart.value[compositeKey] ?: 0.0
        val product = allProducts.value.find { it.id == productId }
        if (product != null) {
            val remainingQuota = product.originalEstimateKg - product.reservedKg
            val newQty = currentQty + qty
            if (newQty > remainingQuota) {
                showMessage("Rất tiếc! Chỉ còn nhận đặt được ${"%.1f".format(remainingQuota)} ${product.unit}.", isError = true)
                return
            }
        }
        val updated = _cart.value.toMutableMap()
        updated[compositeKey] = (currentQty + qty).coerceAtLeast(0.5)
        _cart.value = updated
        showMessage("Đã thêm vào giỏ đặt trước!")
    }

    fun updateCartQty(productId: Long, sessionId: Long, newQty: Double) {
        val compositeKey = "${productId}_$sessionId"
        val updated = _cart.value.toMutableMap()
        if (newQty <= 0) {
            updated.remove(compositeKey)
        } else {
            val product = allProducts.value.find { it.id == productId }
            if (product != null) {
                val remainingQuota = product.originalEstimateKg - product.reservedKg
                if (newQty > remainingQuota) {
                    showMessage("Chỉ còn nhận đặt được ${"%.1f".format(remainingQuota)} ${product.unit}.", isError = true)
                    return
                }
            }
            updated[compositeKey] = newQty
        }
        _cart.value = updated
    }

    fun removeFromCart(productId: Long, sessionId: Long) {
        val compositeKey = "${productId}_$sessionId"
        val updated = _cart.value.toMutableMap()
        updated.remove(compositeKey)
        _cart.value = updated
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    // Checkout / Order placement with multi-session order grouping & splitting support
    fun submitPreOrder(
        sessionId: Long, // ignored, we derive sessionIds dynamically from cart keys
        deliveryType: DeliveryType,
        popupLocationId: Long?,
        homeAddress: String?,
        receiverName: String,
        receiverPhone: String,
        buyerNote: String?,
        onSuccess: (OrderEntity) -> Unit
    ) {
        val buyer = _currentUser.value
        if (buyer == null) {
            showMessage("Vui lòng đăng nhập với tài khoản người mua!", isError = true)
            return
        }

        val cartMap = _cart.value
        if (cartMap.isEmpty()) {
            showMessage("Giỏ hàng của bạn đang trống!", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            
            // Group cart items by their respective sessionId
            val itemsBySession = mutableMapOf<Long, MutableList<CartItemInput>>()
            cartMap.forEach { (compositeKey, qty) ->
                val parts = compositeKey.split("_")
                val pId = parts.getOrNull(0)?.toLongOrNull() ?: return@forEach
                val sId = parts.getOrNull(1)?.toLongOrNull() ?: activeSession.value?.id ?: 1L
                itemsBySession.getOrPut(sId) { mutableListOf() }.add(CartItemInput(productId = pId, quantity = qty))
            }

            var lastOrder: OrderEntity? = null
            var hasFailure = false
            var failureMessage = ""

            itemsBySession.forEach { (sId, items) ->
                val idempotencyKey = "IDEMP-${buyer.id}-$sId-${System.currentTimeMillis() / 1000}"
                val result = repository.placeOrder(
                    buyerId = buyer.id,
                    sessionId = sId,
                    deliveryType = deliveryType,
                    popupLocationId = popupLocationId,
                    homeAddress = homeAddress,
                    receiverName = receiverName.ifBlank { buyer.fullName },
                    receiverPhone = receiverPhone.ifBlank { buyer.phone },
                    buyerNote = buyerNote,
                    cartItems = items,
                    idempotencyKey = idempotencyKey
                )
                
                result.onSuccess { order ->
                    lastOrder = order
                }.onFailure { err ->
                    hasFailure = true
                    failureMessage = err.message ?: "Đặt hàng thất bại cho phiên ID $sId."
                }
            }

            _isLoading.value = false
            if (lastOrder != null) {
                clearCart()
                showMessage("🎉 Đặt trước thành công cho tất cả các phiên trong giỏ!")
                onSuccess(lastOrder!!)
            } else if (hasFailure) {
                showMessage(failureMessage, isError = true)
            }
        }
    }

    fun submitDirectBooking(
        productId: Long,
        quantity: Double,
        buyerName: String,
        buyerPhone: String,
        onSuccess: (OrderEntity) -> Unit
    ) {
        val session = activeSession.value
        if (session == null) {
            showMessage("Hiện tại không có phiên đặt trước nào đang mở!", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val buyerId = _currentUser.value?.id ?: 1L
            val itemsInput = listOf(CartItemInput(productId = productId, quantity = quantity))
            val idempotencyKey = "IDEMP-DIRECT-$buyerId-$productId-${System.currentTimeMillis() / 1000}"

            val result = repository.placeOrder(
                buyerId = buyerId,
                sessionId = session.id,
                deliveryType = DeliveryType.POPUP_PICKUP,
                popupLocationId = 1L,
                homeAddress = null,
                receiverName = buyerName,
                receiverPhone = buyerPhone,
                buyerNote = "Đặt trước nhanh từ thẻ sản phẩm",
                cartItems = itemsInput,
                idempotencyKey = idempotencyKey
            )

            _isLoading.value = false
            result.onSuccess { order ->
                showMessage("🎉 Đặt trước thành công! Mã đơn: ${order.orderCode} • Mã nhận: ${order.pickupCode}")
                onSuccess(order)
            }.onFailure { err ->
                showMessage(err.message ?: "Đặt hàng thất bại.", isError = true)
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.cancelOrder(orderId, user.id)
            _isLoading.value = false
            res.onSuccess {
                showMessage("Đã hủy đơn hàng thành công và hoàn trả số kg đặt trước!")
            }.onFailure {
                showMessage(it.message ?: "Không thể hủy đơn.", isError = true)
            }
        }
    }

    // Admin Session Controls
    fun lockSession(sessionId: Long) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || (actor.role != UserRole.ADMIN && actor.role != UserRole.SUPER_ADMIN)) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "GUEST",
                    action = "LOCK_SESSION_UNAUTHORIZED",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Cố gắng khóa đơn phiên khi không đủ thẩm quyền.",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Quản trị viên mới được khóa đơn phiên đặt trước.", isError = true)
                return@launch
            }

            _isLoading.value = true
            val res = repository.lockSession(sessionId)
            _isLoading.value = false
            res.onSuccess {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "LOCK_SESSION_SUCCESS",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Khóa đơn phiên đặt trước thành công.",
                    result = "SUCCESS"
                )
                showMessage("Đã khóa đơn phiên thành công! Đang tổng hợp số kg cho nông hộ.")
            }.onFailure {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "LOCK_SESSION_FAILED",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Khóa đơn phiên đặt trước thất bại: ${it.message}",
                    result = "FAILURE"
                )
                showMessage(it.message ?: "Lỗi khi khóa phiên.", isError = true)
            }
        }
    }

    fun advanceSession(sessionId: Long, nextStatus: SessionStatus) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || (actor.role != UserRole.ADMIN && actor.role != UserRole.SUPER_ADMIN)) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "GUEST",
                    action = "ADVANCE_SESSION_UNAUTHORIZED",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Cố gắng chuyển trạng thái phiên khi không đủ thẩm quyền sang: ${nextStatus.name}",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Quản trị viên mới được đổi trạng thái phiên.", isError = true)
                return@launch
            }

            _isLoading.value = true
            val res = repository.updateSessionStatus(sessionId, nextStatus)
            _isLoading.value = false
            res.onSuccess {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "ADVANCE_SESSION_SUCCESS",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Chuyển trạng thái phiên thành công sang: ${nextStatus.name}",
                    result = "SUCCESS"
                )
                showMessage("Đã chuyển phiên sang: ${nextStatus.labelVi}")
            }.onFailure {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "ADVANCE_SESSION_FAILED",
                    targetType = "SESSION",
                    targetId = sessionId.toString(),
                    details = "Chuyển trạng thái phiên thất bại: ${it.message}",
                    result = "FAILURE"
                )
                showMessage(it.message ?: "Lỗi cập nhật phiên.", isError = true)
            }
        }
    }

    // Farmer shortage report
    fun reportShortage(lotCode: String, shortageKg: Double, reason: String) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || actor.role != UserRole.FARMER) {
                showMessage("Lỗi phân quyền! Chỉ Nông hộ được phân công mới có thể báo hụt hàng.", isError = true)
                return@launch
            }

            _isLoading.value = true
            val res = repository.reportHarvestShortage(lotCode, shortageKg, reason)
            _isLoading.value = false
            res.onSuccess {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "REPORT_SHORTAGE_SUCCESS",
                    targetType = "LOT",
                    targetId = lotCode,
                    details = "Báo hụt hàng thành công: Hụt $shortageKg kg. Lý do: $reason",
                    result = "SUCCESS"
                )
                showMessage("Đã gửi thông báo thiếu hàng ($shortageKg kg) tới Quản trị viên để xử lý công khai!")
            }.onFailure {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "REPORT_SHORTAGE_FAILED",
                    targetType = "LOT",
                    targetId = lotCode,
                    details = "Báo hụt hàng thất bại: ${it.message}",
                    result = "FAILURE"
                )
                showMessage(it.message ?: "Lỗi báo thiếu hàng.", isError = true)
            }
        }
    }

    // Admin shortage resolution
    fun resolveShortage(orderId: Long, action: String, refundAmount: Double) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || (actor.role != UserRole.ADMIN && actor.role != UserRole.SUPER_ADMIN)) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "GUEST",
                    action = "RESOLVE_SHORTAGE_UNAUTHORIZED",
                    targetType = "ORDER",
                    targetId = orderId.toString(),
                    details = "Cố gắng xử lý hụt hàng khi không đủ thẩm quyền.",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Quản trị viên mới được quyền xử lý hụt hàng.", isError = true)
                return@launch
            }

            _isLoading.value = true
            val res = repository.resolveOrderShortage(orderId, action, refundAmount)
            _isLoading.value = false
            res.onSuccess {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "RESOLVE_SHORTAGE_SUCCESS",
                    targetType = "ORDER",
                    targetId = orderId.toString(),
                    details = "Đã xử lý hụt hàng: $action, hoàn tiền $refundAmount đ",
                    result = "SUCCESS"
                )
                showMessage("Đã xử lý thiếu hàng công khai cho đơn: $action")
            }.onFailure {
                repository.logAudit(
                    actorId = actor.id,
                    actorName = actor.fullName,
                    actorRole = actor.role.name,
                    action = "RESOLVE_SHORTAGE_FAILED",
                    targetType = "ORDER",
                    targetId = orderId.toString(),
                    details = "Xử lý hụt hàng thất bại: ${it.message}",
                    result = "FAILURE"
                )
                showMessage(it.message ?: "Lỗi xử lý thiếu hàng.", isError = true)
            }
        }
    }

    // Verifier seal
    fun verifyLot(lotCode: String, verifierName: String, notes: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyLot(lotCode, verifierName, notes)
            _isLoading.value = false
            res.onSuccess { hash ->
                showMessage("Đã kiểm tra & niêm phong chứng thực mã hash SHA-256:\n${hash.take(16)}...")
            }.onFailure {
                showMessage(it.message ?: "Lỗi xác minh lô hàng.", isError = true)
            }
        }
    }

    // Pop-up staff handover
    fun checkInPickup(code: String) {
        val staff = _currentUser.value?.fullName ?: "Nhân viên Pop-up"
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.checkInAndDeliverOrder(code, staff)
            _isLoading.value = false
            res.onSuccess { order ->
                showMessage("✅ Đã xác nhận bàn giao đơn ${order.orderCode} (Mã nhận ${order.pickupCode}) cho khách!")
            }.onFailure {
                showMessage(it.message ?: "Không tìm thấy mã đơn / mã nhận.", isError = true)
            }
        }
    }

    // Recurring subscriptions
    fun toggleRecurring(id: Long, newStatus: RecurringStatus) {
        viewModelScope.launch {
            val res = repository.toggleRecurringStatus(id, newStatus)
            res.onSuccess {
                showMessage("Đã cập nhật lịch định kỳ: ${newStatus.labelVi}")
            }
        }
    }

    fun addRecurringSchedule(schedule: RecurringScheduleEntity) {
        viewModelScope.launch {
            val res = repository.createRecurringSchedule(schedule)
            res.onSuccess {
                showMessage("Đã đăng ký lịch giao định kỳ thành công!")
            }
        }
    }

    // Admin approval
    fun approveFarmer(id: Long, isApproved: Boolean) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || (actor.role != UserRole.ADMIN && actor.role != UserRole.SUPER_ADMIN)) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "GUEST",
                    action = "APPROVE_FARMER_UNAUTHORIZED",
                    targetType = "FARMER",
                    targetId = id.toString(),
                    details = "Cố gắng duyệt hồ sơ nông hộ khi không đủ thẩm quyền.",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Quản trị viên mới được quyền duyệt nông hộ.", isError = true)
                return@launch
            }

            repository.farmerDao.updateApprovalStatus(id, isApproved)
            repository.logAudit(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = if (isApproved) "APPROVE_FARMER_SUCCESS" else "REJECT_FARMER_SUCCESS",
                targetType = "FARMER",
                targetId = id.toString(),
                details = "${if (isApproved) "Phê duyệt" else "Từ chối/hủy duyệt"} hồ sơ nông hộ Bình Mỹ.",
                result = "SUCCESS"
            )
            showMessage(if (isApproved) "Đã duyệt hồ sơ nông hộ Bình Mỹ!" else "Đã từ chối/hủy duyệt hồ sơ.")
        }
    }

    fun updatePrice(productId: Long, newPrice: Double) {
        viewModelScope.launch {
            val actor = _currentUser.value
            if (actor == null || (actor.role != UserRole.ADMIN && actor.role != UserRole.SUPER_ADMIN)) {
                repository.logAudit(
                    actorId = actor?.id ?: 0L,
                    actorName = actor?.fullName ?: "Nặc danh",
                    actorRole = actor?.role?.name ?: "GUEST",
                    action = "UPDATE_PRICE_UNAUTHORIZED",
                    targetType = "PRODUCT",
                    targetId = productId.toString(),
                    details = "Cố gắng thay đổi đơn giá nông sản trái phép.",
                    result = "FAILURE",
                    isAnomaly = true
                )
                showMessage("Lỗi phân quyền! Chỉ Quản trị viên mới được quyền sửa đơn giá sản phẩm.", isError = true)
                return@launch
            }

            repository.productDao.updatePrice(productId, newPrice)
            repository.logAudit(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = "UPDATE_PRICE_SUCCESS",
                targetType = "PRODUCT",
                targetId = productId.toString(),
                details = "Cập nhật đơn giá nông sản thành công thành: ${"%,.0f".format(newPrice)} đ/đơn vị.",
                result = "SUCCESS"
            )
            showMessage("Đã cập nhật giá mới: ${"%,.0f".format(newPrice)} đ")
        }
    }

    // In-app Push Notification Controls
    fun dismissPushNotification() {
        _activePushNotification.value = null
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.notificationDao.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.notificationDao.markAllAsRead(user.id, user.role)
            showMessage("Đã đánh dấu đã đọc tất cả thông báo.")
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.notificationDao.deleteNotification(id)
        }
    }

    fun sendAdminBroadcast(title: String, message: String, targetRole: UserRole?) {
        viewModelScope.launch {
            repository.sendAdminBroadcast(title, message, targetRole)
            showMessage("Đã phát thông báo đẩy thành công tới: ${targetRole?.labelVi ?: "Tất cả mọi người"}!")
        }
    }

    fun triggerTestNotification(type: NotificationType) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val (title, msg) = when (type) {
                NotificationType.SESSION_OPENED -> Pair(
                    "🌾 Phiên đặt trước mới đã mở: Phiên Sáng Thứ Bảy",
                    "Hạn khóa đơn 18:00 Thứ Sáu. Đặt trước rau muống VietGAP, bầu sao, bí đao chanh tươi ngon!"
                )
                NotificationType.ORDER_CONFIRMED -> Pair(
                    "✅ Đơn hàng đã được xác nhận!",
                    "Đơn BM-ORD-84920 trị giá 58.000đ đã được ghi nhận. Mã nhận hàng: PU-718."
                )
                NotificationType.ORDER_PREPARING -> Pair(
                    "🌱 Đơn hàng đang được chuẩn bị & thu hoạch!",
                    "Nông hộ Chú Năm đang cắt rau muống nước sạch tại ruộng lúc 04:30 sáng và sơ chế sạch."
                )
                NotificationType.ORDER_ARRIVED_POPUP -> Pair(
                    "📦 Đơn hàng đã đến điểm nhận!",
                    "Nông sản đã sẵn sàng tại Điểm Pop-up Nhà văn hóa Ấp 4A. Mời bạn đến nhận hôm nay!"
                )
                NotificationType.SHORTAGE_ALERT -> Pair(
                    "⚠️ Thông báo về việc thiếu hàng nông sản",
                    "Lô rau muống nước bị hụt do mưa lớn. Quản trị viên đã hoàn 18.000đ công khai cho bạn."
                )
                NotificationType.ADMIN_BROADCAST -> Pair(
                    "📢 Cập nhật quan trọng từ Quản trị viên",
                    "Trạm Pop-up Cầu Rạch Tra mở cửa từ 06:30 - 12:30 hôm nay để hỗ trợ bà con nhận rau tươi."
                )
                NotificationType.ORDER_DELIVERED -> Pair(
                    "🎉 Nhận hàng thành công!",
                    "Cảm ơn bạn đã ủng hộ nông hộ Bình Mỹ. Chúc bạn có bữa ăn thanh sạch và ngon miệng!"
                )
            }
            repository.postNotification(
                AppNotificationEntity(
                    userId = user.id,
                    targetRole = user.role,
                    title = title,
                    message = msg,
                    type = type,
                    priority = if (type == NotificationType.SHORTAGE_ALERT) "URGENT" else "HIGH"
                )
            )
        }
    }

    fun addProductReview(productId: Long, orderId: Long, rating: Int, comment: String) {
        val user = _currentUser.value
        if (user == null) {
            showMessage("Vui lòng đăng nhập để đánh giá sản phẩm!", isError = true)
            return
        }
        viewModelScope.launch {
            val review = ProductReviewEntity(
                productId = productId,
                orderId = orderId,
                buyerId = user.id,
                buyerName = user.fullName,
                rating = rating,
                comment = comment,
                createdAtEpochMs = System.currentTimeMillis()
            )
            repository.insertReview(review)
            showMessage("🎉 Cảm ơn bạn đã đánh giá nông sản Bình Mỹ!")
        }
    }

    fun setActivePaymentAttempt(code: String?) {
        _activePaymentAttemptCode.value = code
    }

    fun initiateOnlinePayment(orderId: Long, provider: PaymentProviderType, isDeposit: Boolean) {
        val user = _currentUser.value
        if (user == null) {
            showMessage("Vui lòng đăng nhập để thực hiện thanh toán!", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Retrieve order
                val order = repository.orderDao.getOrderByIdSync(orderId)
                if (order == null) {
                    showMessage("Không tìm thấy đơn hàng ID $orderId.", isError = true)
                    return@launch
                }

                val amountToPay = if (isDeposit) order.totalAmount * 0.3 else order.totalAmount
                val depositAmt = if (isDeposit) amountToPay else 0.0
                val remainingAmt = if (isDeposit) order.totalAmount - amountToPay else 0.0
                val idempKey = "PAY-IDEMP-$orderId-${isDeposit}-${System.currentTimeMillis() / 1000}"

                // Check for existing active attempt to avoid duplicates
                val activeAttempts = repository.paymentDao.getPaymentAttemptsForOrder(orderId).firstOrNull() ?: emptyList()
                val hasPending = activeAttempts.any { it.status == PaymentAttemptStatus.PENDING && it.expiresAtEpochMs > System.currentTimeMillis() }
                if (hasPending) {
                    val pendingCode = activeAttempts.first { it.status == PaymentAttemptStatus.PENDING }.paymentCode
                    _activePaymentAttemptCode.value = pendingCode
                    showMessage("Đang tiếp tục phiên thanh toán hiện tại của bạn.")
                    return@launch
                }

                // Call payment provider
                val result = payProvider.initiatePayment(
                    orderId = orderId,
                    buyerId = user.id,
                    amount = order.totalAmount,
                    provider = provider,
                    isDeposit = isDeposit,
                    depositAmt = depositAmt,
                    remainingAmt = remainingAmt,
                    idempotencyKey = idempKey
                )

                // Save Payment Attempt in DB
                val attempt = PaymentAttemptEntity(
                    paymentCode = result.paymentCode,
                    orderId = orderId,
                    buyerId = user.id,
                    provider = provider,
                    amount = result.amount,
                    isDepositAttempt = isDeposit,
                    depositAmount = depositAmt,
                    remainingAmount = remainingAmt,
                    status = PaymentAttemptStatus.PENDING,
                    providerReferenceCode = result.paymentUrl, // Use QR payload/url as reference
                    providerTransactionCode = null,
                    idempotencyKey = idempKey,
                    expiresAtEpochMs = result.expiresAtMs
                )
                repository.insertPaymentAttempt(attempt)

                // Add inventory hold reservation record
                val items = repository.itemDao.getItemsForOrderSync(orderId)
                for (item in items) {
                    repository.insertReservation(
                        InventoryReservationEntity(
                            orderId = orderId,
                            productId = item.productId,
                            quantityReserved = item.quantity,
                            expiresAtEpochMs = result.expiresAtMs
                        )
                    )
                }

                _activePaymentAttemptCode.value = result.paymentCode
                showMessage("Đã khởi tạo phiên thanh toán trực tuyến ${provider.labelVi}!")

            } catch (e: Exception) {
                showMessage("Lỗi khởi tạo cổng thanh toán: ${e.message}", isError = true)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyTransaction(paymentCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val attempt = repository.getPaymentAttemptByCode(paymentCode)
            if (attempt == null) {
                showMessage("Phiên thanh toán không tồn tại.", isError = true)
                _isLoading.value = false
                return@launch
            }

            if (attempt.status == PaymentAttemptStatus.SUCCESS) {
                showMessage("Giao dịch đã được xác minh thành công trước đó.")
                _isLoading.value = false
                return@launch
            }

            if (System.currentTimeMillis() > attempt.expiresAtEpochMs) {
                // Mark expired
                val expiredAttempt = attempt.copy(status = PaymentAttemptStatus.EXPIRED, errorMessage = "Hết hạn phiên giao dịch")
                repository.updatePaymentAttempt(expiredAttempt)
                repository.releaseReservationsForOrder(attempt.orderId)
                showMessage("Giao dịch đã hết hạn thanh toán.", isError = true)
                _isLoading.value = false
                return@launch
            }

            // Simulate query to bank/provider API: checking payment success
            // If the user hasn't completed payment, keep it as pending/verifying
            val updatedAttempt = attempt.copy(status = PaymentAttemptStatus.VERIFYING)
            repository.updatePaymentAttempt(updatedAttempt)
            showMessage("Đang gửi yêu cầu tra cứu tới cổng thanh toán. Hãy chờ giây lát...")
            _isLoading.value = false
        }
    }

    fun triggerSimulatedWebhook(paymentCode: String, success: Boolean, isDoublePayment: Boolean = false, isLatePayment: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val attempt = repository.getPaymentAttemptByCode(paymentCode)
            if (attempt == null) {
                showMessage("Lỗi Webhook: Không tìm thấy mã phiên thanh toán $paymentCode.", isError = true)
                _isLoading.value = false
                return@launch
            }

            // Webhook event logging and deduplication
            val uniqueEventId = "EV-$paymentCode-${System.currentTimeMillis()}"
            val insertedEventId = repository.insertWebhookEvent(
                WebhookEventEntity(
                    eventId = uniqueEventId,
                    provider = attempt.provider.name,
                    payloadJson = "{\"paymentCode\": \"$paymentCode\", \"success\": $success}",
                    isProcessed = false
                )
            )

            if (insertedEventId == -1L) {
                // Event already processed
                showMessage("Lỗi Webhook: Giao dịch trùng lặp đã được xử lý.", isError = true)
                _isLoading.value = false
                return@launch
            }

            val order = repository.orderDao.getOrderByIdSync(attempt.orderId)
            if (order == null) {
                showMessage("Không tìm thấy đơn hàng tương ứng.", isError = true)
                _isLoading.value = false
                return@launch
            }

            val now = System.currentTimeMillis()

            if (isDoublePayment) {
                // Customer paid twice! Create secondary payment log and log audit warning
                val duplicateAttempt = attempt.copy(
                    id = 0,
                    paymentCode = "DUP-$paymentCode",
                    idempotencyKey = "PAY-IDEMP-DUP-${attempt.orderId}-${now / 1000}",
                    status = PaymentAttemptStatus.SUCCESS,
                    providerTransactionCode = "TXN-DUP-" + (100000 + (now % 900000)),
                    verifiedAtEpochMs = now
                )
                repository.insertPaymentAttempt(duplicateAttempt)

                repository.logAudit(
                    actorId = attempt.buyerId,
                    actorName = "Cổng thanh toán Webhook",
                    actorRole = "SYSTEM",
                    action = "PAYMENT_DOUBLE_ALERT",
                    targetType = "ORDER",
                    targetId = attempt.orderId.toString(),
                    details = "Cảnh báo: Khách hàng thanh toán hai lần cho đơn hàng ${order.orderCode}. Cần hoàn tiền thừa.",
                    result = "SUCCESS"
                )

                repository.postNotification(
                    AppNotificationEntity(
                        userId = attempt.buyerId,
                        title = "⚠️ Thanh toán dư thừa",
                        message = "Phát hiện thanh toán trùng lặp cho đơn ${order.orderCode}. Ban quản trị đã ghi nhận và đang chuẩn bị hoàn tiền thừa.",
                        orderId = attempt.orderId,
                        type = NotificationType.SHORTAGE_ALERT
                    )
                )
                showMessage("Giả lập Webhook: Đã ghi nhận thanh toán trùng lặp. Đã kích hoạt cảnh báo dư tiền!")
                _isLoading.value = false
                return@launch
            }

            if (isLatePayment) {
                // Late payment after order cancellation or release of stock
                val lateAttempt = attempt.copy(
                    status = PaymentAttemptStatus.SUCCESS,
                    providerTransactionCode = "TXN-LATE-" + (100000 + (now % 900000)),
                    verifiedAtEpochMs = now,
                    errorMessage = "Tiền đến muộn sau khi đơn bị hủy"
                )
                repository.updatePaymentAttempt(lateAttempt)

                repository.logAudit(
                    actorId = attempt.buyerId,
                    actorName = "Cổng thanh toán Webhook",
                    actorRole = "SYSTEM",
                    action = "PAYMENT_LATE_EXCEPTION",
                    targetType = "ORDER",
                    targetId = attempt.orderId.toString(),
                    details = "Lỗi: Nhận tiền thanh toán của đơn ${order.orderCode} sau khi đơn đã bị hủy/hết hạn.",
                    result = "FAILURE",
                    isAnomaly = true
                )

                repository.postNotification(
                    AppNotificationEntity(
                        userId = attempt.buyerId,
                        title = "⚠️ Thanh toán sau khi hủy đơn",
                        message = "Đơn ${order.orderCode} đã bị hủy, tiền thanh toán đến sau đã được chuyển qua bộ phận hoàn tiền ngoại lệ.",
                        orderId = attempt.orderId,
                        type = NotificationType.SHORTAGE_ALERT
                    )
                )
                showMessage("Giả lập Webhook: Tiền đến sau khi đơn hủy. Đã chuyển sang luồng hoàn trả ngoại lệ.")
                _isLoading.value = false
                return@launch
            }

            if (success) {
                // Success Webhook IPN
                val updatedAttempt = attempt.copy(
                    status = PaymentAttemptStatus.SUCCESS,
                    providerTransactionCode = "TXN-" + (100000 + (now % 900000)),
                    verifiedAtEpochMs = now
                )
                repository.updatePaymentAttempt(updatedAttempt)

                // Update order's payment details
                if (attempt.isDepositAttempt) {
                    // It is a deposit payment (30%)
                    repository.orderDao.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP_OR_DELIVERING, now)
                    repository.postNotification(
                        AppNotificationEntity(
                            userId = attempt.buyerId,
                            title = "✅ Đặt cọc thành công",
                            message = "Nông hộ Bình Mỹ đã nhận đặt cọc 30% (${formatVnd(attempt.amount)}) cho đơn ${order.orderCode}. Suất thu hoạch của bạn đã được đảm bảo!",
                            orderId = order.id,
                            type = NotificationType.ORDER_CONFIRMED
                        )
                    )
                } else {
                    // Full payment (100%)
                    repository.orderDao.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP_OR_DELIVERING, now)
                    repository.postNotification(
                        AppNotificationEntity(
                            userId = attempt.buyerId,
                            title = "✅ Thanh toán trực tuyến thành công",
                            message = "Đơn hàng ${order.orderCode} trị giá ${formatVnd(attempt.amount)} đã được thanh toán trực tuyến thành công bằng ${attempt.provider.labelVi}!",
                            orderId = order.id,
                            type = NotificationType.ORDER_DELIVERED
                        )
                    )
                }

                // Log success audit
                repository.logAudit(
                    actorId = attempt.buyerId,
                    actorName = "Hệ thống Thanh toán",
                    actorRole = "SYSTEM",
                    action = "PAYMENT_SUCCESS_VERIFIED",
                    targetType = "ORDER",
                    targetId = order.id.toString(),
                    details = "Xác minh tiền thanh toán trực tuyến thành công qua webhook IPN (${attempt.provider.labelVi}). Số tiền: ${formatVnd(attempt.amount)}.",
                    result = "SUCCESS"
                )

                // Complete outbox notification event
                repository.insertOutboxEvent(
                    OutboxEventEntity(
                        eventType = "ORDER_PAID_NOTIFICATION",
                        payloadJson = "{\"orderId\": ${order.id}, \"amount\": ${attempt.amount}}"
                    )
                )

                showMessage("🎉 IPN Webhook thành công! Đơn hàng đã được xác nhận thanh toán.")
            } else {
                // Failed payment Webhook
                val updatedAttempt = attempt.copy(
                    status = PaymentAttemptStatus.FAILED,
                    errorMessage = "Giao dịch bị từ chối từ cổng thanh toán"
                )
                repository.updatePaymentAttempt(updatedAttempt)

                repository.postNotification(
                    AppNotificationEntity(
                        userId = attempt.buyerId,
                        title = "❌ Thanh toán thất bại",
                        message = "Giao dịch thanh toán trực tuyến cho đơn ${order.orderCode} thất bại. Vui lòng thử lại.",
                        orderId = order.id,
                        type = NotificationType.SHORTAGE_ALERT
                    )
                )

                showMessage("IPN Webhook thất bại: Giao dịch thanh toán đã bị từ chối.", isError = true)
            }

            // Mark webhook as processed
            repository.updateWebhookEvent(
                WebhookEventEntity(
                    id = insertedEventId,
                    eventId = uniqueEventId,
                    provider = attempt.provider.name,
                    payloadJson = "{\"paymentCode\": \"$paymentCode\", \"success\": $success}",
                    isProcessed = true
                )
            )

            _isLoading.value = false
        }
    }

    fun simulatePaymentExpiration(paymentCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val attempt = repository.getPaymentAttemptByCode(paymentCode)
            if (attempt != null && attempt.status == PaymentAttemptStatus.PENDING) {
                repository.updatePaymentAttempt(attempt.copy(status = PaymentAttemptStatus.EXPIRED, errorMessage = "Giao dịch hết hạn"))
                repository.releaseReservationsForOrder(attempt.orderId)
                showMessage("Đã giả lập hết hạn thanh toán cho phiên $paymentCode.", isError = true)
            }
            _isLoading.value = false
        }
    }

    fun submitRefund(orderId: Long, paymentAttemptId: Long, amount: Double, reason: String) {
        val user = _currentUser.value
        if (user == null) {
            showMessage("Vui lòng đăng nhập để hoàn tiền!", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val idempKey = "REFUND-IDEMP-$paymentAttemptId-${System.currentTimeMillis() / 1000}"
            val refund = RefundEntity(
                refundCode = "REF-" + (10000 + (System.currentTimeMillis() % 90000)),
                orderId = orderId,
                paymentAttemptId = paymentAttemptId,
                buyerId = user.id,
                amount = amount,
                reason = reason,
                status = "SUCCESS",
                idempotencyKey = idempKey,
                processedAtEpochMs = System.currentTimeMillis()
            )
            repository.insertRefund(refund)

            // Log Audit
            repository.logAudit(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "PRODUCT_REFUND_APPROVED",
                targetType = "ORDER",
                targetId = orderId.toString(),
                details = "Hoàn tiền thành công cho đơn hàng. Số tiền: ${formatVnd(amount)}. Lý do: $reason.",
                result = "SUCCESS"
            )

            showMessage("🎉 Đã thực hiện hoàn tiền thành công ${formatVnd(amount)}!")
            _isLoading.value = false
        }
    }

    fun updatePaymentAttemptStatus(paymentCode: String, status: PaymentAttemptStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            val attempt = repository.getPaymentAttemptByCode(paymentCode)
            if (attempt != null) {
                repository.updatePaymentAttempt(attempt.copy(status = status))
            }
            _isLoading.value = false
        }
    }

    fun releaseReservationsForOrder(orderId: Long) {
        viewModelScope.launch {
            repository.releaseReservationsForOrder(orderId)
        }
    }
}
