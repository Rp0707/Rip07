package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PermissionEntity
import com.example.data.local.entity.RoleEntity
import com.example.data.local.entity.RolePermissionCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface RbacDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<PermissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolePermissions(crossRefs: List<RolePermissionCrossRef>)

    @Query("SELECT * FROM roles")
    fun getAllRoles(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM permissions")
    fun getAllPermissions(): Flow<List<PermissionEntity>>

    @Query("""
        SELECT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.permissionKey = rp.permissionKey
        WHERE rp.roleId = :roleId
    """)
    suspend fun getPermissionsForRoleSync(roleId: String): List<PermissionEntity>

    @Query("""
        SELECT p.permissionKey FROM permissions p
        INNER JOIN role_permissions rp ON p.permissionKey = rp.permissionKey
        WHERE rp.roleId = :roleId
    """)
    suspend fun getPermissionKeysForRoleSync(roleId: String): List<String>

    @Query("SELECT COUNT(*) FROM roles")
    suspend fun getRoleCount(): Int
}
