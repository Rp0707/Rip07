package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RBAC Entity: Roles
 */
@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val roleId: String, // e.g. "ROLE_CUSTOMER", "ROLE_FARMER", "ROLE_STATION_MANAGER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"
    val roleName: String,           // Human readable name (e.g. "Quản trị viên HTX")
    val description: String,        // Responsibility scope
    val isSystemRole: Boolean = true
)

/**
 * RBAC Entity: Permissions
 */
@Entity(tableName = "permissions")
data class PermissionEntity(
    @PrimaryKey val permissionKey: String, // e.g. "order.view_own", "product.edit_own", "price.set_policy"
    val permissionGroup: String,           // e.g. "ORDERS", "PRODUCTS", "PRICING", "ACCOUNTS", "SYSTEM"
    val description: String
)

/**
 * RBAC Entity: Role-Permission Cross-Reference
 */
@Entity(
    tableName = "role_permissions",
    primaryKeys = ["roleId", "permissionKey"],
    indices = [Index(value = ["permissionKey"])]
)
data class RolePermissionCrossRef(
    val roleId: String,
    val permissionKey: String
)
