package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.*
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.util.HashUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppRepository(private val db: AppDatabase) {

    // DAOs
    val userDao = db.userDao()
    val farmerDao = db.farmerProfileDao()
    val sessionDao = db.preOrderSessionDao()
    val productDao = db.productDao()
    val lotDao = db.productLotDao()
    val popupDao = db.popUpLocationDao()
    val orderDao = db.orderDao()
    val itemDao = db.orderItemDao()
    val recurringDao = db.recurringScheduleDao()
    val notificationDao = db.appNotificationDao()
    val sessionDaoSecurity = db.userSessionDao()
    val auditLogDao = db.auditLogDao()
    val reviewDao = db.productReviewDao()
    val paymentDao = db.paymentAttemptDao()

    // Flows
    val allSessions: Flow<List<PreOrderSessionEntity>> = sessionDao.getAllSessions()
    val activeSession: Flow<PreOrderSessionEntity?> = sessionDao.getActiveSession()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allFarmers: Flow<List<FarmerProfileEntity>> = farmerDao.getAllProfiles()
    val allLots: Flow<List<ProductLotEntity>> = lotDao.getAllLots()
    val allPopups: Flow<List<PopUpLocationEntity>> = popupDao.getAllLocations()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val allAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    val allPaymentAttempts: Flow<List<PaymentAttemptEntity>> = paymentDao.getAllPaymentAttempts()
    val allRefunds: Flow<List<RefundEntity>> = paymentDao.getAllRefunds()

    fun getPaymentAttemptsForOrder(orderId: Long): Flow<List<PaymentAttemptEntity>> = paymentDao.getPaymentAttemptsForOrder(orderId)
    fun getPaymentAttemptsForBuyer(buyerId: Long): Flow<List<PaymentAttemptEntity>> = paymentDao.getPaymentAttemptsForBuyer(buyerId)
    fun getRefundsForOrder(orderId: Long): Flow<List<RefundEntity>> = paymentDao.getRefundsForOrder(orderId)

    suspend fun insertPaymentAttempt(attempt: PaymentAttemptEntity): Long = withContext(Dispatchers.IO) {
        paymentDao.insertPaymentAttempt(attempt)
    }

    suspend fun updatePaymentAttempt(attempt: PaymentAttemptEntity) = withContext(Dispatchers.IO) {
        paymentDao.updatePaymentAttempt(attempt)
    }

    suspend fun getPaymentAttemptByCode(code: String): PaymentAttemptEntity? = withContext(Dispatchers.IO) {
        paymentDao.getPaymentAttemptByCode(code)
    }

    suspend fun insertRefund(refund: RefundEntity): Long = withContext(Dispatchers.IO) {
        paymentDao.insertRefund(refund)
    }

    suspend fun updateRefund(refund: RefundEntity) = withContext(Dispatchers.IO) {
        paymentDao.updateRefund(refund)
    }

    suspend fun insertWebhookEvent(event: WebhookEventEntity): Long = withContext(Dispatchers.IO) {
        paymentDao.insertWebhookEvent(event)
    }

    suspend fun getWebhookEventById(eventId: String): WebhookEventEntity? = withContext(Dispatchers.IO) {
        paymentDao.getWebhookEventById(eventId)
    }

    suspend fun updateWebhookEvent(event: WebhookEventEntity) = withContext(Dispatchers.IO) {
        paymentDao.updateWebhookEvent(event)
    }

    suspend fun insertReservation(reservation: InventoryReservationEntity): Long = withContext(Dispatchers.IO) {
        paymentDao.insertReservation(reservation)
    }

    suspend fun getActiveReservationsForOrder(orderId: Long): List<InventoryReservationEntity> = withContext(Dispatchers.IO) {
        paymentDao.getActiveReservationsForOrder(orderId)
    }

    suspend fun releaseReservationsForOrder(orderId: Long) = withContext(Dispatchers.IO) {
        paymentDao.releaseReservationsForOrder(orderId)
    }

    suspend fun insertOutboxEvent(event: OutboxEventEntity): Long = withContext(Dispatchers.IO) {
        paymentDao.insertOutboxEvent(event)
    }

    suspend fun getPendingOutboxEvents(): List<OutboxEventEntity> = withContext(Dispatchers.IO) {
        paymentDao.getPendingOutboxEvents()
    }

    suspend fun updateOutboxEvent(event: OutboxEventEntity) = withContext(Dispatchers.IO) {
        paymentDao.updateOutboxEvent(event)
    }

    fun getReviewsForProduct(productId: Long): Flow<List<ProductReviewEntity>> = reviewDao.getReviewsForProduct(productId)
    fun getAllReviews(): Flow<List<ProductReviewEntity>> = reviewDao.getAllReviews()
    suspend fun getReviewByOrderAndProduct(orderId: Long, productId: Long): ProductReviewEntity? = reviewDao.getReviewByOrderAndProduct(orderId, productId)

    suspend fun insertReview(review: ProductReviewEntity): Long = withContext(Dispatchers.IO) {
        reviewDao.insertReview(review)
    }

    fun getOrdersByBuyer(buyerId: Long): Flow<List<OrderEntity>> = orderDao.getOrdersByBuyer(buyerId)
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>> = itemDao.getItemsForOrder(orderId)
    fun getRecurringSchedules(buyerId: Long): Flow<List<RecurringScheduleEntity>> = recurringDao.getSchedulesByBuyer(buyerId)
    fun getNotifications(userId: Long, role: UserRole): Flow<List<AppNotificationEntity>> = notificationDao.getNotificationsForUser(userId, role)
    fun getUnreadNotificationCount(userId: Long, role: UserRole): Flow<Int> = notificationDao.getUnreadCount(userId, role)
    fun getActiveSessionsForUser(userId: Long): Flow<List<UserSessionEntity>> = sessionDaoSecurity.getActiveSessionsForUser(userId)

    suspend fun logAudit(
        actorId: Long,
        actorName: String,
        actorRole: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String,
        result: String,
        isAnomaly: Boolean = false
    ) {
        val log = AuditLogEntity(
            actorId = actorId,
            actorName = actorName,
            actorRole = actorRole,
            action = action,
            targetType = targetType,
            targetId = targetId,
            details = details,
            timestamp = System.currentTimeMillis(),
            result = result,
            isAnomaly = isAnomaly
        )
        auditLogDao.insertLog(log)
    }

    // Event bus for heads-up push notification banner across all screens
    val latestPushNotification = kotlinx.coroutines.flow.MutableSharedFlow<AppNotificationEntity>(extraBufferCapacity = 10)

    suspend fun postNotification(notification: AppNotificationEntity) {
        val id = notificationDao.insertNotification(notification)
        latestPushNotification.tryEmit(notification.copy(id = id))
    }

    suspend fun initializeDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val hasProducts = productDao.getProductCount() > 0
        val hasUsers = userDao.getUserCount() > 0
        val hasSessions = sessionDao.getSessionCount() > 0
        if (hasProducts && hasUsers && hasSessions) return@withContext

        // 1. Demo Farmers
        val farmer1 = FarmerProfileEntity(
            householdName = "[DỮ LIỆU MẪU] Hộ Chú Năm - Tổ 3 Bình Mỹ",
            representativeName = "Nguyễn Văn Năm (Chú Năm)",
            phone = "0918 223 344",
            addressDetail = "Tổ 3, Ấp 4A, Xã Bình Mỹ, Củ Chi, TP.HCM",
            acreageHa = 1.4,
            hasVietGap = true,
            vietGapCertCode = "BM-VG-2024-059",
            vietGapValidUntil = "30/11/2026",
            isApprovedByAdmin = true,
            bio = "Canh tác rau muống nước sạch truyền thống hơn 15 năm tại xã Bình Mỹ. Đạt chuẩn VietGAP từ 2024, quy trình tưới tiêu nguồn nước giếng tự nhiên kiểm định định kỳ.",
            mainCrops = "Rau muống nước cắt sáng, rau muống cạn lá liễu"
        )
        val farmer2 = FarmerProfileEntity(
            householdName = "[DỮ LIỆU MẪU] Hộ Cô Ba - Vườn Xanh Ấp 2",
            representativeName = "Trần Thị Ba (Cô Ba)",
            phone = "0933 556 677",
            addressDetail = "Tổ 1, Ấp 2, Xã Bình Mỹ, Củ Chi, TP.HCM",
            acreageHa = 0.85,
            hasVietGap = true,
            vietGapCertCode = "BM-VG-2024-012",
            vietGapValidUntil = "15/10/2026",
            isApprovedByAdmin = true,
            bio = "Chuyên canh luân canh các loại quả dây leo hữu cơ: bầu sao, bí đao chanh, mướp hương. Không dùng thuốc diệt cỏ hóa học.",
            mainCrops = "Bầu sao Bình Mỹ, bí đao chanh"
        )
        val farmer3 = FarmerProfileEntity(
            householdName = "[DỮ LIỆU MẪU] Hộ Bác Bảy - Ruộng Phù Sa",
            representativeName = "Lê Hoàng Bảy",
            phone = "0988 991 122",
            addressDetail = "Tổ 5, Ấp 1, Xã Bình Mỹ, Củ Chi, TP.HCM",
            acreageHa = 1.1,
            hasVietGap = false,
            vietGapCertCode = "Đang thẩm định mở rộng",
            vietGapValidUntil = "2026",
            isApprovedByAdmin = false, // Pending approval for CUJ testing
            bio = "Hộ nông dân đăng ký tham gia mô hình đặt trước vụ mới, đang hoàn thiện hồ sơ xét duyệt VietGAP.",
            mainCrops = "Rau dền đỏ, mồng tơi vườn phù sa"
        )
        val f1Id = farmerDao.insertProfile(farmer1)
        val f2Id = farmerDao.insertProfile(farmer2)
        val f3Id = farmerDao.insertProfile(farmer3)

        // 2. Demo Users
        val users = listOf(
            UserEntity(
                username = "buyer_demo",
                fullName = "Chị Lê Thu Trang",
                phone = "0903 123 456",
                email = "thutrang.buyer@binhmy.vn",
                role = UserRole.BUYER,
                avatarEmoji = "👩‍💼",
                passwordHash = HashUtil.sha256("buyer123")
            ),
            UserEntity(
                username = "farmer_demo",
                fullName = "Chú Nguyễn Văn Năm",
                phone = "0918 223 344",
                email = "chunambinhmy@binhmy.vn",
                role = UserRole.FARMER,
                farmerProfileId = f1Id,
                avatarEmoji = "👨‍🌾",
                passwordHash = HashUtil.sha256("farmer123")
            ),
            UserEntity(
                username = "admin_demo",
                fullName = "Ban Điều Phối HTX Bình Mỹ",
                phone = "028 3795 888",
                email = "dieuphoi@htxbinhmy.gov.vn",
                role = UserRole.ADMIN,
                avatarEmoji = "🏛️",
                passwordHash = HashUtil.sha256("admin123"),
                isMfaEnabled = true,
                mfaSecret = "SUPERSECRET123"
            ),
            UserEntity(
                username = "super_admin_demo",
                fullName = "Siêu Quản Trị Hệ Thống",
                phone = "0900 000 000",
                email = "superadmin@htxbinhmy.gov.vn",
                role = UserRole.SUPER_ADMIN,
                avatarEmoji = "👑",
                passwordHash = HashUtil.sha256("super123"),
                isMfaEnabled = true,
                mfaSecret = "SUPERSECRET999"
            ),
            UserEntity(
                username = "popup_demo",
                fullName = "Anh Nguyễn Văn Tuấn (Trạm 4A)",
                phone = "0977 112 233",
                email = "popup.ap4a@binhmy.vn",
                role = UserRole.POPUP_STAFF,
                assignedPopupLocationId = 1L,
                avatarEmoji = "📦",
                passwordHash = HashUtil.sha256("popup123")
            ),
            UserEntity(
                username = "verifier_demo",
                fullName = "KS. Trần Quốc Đạt (Kiểm Định VietGAP)",
                phone = "0944 556 789",
                email = "kiemdinh.vietgap@nongnghiep.gov.vn",
                role = UserRole.VERIFIER,
                avatarEmoji = "🔬",
                passwordHash = HashUtil.sha256("verifier123")
            )
        )
        userDao.insertUsers(users)

        // 3. Demo Pop-up Locations
        val popups = listOf(
            PopUpLocationEntity(
                name = "Điểm Pop-up 1: Nhà văn hóa Ấp 4A",
                address = "Đường Tỉnh Lộ 9, Xã Bình Mỹ, Huyện Củ Chi, TP.HCM",
                operatingHours = "06:30 - 11:30 & 15:30 - 18:30 (Thứ Bảy & Chủ Nhật)",
                contactPhone = "0977 112 233",
                supportsHomeDelivery = true,
                baseDeliveryFee = 15000.0,
                notes = "Có khu mát lưu tạm nông sản rau muống, thùng xốp đá giữ tươi"
            ),
            PopUpLocationEntity(
                name = "Điểm Pop-up 2: Trạm Cầu Rạch Tra",
                address = "Ngã ba Cầu Rạch Tra - Tỉnh Lộ 9, Xã Bình Mỹ",
                operatingHours = "06:30 - 10:30 (Thứ Bảy)",
                contactPhone = "0938 445 566",
                supportsHomeDelivery = true,
                baseDeliveryFee = 20000.0,
                notes = "Thuận tiện cho khách trên trục đường kết nối Q12 & Củ Chi"
            ),
            PopUpLocationEntity(
                name = "Điểm Pop-up 3: Trạm Trung Chuyển Q12 - Hóc Môn",
                address = "Đường Lê Văn Khương (gần chân cầu Rạch Tra phía Nam)",
                operatingHours = "07:00 - 12:00 (Thứ Bảy)",
                contactPhone = "0912 334 455",
                supportsHomeDelivery = true,
                baseDeliveryFee = 25000.0,
                notes = "Hỗ trợ khách nội thành nhận nông sản sớm sau thu hoạch 2 giờ"
            )
        )
        popupDao.insertLocations(popups)

        // 4. Demo Pre-Order Session
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 3600 * 1000L
        val activeSessionEntity = PreOrderSessionEntity(
            sessionCode = "PHIEN-2026-T10-01",
            title = "Phiên Thu Hoạch Sáng Thứ Bảy - Đợt 10/2026",
            description = "Nông sản được thu hoạch lúc 04:30 sáng và chuyển trực tiếp đến các điểm nhận Pop-up từ 07:00 sáng. Giữ trọn vị ngọt tự nhiên, không ngâm hóa chất bảo quản.",
            openDateEpochMs = now - oneDayMs,
            closeDateEpochMs = now + (2 * oneDayMs), // 2 days left
            deliveryDateEpochMs = now + (3 * oneDayMs),
            status = SessionStatus.OPEN,
            cancellationPolicy = "Quý khách được chỉnh sửa số lượng hoặc hủy đơn miễn phí trước 18:00 ngày khóa đơn. Sau khi khóa đơn, nông hộ sẽ cắt rau theo số kg đã chốt."
        )
        val sessionId = sessionDao.insertSession(activeSessionEntity)

        val activeSessionEntity2 = PreOrderSessionEntity(
            sessionCode = "PHIEN-2026-T10-02",
            title = "Phiên Giữa Tuần Kế Tiếp - Đợt 10/2026",
            description = "Phiên thu hoạch khép kín đặc biệt phục vụ nhu cầu ăn rau xanh bổ dưỡng giữa tuần của cư dân đô thị. Gom trạm Pop-up tiện lợi.",
            openDateEpochMs = now - oneDayMs,
            closeDateEpochMs = now + (5 * oneDayMs), // 5 days left
            deliveryDateEpochMs = now + (6 * oneDayMs),
            status = SessionStatus.OPEN,
            cancellationPolicy = "Cho phép sửa hoặc hủy miễn phí trước hạn khóa đơn 18:00 ngày trước thu hoạch."
        )
        sessionDao.insertSession(activeSessionEntity2)

        // 5. Demo Products & Lots
        val lot1Code = "LOT-BM-2026-RM01"
        val lot2Code = "LOT-BM-2026-RM02"
        val lot3Code = "LOT-BM-2026-BS01"
        val lot4Code = "LOT-BM-2026-BD01"
        val lot5Code = "LOT-BM-2026-RD01"

        val products = listOf(
            ProductEntity(
                farmerId = f1Id,
                name = "[DỮ LIỆU MẪU] Rau muống nước Bình Mỹ VietGAP",
                category = "Rau ăn lá",
                unit = "kg",
                currentPricePerUnit = 18000.0,
                originalEstimateKg = 120.0,
                reservedKg = 34.0,
                storageInstructions = "Bọc giấy báo hoặc màng bọc thực phẩm, để ngăn mát tủ lạnh (8-12°C). Dùng ngon nhất trong 48h.",
                originInfo = "Canh tác tại thửa đất số 42, Ấp 4A, Bình Mỹ. Nước giếng khoan tự nhiên, kiểm nghiệm kim loại nặng đạt chỉ số an toàn.",
                lotCode = lot1Code,
                imageUrl = "🥬"
            ),
            ProductEntity(
                farmerId = f1Id,
                name = "[DỮ LIỆU MẪU] Rau muống cạn lá liễu non",
                category = "Rau ăn lá",
                unit = "kg",
                currentPricePerUnit = 22000.0,
                originalEstimateKg = 80.0,
                reservedKg = 18.0,
                storageInstructions = "Tránh dập gãy ngọn non, giữ khô ráo trước khi cho vào ngăn mát tủ lạnh.",
                originInfo = "Trồng trên luống đất cát pha ven sông Sài Gòn, bón phân trùn quế và ủ rơm mục.",
                lotCode = lot2Code,
                imageUrl = "🌱"
            ),
            ProductEntity(
                farmerId = f2Id,
                name = "[DỮ LIỆU MẪU] Bầu sao Bình Mỹ hữu cơ",
                category = "Củ quả quả",
                unit = "kg",
                currentPricePerUnit = 25000.0,
                originalEstimateKg = 60.0,
                reservedKg = 12.0,
                storageInstructions = "Để nơi thoáng mát tự nhiên được 5-7 ngày hoặc ngăn rau củ tủ lạnh.",
                originInfo = "Giàn bầu leo tự nhiên tại Ấp 2 Bình Mỹ, thụ phấn tự nhiên, bao trái chống ruồi vàng.",
                lotCode = lot3Code,
                imageUrl = "🥒"
            ),
            ProductEntity(
                farmerId = f2Id,
                name = "[DỮ LIỆU MẪU] Bí đao chanh cùi dày",
                category = "Củ quả quả",
                unit = "kg",
                currentPricePerUnit = 26000.0,
                originalEstimateKg = 50.0,
                reservedKg = 8.0,
                storageInstructions = "Bảo quản nơi khô ráo, tránh ánh nắng trực tiếp. Để được 10-15 ngày.",
                originInfo = "Hộ Cô Ba Vườn Xanh, giống bí đao chanh thuần, quả vừa nấu canh thơm mát.",
                lotCode = lot4Code,
                imageUrl = "🍈"
            ),
            ProductEntity(
                farmerId = f3Id,
                name = "[DỮ LIỆU MẪU] Rau dền đỏ vườn phù sa",
                category = "Rau ăn lá",
                unit = "bó 500g",
                currentPricePerUnit = 15000.0,
                originalEstimateKg = 40.0,
                reservedKg = 0.0,
                storageInstructions = "Để ngăn mát 2-3 ngày.",
                originInfo = "Đất bãi bồi ven rạch Bình Mỹ, rau mọc tự nhiên xanh mướt.",
                lotCode = lot5Code,
                imageUrl = "🌿"
            )
        )
        val pIds = products.map { productDao.insertProduct(it) }

        // 6. Product Lots
        val harvestTime = activeSessionEntity.deliveryDateEpochMs - (3 * 3600 * 1000L) // 04:00 AM delivery day
        val lot1Hash = HashUtil.sha256("$lot1Code:$f1Id:$harvestTime:BM-VG-2024-059")
        val lot3Hash = HashUtil.sha256("$lot3Code:$f2Id:$harvestTime:BM-VG-2024-012")

        val lots = listOf(
            ProductLotEntity(
                lotCode = lot1Code,
                productId = pIds[0],
                farmerId = f1Id,
                harvestDateEpochMs = harvestTime,
                estimatedYieldKg = 120.0,
                actualYieldKg = null,
                isShortage = false,
                verificationStatus = VerificationStatus.VERIFIED,
                verifierName = "KS. Trần Quốc Đạt",
                verificationTimeEpochMs = now - 3600000L,
                sha256Hash = lot1Hash,
                publicNotes = "Hồ sơ nhật ký đồng ruộng đạt chuẩn VietGAP đợt kiểm tra ngày 28/09. Nước tưới đạt chuẩn an toàn vệ sinh thực phẩm."
            ),
            ProductLotEntity(
                lotCode = lot2Code,
                productId = pIds[1],
                farmerId = f1Id,
                harvestDateEpochMs = harvestTime,
                estimatedYieldKg = 80.0,
                actualYieldKg = null,
                isShortage = false,
                verificationStatus = VerificationStatus.VERIFIED,
                verifierName = "KS. Trần Quốc Đạt",
                verificationTimeEpochMs = now - 3600000L,
                sha256Hash = HashUtil.sha256("$lot2Code:$f1Id:$harvestTime:BM-VG-2024-059"),
                publicNotes = "Rau cạn luống 3, thu hoạch đúng lứa 22 ngày sau gieo."
            ),
            ProductLotEntity(
                lotCode = lot3Code,
                productId = pIds[2],
                farmerId = f2Id,
                harvestDateEpochMs = harvestTime,
                estimatedYieldKg = 60.0,
                actualYieldKg = null,
                isShortage = false,
                verificationStatus = VerificationStatus.VERIFIED,
                verifierName = "KS. Trần Quốc Đạt",
                verificationTimeEpochMs = now - 7200000L,
                sha256Hash = lot3Hash,
                publicNotes = "Trái cắt đợt 1, trọng lượng đồng đều 1.2 - 1.5kg/trái."
            ),
            ProductLotEntity(
                lotCode = lot4Code,
                productId = pIds[3],
                farmerId = f2Id,
                harvestDateEpochMs = harvestTime,
                estimatedYieldKg = 50.0,
                actualYieldKg = null,
                isShortage = false,
                verificationStatus = VerificationStatus.PENDING, // For Verifier CUJ testing
                verifierName = null,
                verificationTimeEpochMs = null,
                sha256Hash = null,
                publicNotes = "Đang chờ thanh tra viên ký duyệt kiểm định trước giờ thu hoạch."
            ),
            ProductLotEntity(
                lotCode = lot5Code,
                productId = pIds[4],
                farmerId = f3Id,
                harvestDateEpochMs = harvestTime,
                estimatedYieldKg = 40.0,
                actualYieldKg = null,
                isShortage = false,
                verificationStatus = VerificationStatus.PENDING,
                verifierName = null,
                verificationTimeEpochMs = null,
                sha256Hash = null,
                publicNotes = "Hồ sơ mở rộng đang chuẩn bị thẩm tra."
            )
        )
        lotDao.insertLots(lots)

        // 7. Initial Demo Order for Buyer (to immediately see order tracking and pickup QR)
        val buyerUser = userDao.getUserByUsername("buyer_demo") ?: return@withContext
        val initialOrder = OrderEntity(
            orderCode = "BM-ORD-84920",
            buyerId = buyerUser.id,
            sessionId = sessionId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Lê Thu Trang",
            receiverPhone = "0903 123 456",
            status = OrderStatus.PLACED,
            deliveryFee = 0.0,
            totalAmount = 58000.0,
            pickupCode = "PU-718",
            idempotencyKey = "INIT-DEMO-ORDER-001",
            buyerNote = "Gom sớm giúp chị trước 08:30 sáng nha",
            createdAtEpochMs = now - 1800000L,
            updatedAtEpochMs = now - 1800000L
        )
        val oId = orderDao.insertOrder(initialOrder)
        val items = listOf(
            OrderItemEntity(
                orderId = oId,
                productId = pIds[0],
                productName = "[DỮ LIỆU MẪU] Rau muống nước Bình Mỹ VietGAP",
                farmerId = f1Id,
                farmerName = "Hộ Chú Năm - Tổ 3",
                unit = "kg",
                quantity = 2.0,
                unitPriceSnapshot = 18000.0,
                lineTotal = 36000.0
            ),
            OrderItemEntity(
                orderId = oId,
                productId = pIds[1],
                productName = "[DỮ LIỆU MẪU] Rau muống cạn lá liễu non",
                farmerId = f1Id,
                farmerName = "Hộ Chú Năm - Tổ 3",
                unit = "kg",
                quantity = 1.0,
                unitPriceSnapshot = 22000.0,
                lineTotal = 22000.0
            )
        )
        itemDao.insertOrderItems(items)

        // 8. Recurring Schedule demo
        val recurring = RecurringScheduleEntity(
            buyerId = buyerUser.id,
            productId = pIds[0],
            quantity = 3.0,
            frequency = RecurringFrequency.WEEKLY,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Lê Thu Trang",
            receiverPhone = "0903 123 456",
            status = RecurringStatus.ACTIVE,
            nextDeliveryDateEpochMs = activeSessionEntity.deliveryDateEpochMs + (7 * oneDayMs)
        )
        recurringDao.insertSchedule(recurring)

        // 9. Initial Notifications for all roles
        notificationDao.insertNotifications(
            listOf(
                AppNotificationEntity(
                    userId = buyerUser.id,
                    title = "Đơn hàng đã được xác nhận!",
                    message = "Đơn hàng BM-ORD-84920 cho phiên sáng Thứ Bảy đã được ghi nhận. Bạn có thể sửa hoặc hủy trước hạn khóa đơn.",
                    orderId = oId,
                    sessionId = sessionId,
                    type = NotificationType.ORDER_CONFIRMED,
                    priority = "HIGH"
                ),
                AppNotificationEntity(
                    userId = 0L,
                    targetRole = UserRole.FARMER,
                    title = "Phiên đặt mới mở: Phiên Sáng Thứ Bảy",
                    message = "Phiên đặt trước đã mở nhận đơn. Nông hộ vui lòng theo dõi sản lượng chốt tại Bảng điều khiển.",
                    sessionId = sessionId,
                    type = NotificationType.SESSION_OPENED,
                    priority = "NORMAL"
                ),
                AppNotificationEntity(
                    userId = 0L,
                    targetRole = UserRole.ADMIN,
                    title = "Thông báo hệ thống ban điều phối",
                    message = "Phiên đặt trước đợt 10/2026 đã kích hoạt. Kiểm soát tiến độ khóa đơn trước 18:00 Thứ Sáu.",
                    sessionId = sessionId,
                    type = NotificationType.ADMIN_BROADCAST,
                    priority = "NORMAL"
                ),
                AppNotificationEntity(
                    userId = 0L,
                    targetRole = UserRole.POPUP_STAFF,
                    title = "Lịch phân phối nông sản điểm Pop-up",
                    message = "Điểm nhận chuẩn bị đón nông sản tươi giao về trạm lúc 06:30 Thứ Bảy. Kiểm tra kho mát lưu tạm.",
                    sessionId = sessionId,
                    type = NotificationType.ORDER_PREPARING,
                    priority = "NORMAL"
                )
            )
        )
    }

    // Business Methods

    /**
     * Place a pre-order with strict validations:
     * - Check if session is open and cutoff deadline not passed
     * - Check product quota / available kg
     * - Snapshot current price
     * - Enforce idempotency (avoid double-click duplicates)
     */
    suspend fun placeOrder(
        buyerId: Long,
        sessionId: Long,
        deliveryType: DeliveryType,
        popupLocationId: Long?,
        homeAddress: String?,
        receiverName: String,
        receiverPhone: String,
        buyerNote: String?,
        cartItems: List<CartItemInput>,
        idempotencyKey: String
    ): Result<OrderEntity> = withContext(Dispatchers.IO) {
        // 1. Check idempotency
        val existingOrder = orderDao.getByIdempotencyKey(idempotencyKey)
        if (existingOrder != null) {
            return@withContext Result.success(existingOrder)
        }

        // 2. Validate session
        val session = sessionDao.getSessionByIdSync(sessionId)
            ?: return@withContext Result.failure(Exception("Phiên đặt trước không tồn tại."))

        val now = System.currentTimeMillis()
        if (session.status != SessionStatus.OPEN) {
            return@withContext Result.failure(Exception("Phiên này hiện không mở đặt trước (Trạng thái: ${session.status.labelVi})."))
        }
        if (now >= session.closeDateEpochMs) {
            return@withContext Result.failure(Exception("Đã quá hạn khóa đơn (${formatTime(session.closeDateEpochMs)}). Không thể đặt thêm."))
        }

        if (cartItems.isEmpty()) {
            return@withContext Result.failure(Exception("Giỏ hàng của bạn đang trống."))
        }

        // 3. Validate products and quotas
        var goodsTotal = 0.0
        val preparedItems = mutableListOf<OrderItemEntity>()

        for (cartItem in cartItems) {
            val product = productDao.getProductByIdSync(cartItem.productId)
                ?: return@withContext Result.failure(Exception("Sản phẩm ID ${cartItem.productId} không tồn tại."))

            val remainingQuota = product.originalEstimateKg - product.reservedKg
            if (cartItem.quantity > remainingQuota) {
                return@withContext Result.failure(
                    Exception("Sản phẩm \"${product.name}\" chỉ còn nhận đặt được ${"%.1f".format(remainingQuota)} ${product.unit}. Bạn đã yêu cầu ${cartItem.quantity} ${product.unit}.")
                )
            }

            val farmer = farmerDao.getProfileByIdSync(product.farmerId)
            val lineTotal = cartItem.quantity * product.currentPricePerUnit
            goodsTotal += lineTotal

            preparedItems.add(
                OrderItemEntity(
                    orderId = 0, // will set after insert
                    productId = product.id,
                    productName = product.name,
                    farmerId = product.farmerId,
                    farmerName = farmer?.householdName ?: "Nông hộ Bình Mỹ",
                    unit = product.unit,
                    quantity = cartItem.quantity,
                    unitPriceSnapshot = product.currentPricePerUnit, // Strict snapshot
                    lineTotal = lineTotal
                )
            )
        }

        // Delivery fee
        var deliveryFee = 0.0
        if (deliveryType == DeliveryType.HOME_DELIVERY) {
            val popup = popupLocationId?.let { popupDao.getLocationByIdSync(it) }
            deliveryFee = popup?.baseDeliveryFee ?: 20000.0
        }
        val grandTotal = goodsTotal + deliveryFee

        val orderCode = "BM-ORD-" + (10000 + (System.currentTimeMillis() % 90000))
        val pickupCode = "PU-" + (100 + (System.currentTimeMillis() % 900))

        val order = OrderEntity(
            orderCode = orderCode,
            buyerId = buyerId,
            sessionId = sessionId,
            deliveryType = deliveryType,
            popupLocationId = popupLocationId,
            homeAddress = homeAddress,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            status = OrderStatus.PLACED,
            deliveryFee = deliveryFee,
            totalAmount = grandTotal,
            pickupCode = pickupCode,
            idempotencyKey = idempotencyKey,
            buyerNote = buyerNote,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val insertedOrderId = orderDao.insertOrder(order)
        val finalItems = preparedItems.map { it.copy(orderId = insertedOrderId) }
        itemDao.insertOrderItems(finalItems)

        // Increment reservedKg on each product
        for (item in cartItems) {
            productDao.updateReservedKg(item.productId, item.quantity)
        }

        // Send in-app push notifications for all relevant roles:
        // 1. To Buyer: Order confirmed
        postNotification(
            AppNotificationEntity(
                userId = buyerId,
                title = "Đơn hàng đã được xác nhận!",
                message = "Đơn $orderCode trị giá ${"%,.0f".format(grandTotal)}đ đã được ghi nhận. Mã nhận: $pickupCode. Quý khách có thể sửa/hủy trước hạn khóa đơn.",
                orderId = insertedOrderId,
                sessionId = sessionId,
                type = NotificationType.ORDER_CONFIRMED,
                priority = "HIGH"
            )
        )

        // 2. To Farmers: notify of new demand
        postNotification(
            AppNotificationEntity(
                userId = 0L,
                targetRole = UserRole.FARMER,
                title = "Nhận đơn đặt trước mới: $orderCode",
                message = "Đơn hàng mới với nông sản của nông hộ vừa được chốt đặt. Vui lòng theo dõi sản lượng chốt phiên.",
                orderId = insertedOrderId,
                sessionId = sessionId,
                type = NotificationType.ORDER_CONFIRMED,
                priority = "NORMAL"
            )
        )

        // 3. To Admin: monitoring update
        postNotification(
            AppNotificationEntity(
                userId = 0L,
                targetRole = UserRole.ADMIN,
                title = "Đơn đặt mới: $orderCode",
                message = "Khách $receiverName vừa đặt đơn $orderCode trị giá ${"%,.0f".format(grandTotal)}đ (${deliveryType.labelVi}).",
                orderId = insertedOrderId,
                sessionId = sessionId,
                type = NotificationType.ORDER_CONFIRMED,
                priority = "NORMAL"
            )
        )

        val finalOrder = orderDao.getOrderByIdSync(insertedOrderId) ?: order.copy(id = insertedOrderId)
        Result.success(finalOrder)
    }

    /**
     * Cancel an order before the session cutoff
     */
    suspend fun cancelOrder(orderId: Long, requestedByUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderByIdSync(orderId)
            ?: return@withContext Result.failure(Exception("Không tìm thấy đơn hàng."))

        if (order.status == OrderStatus.CANCELLED) {
            return@withContext Result.failure(Exception("Đơn hàng này đã bị hủy trước đó."))
        }

        val session = sessionDao.getSessionByIdSync(order.sessionId)
            ?: return@withContext Result.failure(Exception("Không tìm thấy thông tin phiên."))

        val now = System.currentTimeMillis()
        if (session.status != SessionStatus.OPEN || now >= session.closeDateEpochMs) {
            return@withContext Result.failure(
                Exception("Phiên đặt trước đã khóa đơn (${formatTime(session.closeDateEpochMs)}). Không thể tự ý hủy đơn vì nông hộ đã lên kế hoạch thu hoạch theo số kg đã chốt.")
            )
        }

        // Release reserved quota
        val items = itemDao.getItemsForOrderSync(orderId)
        for (item in items) {
            productDao.updateReservedKg(item.productId, -item.quantity)
        }

        orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED, now)
        postNotification(
            AppNotificationEntity(
                userId = order.buyerId,
                title = "Đã hủy đơn hàng",
                message = "Đơn hàng ${order.orderCode} đã được hủy thành công trước hạn khóa đơn và hoàn trả định mức cho phiên.",
                orderId = orderId,
                sessionId = order.sessionId,
                type = NotificationType.ORDER_CONFIRMED
            )
        )
        Result.success(Unit)
    }

    /**
     * Admin locks the session:
     * - Marks session as LOCKED
     * - Transitions all PLACED orders to LOCKED / PREPARING
     * - Freezes customer edits
     */
    suspend fun lockSession(sessionId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionByIdSync(sessionId)
            ?: return@withContext Result.failure(Exception("Không tìm thấy phiên."))

        sessionDao.updateSessionStatus(sessionId, SessionStatus.LOCKED)

        // Lock all active orders in this session
        val orders = orderDao.getOrdersBySessionSync(sessionId)
        val now = System.currentTimeMillis()
        for (order in orders) {
            if (order.status == OrderStatus.PLACED || order.status == OrderStatus.AWAITING_CONFIRMATION) {
                orderDao.updateOrderStatus(order.id, OrderStatus.LOCKED, now)
                postNotification(
                    AppNotificationEntity(
                        userId = order.buyerId,
                        title = "Phiên đặt trước đã khóa đơn",
                        message = "Đơn ${order.orderCode} đã được chuyển sang trạng thái khóa đơn. Nông hộ đang chuẩn bị thu hoạch theo số lượng đã chốt.",
                        orderId = order.id,
                        sessionId = sessionId,
                        type = NotificationType.ORDER_PREPARING,
                        priority = "NORMAL"
                    )
                )
            }
        }

        // Notify Farmers
        postNotification(
            AppNotificationEntity(
                userId = 0L,
                targetRole = UserRole.FARMER,
                title = "Đã chốt sổ phiên: ${session.sessionCode}",
                message = "Hạn khóa đơn đã kết thúc. Vui lòng vào Bảng điều khiển Nông hộ kiểm tra tổng số kg cần cắt sáng mai.",
                sessionId = sessionId,
                type = NotificationType.ADMIN_BROADCAST,
                priority = "HIGH"
            )
        )

        Result.success(Unit)
    }

    /**
     * Advance session status (OPEN, HARVESTING, DISTRIBUTING, COMPLETED)
     */
    suspend fun updateSessionStatus(sessionId: Long, newStatus: SessionStatus): Result<Unit> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionByIdSync(sessionId)
        sessionDao.updateSessionStatus(sessionId, newStatus)
        val orders = orderDao.getOrdersBySessionSync(sessionId)
        val now = System.currentTimeMillis()

        val targetOrderStatus = when (newStatus) {
            SessionStatus.HARVESTING -> OrderStatus.PREPARING
            SessionStatus.DISTRIBUTING -> OrderStatus.READY_FOR_PICKUP_OR_DELIVERING
            SessionStatus.COMPLETED -> OrderStatus.COMPLETED
            SessionStatus.CANCELLED -> OrderStatus.CANCELLED
            else -> null
        }

        if (targetOrderStatus != null) {
            for (order in orders) {
                if (order.status != OrderStatus.CANCELLED && order.status != OrderStatus.COMPLETED) {
                    orderDao.updateOrderStatus(order.id, targetOrderStatus, now)
                }
            }
        }

        // Push notifications according to new status:
        when (newStatus) {
            SessionStatus.OPEN -> {
                // Broadcast new session open to all buyers & farmers
                postNotification(
                    AppNotificationEntity(
                        userId = 0L,
                        targetRole = null, // Broadcast to all
                        title = "🌾 Phiên đặt trước mới đã mở!",
                        message = "Phiên \"${session?.title ?: "Mới"}\" vừa mở. Nông sản sạch Bình Mỹ chuẩn VietGAP sẵn sàng nhận đặt trước!",
                        sessionId = sessionId,
                        type = NotificationType.SESSION_OPENED,
                        priority = "HIGH"
                    )
                )
            }
            SessionStatus.HARVESTING -> {
                // Notify buyers their orders are being prepared
                for (order in orders.filter { it.status != OrderStatus.CANCELLED }) {
                    postNotification(
                        AppNotificationEntity(
                            userId = order.buyerId,
                            title = "🌱 Đơn hàng đang được chuẩn bị & thu hoạch!",
                            message = "Đơn ${order.orderCode} của bạn đang được nông hộ cắt sáng tại ruộng lúc 04:30 và sơ chế sạch.",
                            orderId = order.id,
                            sessionId = sessionId,
                            type = NotificationType.ORDER_PREPARING,
                            priority = "HIGH"
                        )
                    )
                }
                // Notify popup staff
                postNotification(
                    AppNotificationEntity(
                        userId = 0L,
                        targetRole = UserRole.POPUP_STAFF,
                        title = "📦 Nông hộ đang thu hoạch",
                        message = "Các nông hộ đang cắt rau cho phiên ${session?.sessionCode ?: ""}, chuẩn bị gom hàng về trạm.",
                        sessionId = sessionId,
                        type = NotificationType.ORDER_PREPARING,
                        priority = "NORMAL"
                    )
                )
            }
            SessionStatus.DISTRIBUTING -> {
                // Notify buyers orders arrived at Pop-up point
                for (order in orders.filter { it.status != OrderStatus.CANCELLED }) {
                    val pickupPopup = order.popupLocationId?.let { popupDao.getLocationByIdSync(it) }
                    val locName = pickupPopup?.name ?: "Điểm Pop-up Bình Mỹ"
                    postNotification(
                        AppNotificationEntity(
                            userId = order.buyerId,
                            title = "📦 Đơn hàng đã đến điểm nhận!",
                            message = "Nông sản đơn ${order.orderCode} đã sẵn sàng tại ${locName}. Vui lòng mang mã nhận [${order.pickupCode}] đến lấy hôm nay!",
                            orderId = order.id,
                            sessionId = sessionId,
                            type = NotificationType.ORDER_ARRIVED_POPUP,
                            priority = "HIGH"
                        )
                    )
                }
                // Notify popup staff
                postNotification(
                    AppNotificationEntity(
                        userId = 0L,
                        targetRole = UserRole.POPUP_STAFF,
                        title = "🏢 Hàng đã đến trạm Pop-up!",
                        message = "Nông sản đã chuyển về trạm. Vui lòng mở kho mát, sẵn sàng kiểm tra mã PU-xxx và bàn giao cho khách.",
                        sessionId = sessionId,
                        type = NotificationType.ORDER_ARRIVED_POPUP,
                        priority = "HIGH"
                    )
                )
            }
            SessionStatus.COMPLETED -> {
                postNotification(
                    AppNotificationEntity(
                        userId = 0L,
                        targetRole = null,
                        title = "🎉 Phiên thu hoạch đã hoàn tất!",
                        message = "Phiên ${session?.sessionCode ?: ""} đã hoàn thành tốt đẹp. Cảm ơn bà con nông hộ và quý khách hàng!",
                        sessionId = sessionId,
                        type = NotificationType.ORDER_DELIVERED,
                        priority = "NORMAL"
                    )
                )
            }
            else -> {}
        }

        Result.success(Unit)
    }

    /**
     * Farmer confirms harvest capability or reports shortage
     */
    suspend fun reportHarvestShortage(
        lotCode: String,
        shortageKg: Double,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val lot = lotDao.getLotByCodeSync(lotCode)
            ?: return@withContext Result.failure(Exception("Không tìm thấy lô hàng $lotCode"))

        lotDao.updateShortage(lotCode, shortageKg > 0, shortageKg, reason)

        // Flag affected orders as ISSUE_PENDING so admin can resolve publicly
        val product = productDao.getProductByIdSync(lot.productId)
        if (product != null && shortageKg > 0) {
            val allItems = itemDao.getAllOrderItems().firstOrNull() ?: emptyList()
            val affectedOrderIds = allItems.filter { it.productId == product.id }.map { it.orderId }.distinct()
            val now = System.currentTimeMillis()
            for (oId in affectedOrderIds) {
                val order = orderDao.getOrderByIdSync(oId)
                if (order != null && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.COMPLETED) {
                    orderDao.updateOrderStatus(oId, OrderStatus.ISSUE_PENDING, now)
                    postNotification(
                        AppNotificationEntity(
                            userId = order.buyerId,
                            title = "⚠️ Thông báo về việc thiếu hàng nông sản",
                            message = "Nông hộ thông báo sản lượng \"${product.name}\" bị hụt ${shortageKg} kg do: $reason. Quản trị viên đang xử lý hoàn tiền hoặc thay thế công khai cho bạn.",
                            orderId = oId,
                            type = NotificationType.SHORTAGE_ALERT,
                            priority = "URGENT"
                        )
                    )
                }
            }

            // Also alert Admin
            postNotification(
                AppNotificationEntity(
                    userId = 0L,
                    targetRole = UserRole.ADMIN,
                    title = "🚨 Báo cáo hụt sản lượng: Lô $lotCode",
                    message = "Nông hộ báo thiếu ${shortageKg} kg \"${product.name}\" do: $reason. Vui lòng vào duyệt xử lý bù hoàn cho các đơn bị ảnh hưởng.",
                    type = NotificationType.SHORTAGE_ALERT,
                    priority = "URGENT"
                )
            )
        }
        Result.success(Unit)
    }

    /**
     * Admin resolves shortage publicly (refund adjustment or substitute item)
     */
    suspend fun resolveOrderShortage(
        orderId: Long,
        compensationAction: String, // e.g. "Hoàn tiền phần thiếu 1kg (18.000đ)"
        refundAmount: Double,
        newStatus: OrderStatus = OrderStatus.PREPARING
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderByIdSync(orderId)
            ?: return@withContext Result.failure(Exception("Không tìm thấy đơn hàng."))

        val now = System.currentTimeMillis()
        orderDao.updateOrderCompensation(orderId, compensationAction, refundAmount, newStatus, now)

        postNotification(
            AppNotificationEntity(
                userId = order.buyerId,
                title = "💰 Đã xử lý bù hoàn đơn ${order.orderCode}",
                message = "Phương án giải quyết: $compensationAction. Tiền đã được cập nhật trừ ${"%,.0f".format(refundAmount)}đ khi nhận hàng. Cảm ơn bạn đã thông cảm!",
                orderId = orderId,
                type = NotificationType.SHORTAGE_ALERT,
                priority = "HIGH"
            )
        )
        Result.success(Unit)
    }

    /**
     * Admin broadcasts an important announcement to all users or specific roles
     */
    suspend fun sendAdminBroadcast(
        title: String,
        message: String,
        targetRole: UserRole? = null,
        priority: String = "HIGH"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        postNotification(
            AppNotificationEntity(
                userId = 0L,
                targetRole = targetRole,
                title = title,
                message = message,
                type = NotificationType.ADMIN_BROADCAST,
                priority = priority,
                timestampEpochMs = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    }

    /**
     * Verifier verifies lot and creates SHA-256 seal
     */
    suspend fun verifyLot(
        lotCode: String,
        verifierName: String,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val lot = lotDao.getLotByCodeSync(lotCode)
            ?: return@withContext Result.failure(Exception("Không tìm thấy lô hàng."))

        val farmer = farmerDao.getProfileByIdSync(lot.farmerId)
        val now = System.currentTimeMillis()
        val payload = "$lotCode:${lot.farmerId}:${lot.harvestDateEpochMs}:${farmer?.vietGapCertCode ?: "NONE"}:$now"
        val hash = HashUtil.sha256(payload)

        lotDao.updateVerification(
            lotCode = lotCode,
            status = VerificationStatus.VERIFIED,
            verifierName = verifierName,
            timeMs = now,
            hash = hash
        )
        Result.success(hash)
    }

    /**
     * Pop-up staff confirms handover to customer
     */
    suspend fun checkInAndDeliverOrder(pickupOrOrderCode: String, staffName: String): Result<OrderEntity> = withContext(Dispatchers.IO) {
        val cleanCode = pickupOrOrderCode.trim().uppercase()
        val order = orderDao.getOrderByPickupCodeSync(cleanCode)
            ?: orderDao.getOrderByCodeSync(cleanCode)
            ?: return@withContext Result.failure(Exception("Không tìm thấy đơn hàng với mã \"$cleanCode\""))

        if (order.status == OrderStatus.COMPLETED) {
            return@withContext Result.failure(Exception("Đơn hàng ${order.orderCode} đã được nhận trước đó rồi."))
        }
        if (order.status == OrderStatus.CANCELLED) {
            return@withContext Result.failure(Exception("Đơn hàng ${order.orderCode} đã bị hủy."))
        }

        val now = System.currentTimeMillis()
        orderDao.updateOrderStatus(order.id, OrderStatus.COMPLETED, now)

        postNotification(
            AppNotificationEntity(
                userId = order.buyerId,
                title = "🎉 Nhận hàng thành công!",
                message = "Đơn ${order.orderCode} đã được nhận tại điểm Pop-up (Bàn giao bởi: $staffName). Chúc bạn có bữa ăn ngon lành với nông sản tươi Bình Mỹ!",
                orderId = order.id,
                sessionId = order.sessionId,
                type = NotificationType.ORDER_DELIVERED,
                priority = "HIGH"
            )
        )

        val updated = orderDao.getOrderByIdSync(order.id) ?: order
        Result.success(updated)
    }

    /**
     * Recurring subscription controls
     */
    suspend fun toggleRecurringStatus(scheduleId: Long, newStatus: RecurringStatus): Result<Unit> = withContext(Dispatchers.IO) {
        recurringDao.updateStatus(scheduleId, newStatus)
        Result.success(Unit)
    }

    suspend fun createRecurringSchedule(schedule: RecurringScheduleEntity): Result<Long> = withContext(Dispatchers.IO) {
        val id = recurringDao.insertSchedule(schedule)
        Result.success(id)
    }

    private fun formatTime(epochMs: Long): String {
        return SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date(epochMs))
    }
}

data class CartItemInput(
    val productId: Long,
    val quantity: Double
)
