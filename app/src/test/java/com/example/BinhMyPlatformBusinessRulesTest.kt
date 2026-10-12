package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FarmerProfileEntity
import com.example.data.local.entity.PreOrderSessionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductLotEntity
import com.example.data.local.entity.RecurringScheduleEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DeliveryType
import com.example.data.model.OrderStatus
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringStatus
import com.example.data.model.SessionStatus
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.repository.AppRepository
import com.example.data.repository.CartItemInput
import com.example.data.util.HashUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BinhMyPlatformBusinessRulesTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testAppStringResourceMatchesProjectName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nông Sản Bình Mỹ", appName)
    }

    @Test
    fun testOrderPlacement_RejectWhenCutoffPassed() = runBlocking {
        val now = System.currentTimeMillis()
        val expiredSession = PreOrderSessionEntity(
            sessionCode = "EXPIRED-01",
            title = "Phiên Đã Hết Hạn",
            description = "Test cutoff",
            openDateEpochMs = now - 100000,
            closeDateEpochMs = now - 1000, // Past deadline!
            deliveryDateEpochMs = now + 50000,
            status = SessionStatus.OPEN
        )
        val sId = repository.sessionDao.insertSession(expiredSession)

        val product = ProductEntity(
            farmerId = 1L,
            name = "Rau muống nước",
            category = "Rau",
            unit = "kg",
            currentPricePerUnit = 18000.0,
            originalEstimateKg = 50.0,
            reservedKg = 0.0,
            storageInstructions = "Mát",
            originInfo = "Bình Mỹ",
            lotCode = "LOT-01"
        )
        val pId = repository.productDao.insertProduct(product)

        val result = repository.placeOrder(
            buyerId = 1L,
            sessionId = sId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Người mua test",
            receiverPhone = "0900000000",
            buyerNote = null,
            cartItems = listOf(CartItemInput(productId = pId, quantity = 2.0)),
            idempotencyKey = "IDEMP-TEST-001"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("hạn khóa đơn") == true)
    }

    @Test
    fun testOrderPlacement_RejectWhenExceedsAvailableQuota() = runBlocking {
        val now = System.currentTimeMillis()
        val openSession = PreOrderSessionEntity(
            sessionCode = "OPEN-01",
            title = "Phiên Đang Mở",
            description = "Test quota",
            openDateEpochMs = now - 10000,
            closeDateEpochMs = now + 100000, // Still valid
            deliveryDateEpochMs = now + 200000,
            status = SessionStatus.OPEN
        )
        val sId = repository.sessionDao.insertSession(openSession)

        val product = ProductEntity(
            farmerId = 1L,
            name = "Rau muống cạn",
            category = "Rau",
            unit = "kg",
            currentPricePerUnit = 22000.0,
            originalEstimateKg = 10.0, // Only 10kg available
            reservedKg = 8.0,          // 8kg already reserved
            storageInstructions = "Mát",
            originInfo = "Bình Mỹ",
            lotCode = "LOT-02"
        )
        val pId = repository.productDao.insertProduct(product)

        // Requesting 5kg while only 2kg is left
        val result = repository.placeOrder(
            buyerId = 1L,
            sessionId = sId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Người mua test",
            receiverPhone = "0900000000",
            buyerNote = null,
            cartItems = listOf(CartItemInput(productId = pId, quantity = 5.0)),
            idempotencyKey = "IDEMP-TEST-002"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("chỉ còn nhận đặt được") == true)
    }

    @Test
    fun testOrderPlacement_IdempotencyPreventsDuplicateOrders() = runBlocking {
        val now = System.currentTimeMillis()
        val openSession = PreOrderSessionEntity(
            sessionCode = "OPEN-IDEMP",
            title = "Phiên Test Trùng",
            description = "Idempotency",
            openDateEpochMs = now - 10000,
            closeDateEpochMs = now + 100000,
            deliveryDateEpochMs = now + 200000,
            status = SessionStatus.OPEN
        )
        val sId = repository.sessionDao.insertSession(openSession)

        val product = ProductEntity(
            farmerId = 1L,
            name = "Bầu sao",
            category = "Quả",
            unit = "kg",
            currentPricePerUnit = 25000.0,
            originalEstimateKg = 50.0,
            reservedKg = 0.0,
            storageInstructions = "Mát",
            originInfo = "Bình Mỹ",
            lotCode = "LOT-BS"
        )
        val pId = repository.productDao.insertProduct(product)

        val key = "DOUBLE-CLICK-IDEMPOTENCY-KEY"

        val firstCall = repository.placeOrder(
            buyerId = 1L,
            sessionId = sId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Trang",
            receiverPhone = "0903123456",
            buyerNote = null,
            cartItems = listOf(CartItemInput(productId = pId, quantity = 2.0)),
            idempotencyKey = key
        )

        assertTrue(firstCall.isSuccess)
        val firstOrder = firstCall.getOrNull()!!

        // Second click with identical idempotency key
        val secondCall = repository.placeOrder(
            buyerId = 1L,
            sessionId = sId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Trang",
            receiverPhone = "0903123456",
            buyerNote = null,
            cartItems = listOf(CartItemInput(productId = pId, quantity = 2.0)),
            idempotencyKey = key
        )

        assertTrue(secondCall.isSuccess)
        val secondOrder = secondCall.getOrNull()!!

        // Must return the exact same existing order without creating a duplicate!
        assertEquals(firstOrder.id, secondOrder.id)
        assertEquals(firstOrder.orderCode, secondOrder.orderCode)

        // Reserved kg must have incremented only once (2.0 kg, not 4.0 kg)
        val updatedProduct = repository.productDao.getProductByIdSync(pId)!!
        assertEquals(2.0, updatedProduct.reservedKg, 0.01)
    }

    @Test
    fun testLockSession_TransitionsOrdersAndFreezesCancellations() = runBlocking {
        val now = System.currentTimeMillis()
        val session = PreOrderSessionEntity(
            sessionCode = "SESSION-LOCK-TEST",
            title = "Phiên Khóa Đơn",
            description = "Test Lock",
            openDateEpochMs = now - 10000,
            closeDateEpochMs = now + 100000,
            deliveryDateEpochMs = now + 200000,
            status = SessionStatus.OPEN
        )
        val sId = repository.sessionDao.insertSession(session)

        val product = ProductEntity(
            farmerId = 1L,
            name = "Rau muống nước",
            category = "Rau",
            unit = "kg",
            currentPricePerUnit = 18000.0,
            originalEstimateKg = 50.0,
            reservedKg = 0.0,
            storageInstructions = "Mát",
            originInfo = "Bình Mỹ",
            lotCode = "LOT-01"
        )
        val pId = repository.productDao.insertProduct(product)

        val order = repository.placeOrder(
            buyerId = 1L,
            sessionId = sId,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Trang",
            receiverPhone = "0903123456",
            buyerNote = null,
            cartItems = listOf(CartItemInput(productId = pId, quantity = 2.0)),
            idempotencyKey = "IDEMP-LOCK-1"
        ).getOrNull()!!

        assertEquals(OrderStatus.PLACED, order.status)

        // Admin locks session
        repository.lockSession(sId)

        val updatedOrder = repository.orderDao.getOrderByIdSync(order.id)!!
        assertEquals(OrderStatus.LOCKED, updatedOrder.status)

        // Buyer attempting to cancel after lock should be rejected
        val cancelAttempt = repository.cancelOrder(order.id, 1L)
        assertTrue(cancelAttempt.isFailure)
        assertTrue(cancelAttempt.exceptionOrNull()?.message?.contains("đã khóa đơn") == true)
    }

    @Test
    fun testSha256Verification_IntegrityHash() = runBlocking {
        val lot = ProductLotEntity(
            lotCode = "LOT-TEST-SHA",
            productId = 1L,
            farmerId = 1L,
            harvestDateEpochMs = 1700000000000L,
            estimatedYieldKg = 100.0,
            verificationStatus = VerificationStatus.PENDING
        )
        repository.lotDao.insertLot(lot)

        val farmer = FarmerProfileEntity(
            householdName = "Hộ Chú Năm",
            representativeName = "Nguyễn Văn Năm",
            phone = "0918000000",
            addressDetail = "Bình Mỹ",
            acreageHa = 1.2,
            hasVietGap = true,
            vietGapCertCode = "BM-VG-2024-059",
            vietGapValidUntil = "2026",
            isApprovedByAdmin = true,
            bio = "Canh tác sạch"
        )
        repository.farmerDao.insertProfile(farmer)

        val result = repository.verifyLot("LOT-TEST-SHA", "KS. Trần Quốc Đạt", "Đạt chuẩn VietGAP")
        assertTrue(result.isSuccess)

        val verifiedLot = repository.lotDao.getLotByCodeSync("LOT-TEST-SHA")!!
        assertEquals(VerificationStatus.VERIFIED, verifiedLot.verificationStatus)
        assertEquals("KS. Trần Quốc Đạt", verifiedLot.verifierName)
        assertNotNull(verifiedLot.sha256Hash)
        assertEquals(64, verifiedLot.sha256Hash!!.length) // Valid SHA-256 hex string
    }

    @Test
    fun testRecurringSchedule_StatusTransitions() = runBlocking {
        val schedule = RecurringScheduleEntity(
            buyerId = 1L,
            productId = 1L,
            quantity = 2.0,
            frequency = RecurringFrequency.WEEKLY,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Chị Trang",
            receiverPhone = "0903123456",
            status = RecurringStatus.ACTIVE,
            nextDeliveryDateEpochMs = System.currentTimeMillis() + 600000
        )
        val id = repository.recurringDao.insertSchedule(schedule)

        // Pause schedule
        repository.toggleRecurringStatus(id, RecurringStatus.PAUSED)
        var updated = repository.recurringDao.getAllActiveSchedulesSync()
        assertTrue(updated.none { it.id == id })

        // Resume schedule
        repository.toggleRecurringStatus(id, RecurringStatus.ACTIVE)
        updated = repository.recurringDao.getAllActiveSchedulesSync()
        assertTrue(updated.any { it.id == id })
    }

    @Test
    fun testPushNotifications_CoversAllRequiredTypes() = runBlocking {
        val types = listOf(
            com.example.data.model.NotificationType.SESSION_OPENED,
            com.example.data.model.NotificationType.ORDER_CONFIRMED,
            com.example.data.model.NotificationType.ORDER_PREPARING,
            com.example.data.model.NotificationType.ORDER_ARRIVED_POPUP,
            com.example.data.model.NotificationType.SHORTAGE_ALERT,
            com.example.data.model.NotificationType.ADMIN_BROADCAST
        )

        for (type in types) {
            repository.postNotification(
                com.example.data.local.entity.AppNotificationEntity(
                    userId = 1L,
                    title = "Test ${type.name}",
                    message = "Nội dung cho ${type.labelVi}",
                    type = type
                )
            )
        }

        val notifications = repository.notificationDao.getAllNotifications()
        val list = notifications.first()
        assertEquals(6, list.size)
        for (type in types) {
            assertTrue("Phải chứa loại thông báo $type", list.any { it.type == type })
        }
    }

    @Test
    fun testPushNotifications_RoleTargeting() = runBlocking {
        // 1. Notification targeted only to FARMER
        repository.postNotification(
            com.example.data.local.entity.AppNotificationEntity(
                userId = 0L,
                targetRole = UserRole.FARMER,
                title = "Thông báo nông hộ",
                message = "Chốt số kg",
                type = com.example.data.model.NotificationType.ORDER_CONFIRMED
            )
        )

        // 2. Notification targeted only to POPUP_STAFF
        repository.postNotification(
            com.example.data.local.entity.AppNotificationEntity(
                userId = 0L,
                targetRole = UserRole.POPUP_STAFF,
                title = "Hàng đến trạm",
                message = "Chuẩn bị kho mát",
                type = com.example.data.model.NotificationType.ORDER_ARRIVED_POPUP
            )
        )

        // 3. Broadcast to all (targetRole = null, userId = 0)
        repository.postNotification(
            com.example.data.local.entity.AppNotificationEntity(
                userId = 0L,
                targetRole = null,
                title = "Phiên mới mở",
                message = "Đặt rau sạch",
                type = com.example.data.model.NotificationType.SESSION_OPENED
            )
        )

        // Check farmer inbox: should have 2 notifications (Farmer specific + Broadcast)
        val farmerNotifs = repository.notificationDao.getNotificationsForUser(userId = 2L, userRole = UserRole.FARMER).first()
        assertEquals(2, farmerNotifs.size)
        assertTrue(farmerNotifs.any { it.title == "Thông báo nông hộ" })
        assertTrue(farmerNotifs.any { it.title == "Phiên mới mở" })

        // Check popup staff inbox: should have 2 notifications (Staff specific + Broadcast)
        val staffNotifs = repository.notificationDao.getNotificationsForUser(userId = 4L, userRole = UserRole.POPUP_STAFF).first()
        assertEquals(2, staffNotifs.size)
        assertTrue(staffNotifs.any { it.title == "Hàng đến trạm" })
        assertTrue(staffNotifs.any { it.title == "Phiên mới mở" })
    }
}
