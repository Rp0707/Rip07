package com.example.security

import com.example.data.local.User
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.DeliveryType
import com.example.data.model.UserRole

data class RbacTestCaseResult(
    val testName: String,
    val role: UserRole,
    val isPassed: Boolean,
    val detail: String
)

/**
 * Automated Verification Test Suite for the 5-Role RBAC & Data Scope Matrix.
 */
object RbacTestVerifier {

    fun runAllRbacTests(): List<RbacTestCaseResult> {
        val results = mutableListOf<RbacTestCaseResult>()

        val customer = User(id = 1, username = "khachhang_demo", fullName = "Khách Hàng", phone = "090", email = "k@a.com", role = UserRole.BUYER)
        val farmer = User(id = 2, username = "nongho_cunam", fullName = "Chú Năm", phone = "091", email = "f@a.com", role = UserRole.FARMER, farmerProfileId = 101L)
        val stationMgr = User(id = 3, username = "tram_4a", fullName = "Anh Tuấn", phone = "092", email = "s@a.com", role = UserRole.POPUP_STAFF, assignedPopupLocationId = 1L)
        val htxAdmin = User(id = 4, username = "quantri_htx", fullName = "BDI HTX", phone = "093", email = "a@a.com", role = UserRole.ADMIN)
        val superAdmin = User(id = 5, username = "superadmin_system", fullName = "Super Admin", phone = "099", email = "sa@a.com", role = UserRole.SUPER_ADMIN)

        val pFarmer1 = ProductEntity(
            id = 10,
            farmerId = 101L,
            name = "Rau Muống",
            category = "Rau ăn lá",
            unit = "Kg",
            currentPricePerUnit = 25000.0,
            originalEstimateKg = 50.0,
            storageInstructions = "Bảo quản mát",
            originInfo = "Xã Bình Mỹ",
            lotCode = "LOT-101"
        )
        val pFarmer2 = ProductEntity(
            id = 20,
            farmerId = 202L,
            name = "Bầu Sao",
            category = "Rau ăn quả",
            unit = "Kg",
            currentPricePerUnit = 30000.0,
            originalEstimateKg = 30.0,
            storageInstructions = "Bảo quản mát",
            originInfo = "Xã Bình Mỹ",
            lotCode = "LOT-202"
        )

        val order1 = OrderEntity(
            id = 100,
            orderCode = "BM-100",
            buyerId = 1,
            sessionId = 1L,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 1L,
            homeAddress = null,
            receiverName = "Mai",
            receiverPhone = "0901234567",
            totalAmount = 50000.0,
            pickupCode = "PU-100",
            idempotencyKey = "KEY-100"
        )
        val order2 = OrderEntity(
            id = 200,
            orderCode = "BM-200",
            buyerId = 99,
            sessionId = 1L,
            deliveryType = DeliveryType.POPUP_PICKUP,
            popupLocationId = 2L,
            homeAddress = null,
            receiverName = "Khác",
            receiverPhone = "0909999999",
            totalAmount = 70000.0,
            pickupCode = "PU-200",
            idempotencyKey = "KEY-200"
        )

        val orderItems = listOf(
            OrderItemEntity(
                id = 1,
                orderId = 100,
                productId = 10,
                productName = "Rau Muống",
                farmerId = 101L,
                farmerName = "Chú Năm",
                unit = "Kg",
                quantity = 2.0,
                unitPriceSnapshot = 25000.0,
                lineTotal = 50000.0
            ),
            OrderItemEntity(
                id = 2,
                orderId = 200,
                productId = 20,
                productName = "Bầu Sao",
                farmerId = 202L,
                farmerName = "Hộ Khác",
                unit = "Kg",
                quantity = 2.0,
                unitPriceSnapshot = 35000.0,
                lineTotal = 70000.0
            )
        )

        // TEST 1: Customer Scope Check
        val custOrders = SecurityPolicyEngine.filterOrdersByScope(customer, listOf(order1, order2), orderItems)
        val custCanViewReport = SecurityPolicyEngine.hasPermission(customer, SecurityPolicyEngine.Permissions.REPORT_VIEW_SYSTEM_TOTAL)
        results.add(
            RbacTestCaseResult(
                testName = "Khách hàng: Chỉ xem đơn của mình & Không xem báo cáo tổng",
                role = UserRole.BUYER,
                isPassed = custOrders.size == 1 && custOrders.first().id == 100L && !custCanViewReport,
                detail = "Khách hàng chỉ lọc ra đơn id=100 và bị chặn truy cập báo cáo tài chính."
            )
        )

        // TEST 2: Farmer Scope Check
        val farmerCanEditOwn = SecurityPolicyEngine.canEditProduct(farmer, pFarmer1)
        val farmerCanEditOther = SecurityPolicyEngine.canEditProduct(farmer, pFarmer2)
        val farmerCanViewOtherRev = SecurityPolicyEngine.canViewFarmerRevenue(farmer, 202L)
        results.add(
            RbacTestCaseResult(
                testName = "Nông hộ: Chỉ sửa sản phẩm của mình & Không xem doanh thu nông hộ khác",
                role = UserRole.FARMER,
                isPassed = farmerCanEditOwn && !farmerCanEditOther && !farmerCanViewOtherRev,
                detail = "Chú Năm (farmerId=101) chỉ sửa được sản phẩm p10, bị chặn sửa p20 và xem doanh thu farmerId=202."
            )
        )

        // TEST 3: Station Manager Scope Check
        val stationOrders = SecurityPolicyEngine.filterOrdersByScope(stationMgr, listOf(order1, order2), orderItems)
        val stationCanSetPrice = SecurityPolicyEngine.hasPermission(stationMgr, SecurityPolicyEngine.Permissions.PRICE_SET_POLICY)
        results.add(
            RbacTestCaseResult(
                testName = "Trạm 4A: Chỉ xem đơn thuộc trạm 1 & Không có quyền thiết lập giá",
                role = UserRole.POPUP_STAFF,
                isPassed = stationOrders.size == 1 && stationOrders.first().popupLocationId == 1L && !stationCanSetPrice,
                detail = "Trạm 4A chỉ nhận đơn định tuyến về popupLocationId=1, bị chặn thiết lập chính sách giá."
            )
        )

        // TEST 4: HTX Admin Scope Check
        val adminCanManageFarmer = SecurityPolicyEngine.canManageUserAccount(htxAdmin, UserRole.FARMER)
        val adminCanManageSuperAdmin = SecurityPolicyEngine.canManageUserAccount(htxAdmin, UserRole.SUPER_ADMIN)
        val adminCanTechConfig = SecurityPolicyEngine.canAccessTechnicalSettings(htxAdmin)
        results.add(
            RbacTestCaseResult(
                testName = "Quản trị HTX: Quản lý Nông hộ/Trạm nhưng KHÔNG thể sửa Super Admin/Config gốc",
                role = UserRole.ADMIN,
                isPassed = adminCanManageFarmer && !adminCanManageSuperAdmin && !adminCanTechConfig,
                detail = "Quản trị viên HTX duyệt nông hộ/trạm bình thường, nhưng bị chặn sửa tài khoản Super Admin."
            )
        )

        // TEST 5: Super Admin Override & Full Access Check
        val superCanTechConfig = SecurityPolicyEngine.canAccessTechnicalSettings(superAdmin)
        val superCanManageAdmin = SecurityPolicyEngine.canManageUserAccount(superAdmin, UserRole.ADMIN)
        val superCanViewAllOrders = SecurityPolicyEngine.filterOrdersByScope(superAdmin, listOf(order1, order2), orderItems).size == 2
        results.add(
            RbacTestCaseResult(
                testName = "Siêu Quản Trị: Toàn quyền cấu hình kỹ thuật, override và quản lý mọi cấp vai trò",
                role = UserRole.SUPER_ADMIN,
                isPassed = superCanTechConfig && superCanManageAdmin && superCanViewAllOrders,
                detail = "Super Admin sở hữu toàn quyền kỹ thuật và xem được toàn bộ dữ liệu 2 đơn hàng."
            )
        )

        return results
    }
}
