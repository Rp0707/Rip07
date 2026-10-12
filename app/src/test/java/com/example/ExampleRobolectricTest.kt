package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nông Sản Bình Mỹ", appName)
  }

  @Test
  fun `launch MainActivity without crash`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `verify core MVP room entities User Product Session Order`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    // 1. User Entity test
    val userId = db.userDao().insertUser(
        com.example.data.local.entity.UserEntity(
            username = "test_buyer",
            fullName = "Nguyễn Văn Test",
            phone = "0901234567",
            email = "test@example.com",
            role = com.example.data.model.UserRole.BUYER
        )
    )
    val user = db.userDao().getUserByIdSync(userId)
    assertNotNull(user)
    assertEquals("test_buyer", user?.username)

    // 2. Session Entity test
    val sessionId = db.preOrderSessionDao().insertSession(
        com.example.data.local.entity.PreOrderSessionEntity(
            sessionCode = "SES-TEST",
            title = "Phiên Thu Hoạch Test",
            description = "Mô tả phiên",
            openDateEpochMs = System.currentTimeMillis(),
            closeDateEpochMs = System.currentTimeMillis() + 86400000,
            deliveryDateEpochMs = System.currentTimeMillis() + 172800000,
            status = com.example.data.model.SessionStatus.OPEN
        )
    )
    val session = db.preOrderSessionDao().getSessionByIdSync(sessionId)
    assertNotNull(session)
    assertEquals("SES-TEST", session?.sessionCode)

    // 3. Product Entity test
    val productId = db.productDao().insertProduct(
        com.example.data.local.entity.ProductEntity(
            farmerId = 1L,
            name = "Rau muống nước Bình Mỹ VietGAP",
            category = "Rau ăn lá",
            unit = "kg",
            currentPricePerUnit = 18000.0,
            originalEstimateKg = 150.0,
            reservedKg = 0.0,
            storageInstructions = "Bảo quản ngăn mát 5-10 độ C",
            originInfo = "Tổ 3, Xã Bình Mỹ",
            lotCode = "LOT-RM-01"
        )
    )
    val product = db.productDao().getProductByIdSync(productId)
    assertNotNull(product)
    assertEquals(18000.0, product?.currentPricePerUnit)

    // 4. Order & OrderItem Entities test
    val orderId = db.orderDao().insertOrder(
        com.example.data.local.entity.OrderEntity(
            orderCode = "ORD-TEST-001",
            buyerId = userId,
            sessionId = sessionId,
            deliveryType = com.example.data.model.DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Nguyễn Văn Test",
            receiverPhone = "0901234567",
            status = com.example.data.model.OrderStatus.PLACED,
            totalAmount = 36000.0,
            pickupCode = "PK-1234",
            idempotencyKey = "KEY-TEST-001"
        )
    )
    db.orderItemDao().insertOrderItems(
        listOf(
            com.example.data.local.entity.OrderItemEntity(
                orderId = orderId,
                productId = productId,
                productName = product!!.name,
                farmerId = 1L,
                farmerName = "Hộ Chú Năm",
                unit = "kg",
                quantity = 2.0,
                unitPriceSnapshot = 18000.0,
                lineTotal = 36000.0
            )
        )
    )

    val order = db.orderDao().getOrderByIdSync(orderId)
    val items = db.orderItemDao().getItemsForOrderSync(orderId)
    assertNotNull(order)
    assertEquals(1, items.size)
    assertEquals(36000.0, items[0].lineTotal, 0.01)

    db.close()
  }

}
