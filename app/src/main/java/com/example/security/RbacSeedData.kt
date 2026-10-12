package com.example.security

import com.example.data.local.AppDatabase
import com.example.data.local.User
import com.example.data.local.entity.PermissionEntity
import com.example.data.local.entity.RoleEntity
import com.example.data.local.entity.RolePermissionCrossRef
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bootstraps standard RBAC roles, permission keys, cross-references, and 5 sample test accounts.
 */
object RbacSeedData {

    suspend fun seedRbacSystem(db: AppDatabase) = withContext(Dispatchers.IO) {
        val rbacDao = db.rbacDao()
        val userDao = db.userDao()

        // 1. Seed 5 Roles
        val roles = listOf(
            RoleEntity(
                roleId = UserRole.BUYER.name,
                roleName = "Khách hàng (Customer)",
                description = "Người mua nông sản Bình Mỹ, theo dõi đơn hàng cá nhân"
            ),
            RoleEntity(
                roleId = UserRole.FARMER.name,
                roleName = "Nông hộ (Farmer)",
                description = "Trực tiếp canh tác, đăng bán sản phẩm và theo dõi đơn nông sản"
            ),
            RoleEntity(
                roleId = UserRole.POPUP_STAFF.name,
                roleName = "Trạm phân phối (Station Manager)",
                description = "Vận hành điểm trung chuyển/trạm giao nhận khu vực"
            ),
            RoleEntity(
                roleId = UserRole.ADMIN.name,
                roleName = "Quản trị viên HTX (Admin/Coordinator)",
                description = "Ban Điều Phối HTX Bình Mỹ, quản lý hoạt động toàn hợp tác xã"
            ),
            RoleEntity(
                roleId = UserRole.SUPER_ADMIN.name,
                roleName = "Siêu quản trị hệ thống (Super Admin)",
                description = "Toàn quyền kỹ thuật, phân quyền gốc và cấu hình hệ thống"
            )
        )
        rbacDao.insertRoles(roles)

        // 2. Seed Granular Permissions
        val permissions = listOf(
            PermissionEntity(SecurityPolicyEngine.Permissions.PRODUCT_VIEW, "PRODUCTS", "Xem danh sách và chi tiết sản phẩm"),
            PermissionEntity(SecurityPolicyEngine.Permissions.PRODUCT_EDIT_OWN, "PRODUCTS", "Đăng/sửa/xóa sản phẩm do chính mình cung cấp"),
            PermissionEntity(SecurityPolicyEngine.Permissions.PRODUCT_MANAGE_ALL, "PRODUCTS", "Duyệt và quản lý toàn bộ sản phẩm trên sàn"),

            PermissionEntity(SecurityPolicyEngine.Permissions.ORDER_VIEW_OWN, "ORDERS", "Xem danh sách đơn hàng cá nhân hoặc đơn có sản phẩm của mình"),
            PermissionEntity(SecurityPolicyEngine.Permissions.ORDER_VIEW_STATION, "ORDERS", "Xem danh sách đơn hàng thuộc trạm khu vực phụ trách"),
            PermissionEntity(SecurityPolicyEngine.Permissions.ORDER_VIEW_ALL, "ORDERS", "Xem tất cả đơn hàng trên toàn hệ thống"),

            PermissionEntity(SecurityPolicyEngine.Permissions.FARMER_VIEW_OWN_REVENUE, "FARMERS", "Xem doanh thu và lịch sử bán hàng cá nhân"),
            PermissionEntity(SecurityPolicyEngine.Permissions.FARMER_APPROVE, "FARMERS", "Phê duyệt nông hộ mới tham gia HTX"),

            PermissionEntity(SecurityPolicyEngine.Permissions.PRICE_SET_POLICY, "PRICING", "Thiết lập khung giá sàn/trần và chính sách chiết khấu"),
            PermissionEntity(SecurityPolicyEngine.Permissions.PRICE_EDIT_WITHIN_BOUNDS, "PRICING", "Cập nhật giá sản phẩm trong khung do HTX quy định"),

            PermissionEntity(SecurityPolicyEngine.Permissions.STATION_REPORT_INCIDENT, "STATION", "Gửi báo cáo sự cố trạm phân phối"),
            PermissionEntity(SecurityPolicyEngine.Permissions.REPORT_VIEW_SYSTEM_TOTAL, "REPORTS", "Xem báo cáo doanh thu và sản lượng tổng"),

            PermissionEntity(SecurityPolicyEngine.Permissions.ACCOUNT_MANAGE_HTX, "ACCOUNTS", "Quản lý tài khoản Nông hộ, Trạm và Khách hàng"),
            PermissionEntity(SecurityPolicyEngine.Permissions.ACCOUNT_MANAGE_ALL, "ACCOUNTS", "Toàn quyền quản lý mọi loại tài khoản kể cả Admin"),

            PermissionEntity(SecurityPolicyEngine.Permissions.SYSTEM_CONFIG_TECH, "SYSTEM", "Cấu hình kỹ thuật, database và phân quyền gốc"),
            PermissionEntity(SecurityPolicyEngine.Permissions.AUDIT_VIEW_LOGS, "SYSTEM", "Truy cập nhật ký giám sát hệ thống (Audit Trail)")
        )
        rbacDao.insertPermissions(permissions)

        // 3. Seed Role-Permission Mappings
        val crossRefs = mutableListOf<RolePermissionCrossRef>()
        SecurityPolicyEngine.ROLE_PERMISSIONS_MAP.forEach { (role, permSet) ->
            permSet.forEach { permKey ->
                crossRefs.add(RolePermissionCrossRef(role.name, permKey))
            }
        }
        rbacDao.insertRolePermissions(crossRefs)

        // 4. Seed 5 Sample Users for RBAC Testing
        val testUsers = listOf(
            User(
                username = "khachhang_demo",
                fullName = "Chị Nguyễn Thị Mai",
                phone = "0901234567",
                email = "mai.nguyen@example.com",
                role = UserRole.BUYER,
                avatarEmoji = "👩"
            ),
            User(
                username = "nongho_cunam",
                fullName = "Chú Nguyễn Văn Năm",
                phone = "0912345678",
                email = "chunam.binhmy@example.com",
                role = UserRole.FARMER,
                farmerProfileId = 101L,
                avatarEmoji = "👨‍🌾"
            ),
            User(
                username = "tram_4a",
                fullName = "Anh Nguyễn Văn Tuấn (Trạm 4A)",
                phone = "0923456789",
                email = "tuan.tram4a@example.com",
                role = UserRole.POPUP_STAFF,
                assignedPopupLocationId = 1L,
                avatarEmoji = "🏢"
            ),
            User(
                username = "quantri_htx",
                fullName = "Ban Điều Phối HTX Bình Mỹ",
                phone = "0934567890",
                email = "dieuphoi@htxbinhmy.vn",
                role = UserRole.ADMIN,
                avatarEmoji = "🏛️"
            ),
            User(
                username = "superadmin_system",
                fullName = "Siêu Quản Trị Hệ Thống",
                phone = "0999999999",
                email = "sysadmin@binhmy.vn",
                role = UserRole.SUPER_ADMIN,
                avatarEmoji = "⚡"
            )
        )

        for (u in testUsers) {
            val existing = userDao.getUserByUsername(u.username)
            if (existing == null) {
                userDao.insertUser(u)
            }
        }
    }
}
