package com.example.security

import com.example.data.local.User
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.model.UserRole

/**
 * Enterprise RBAC Authorization & Data Scope Engine for "Sàn Nông Sản Bình Mỹ".
 * Enforces action permissions and row-level data scoping at the backend/data layer.
 */
object SecurityPolicyEngine {

    // Defined Permission Keys
    object Permissions {
        const val PRODUCT_VIEW = "product.view"
        const val PRODUCT_EDIT_OWN = "product.edit_own"
        const val PRODUCT_MANAGE_ALL = "product.manage_all"

        const val ORDER_VIEW_OWN = "order.view_own"
        const val ORDER_VIEW_STATION = "order.view_station"
        const val ORDER_VIEW_ALL = "order.view_all"

        const val FARMER_VIEW_OWN_REVENUE = "farmer.view_own_revenue"
        const val FARMER_APPROVE = "farmer.approve"

        const val PRICE_SET_POLICY = "price.set_policy"
        const val PRICE_EDIT_WITHIN_BOUNDS = "price.edit_within_bounds"

        const val STATION_REPORT_INCIDENT = "station.report_incident"
        const val REPORT_VIEW_SYSTEM_TOTAL = "report.view_system_total"

        const val ACCOUNT_MANAGE_HTX = "account.manage_htx"
        const val ACCOUNT_MANAGE_ALL = "account.manage_all"

        const val SYSTEM_CONFIG_TECH = "system.config_tech"
        const val AUDIT_VIEW_LOGS = "audit.view_logs"
    }

    // Static Role-Permission Mapping (Loaded into DB via RbacSeedData for runtime flexibility)
    val ROLE_PERMISSIONS_MAP = mapOf(
        UserRole.BUYER to setOf(
            Permissions.PRODUCT_VIEW,
            Permissions.ORDER_VIEW_OWN
        ),
        UserRole.FARMER to setOf(
            Permissions.PRODUCT_VIEW,
            Permissions.PRODUCT_EDIT_OWN,
            Permissions.ORDER_VIEW_OWN,
            Permissions.FARMER_VIEW_OWN_REVENUE,
            Permissions.PRICE_EDIT_WITHIN_BOUNDS
        ),
        UserRole.POPUP_STAFF to setOf(
            Permissions.PRODUCT_VIEW,
            Permissions.ORDER_VIEW_STATION,
            Permissions.STATION_REPORT_INCIDENT
        ),
        UserRole.ADMIN to setOf(
            Permissions.PRODUCT_VIEW,
            Permissions.PRODUCT_EDIT_OWN,
            Permissions.PRODUCT_MANAGE_ALL,
            Permissions.ORDER_VIEW_ALL,
            Permissions.FARMER_APPROVE,
            Permissions.PRICE_SET_POLICY,
            Permissions.REPORT_VIEW_SYSTEM_TOTAL,
            Permissions.ACCOUNT_MANAGE_HTX
        ),
        UserRole.SUPER_ADMIN to setOf(
            Permissions.PRODUCT_VIEW,
            Permissions.PRODUCT_EDIT_OWN,
            Permissions.PRODUCT_MANAGE_ALL,
            Permissions.ORDER_VIEW_ALL,
            Permissions.FARMER_APPROVE,
            Permissions.PRICE_SET_POLICY,
            Permissions.REPORT_VIEW_SYSTEM_TOTAL,
            Permissions.ACCOUNT_MANAGE_HTX,
            Permissions.ACCOUNT_MANAGE_ALL,
            Permissions.SYSTEM_CONFIG_TECH,
            Permissions.AUDIT_VIEW_LOGS
        )
    )

    /**
     * Checks if a user role possesses a specific permission key.
     */
    fun hasPermission(user: User?, permissionKey: String): Boolean {
        if (user == null) return false
        val userPermissions = ROLE_PERMISSIONS_MAP[user.role] ?: emptySet()
        return userPermissions.contains(permissionKey)
    }

    /**
     * DATA SCOPE FILTER 1: Orders
     * Backend filter restricting orders based on user's role and data scope.
     */
    fun filterOrdersByScope(
        user: User?,
        allOrders: List<OrderEntity>,
        allOrderItems: List<OrderItemEntity> = emptyList()
    ): List<OrderEntity> {
        if (user == null) return emptyList()

        return when (user.role) {
            UserRole.BUYER -> {
                // Customer only sees orders placed by themselves
                allOrders.filter { it.buyerId == user.id }
            }
            UserRole.FARMER -> {
                // Farmer only sees orders containing items from their farmer profile ID
                val farmerProfileId = user.farmerProfileId ?: return emptyList()
                val farmerOrderIds = allOrderItems
                    .filter { item -> item.farmerId == farmerProfileId }
                    .map { item -> item.orderId }
                    .toSet()
                allOrders.filter { order -> farmerOrderIds.contains(order.id) }
            }
            UserRole.POPUP_STAFF -> {
                // Station Manager only sees orders routed to their assigned Station ID
                val stationId = user.assignedPopupLocationId ?: return emptyList()
                allOrders.filter { it.popupLocationId == stationId }
            }
            UserRole.ADMIN, UserRole.SUPER_ADMIN -> {
                // HTX Admin & Super Admin see all orders across all stations
                allOrders
            }
            else -> emptyList()
        }
    }

    /**
     * DATA SCOPE FILTER 2: Products Editing
     */
    fun canEditProduct(user: User?, product: ProductEntity, proposedPrice: Double? = null, priceMin: Double = 0.0, priceMax: Double = 1_000_000.0): Boolean {
        if (user == null) return false

        return when (user.role) {
            UserRole.SUPER_ADMIN, UserRole.ADMIN -> true
            UserRole.FARMER -> {
                val isOwnProduct = product.farmerId == user.farmerProfileId
                if (!isOwnProduct) return false

                // If proposing a new price, check if within HTX price boundary guidelines (price ceiling/floor)
                if (proposedPrice != null) {
                    proposedPrice in priceMin..priceMax
                } else {
                    true
                }
            }
            else -> false
        }
    }

    /**
     * DATA SCOPE FILTER 3: Revenue Visibility
     */
    fun canViewFarmerRevenue(user: User?, targetFarmerProfileId: Long): Boolean {
        if (user == null) return false

        return when (user.role) {
            UserRole.SUPER_ADMIN, UserRole.ADMIN -> true
            UserRole.FARMER -> user.farmerProfileId == targetFarmerProfileId
            else -> false
        }
    }

    /**
     * DATA SCOPE FILTER 4: User Account Management Hierarchy
     */
    fun canManageUserAccount(actor: User?, targetUserRole: UserRole): Boolean {
        if (actor == null) return false

        return when (actor.role) {
            UserRole.SUPER_ADMIN -> true // Can manage all accounts including Admin
            UserRole.ADMIN -> {
                // HTX Admin can manage Farmers, Station Managers, and Customers, but CANNOT create/edit Super Admins
                targetUserRole != UserRole.SUPER_ADMIN
            }
            else -> false
        }
    }

    /**
     * DATA SCOPE FILTER 5: System Configuration & Technical Audit Access
     */
    fun canAccessTechnicalSettings(actor: User?): Boolean {
        return actor?.role == UserRole.SUPER_ADMIN
    }
}
