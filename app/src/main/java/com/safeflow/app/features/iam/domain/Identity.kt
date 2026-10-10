package com.safeflow.app.features.iam.domain

import com.safeflow.app.core.domain.Records
import kotlinx.coroutines.flow.Flow

enum class Role { OPERADOR, SUPERVISOR, ADMIN }
enum class Permission(val label: String) {
    WRITE_INVENTORY("Registrar productos, lotes e ingresos"),
    READ_INVENTORY("Consultar inventario y temperaturas"),
    READ_SHIPMENTS("Consultar y validar despachos"),
    CREATE_SHIPMENT("Registrar despachos"),
    DELIVER("Confirmar entregas"),
    SUMMARY("Consultar resumen operativo"),
    RISKS("Consultar riesgos e historial térmico"),
    ALERTS("Recibir y consultar alertas"),
    RESOLVE("Registrar acciones correctivas")
}
data class Account(val id: String, val email: String, val role: Role)
data class RolePermissions(val role: Role, val granted: Set<Permission>)
data class Session(val account: Account, val expiresAt: Long, val token: String, val permissions: Set<Permission>)
interface IdentityRepository {
    suspend fun register(email: String, password: String, terms: Boolean)
    suspend fun login(email: String, password: String): Session
    suspend fun me(): Pair<Account, Set<Permission>>
    suspend fun recover(email: String)
    suspend fun logout()
}
interface PermissionsRepository : Records<RolePermissions> {
    suspend fun replace(role: Role, granted: Set<Permission>)
}
interface SessionRepository {
    val sessions: Flow<Session?>
    suspend fun current(): Session?
    suspend fun save(session: Session?)
    suspend fun clearIfToken(token: String)
}
interface Access {
    suspend fun account(): Account?
    suspend fun permissions(): Set<Permission>
    suspend fun require(permission: Permission): Account
    suspend fun requireAdmin(): Account
}
object IdentityRules {
    fun validEmail(email: String) = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
    fun validPassword(value: String) = value.length >= 8 && value.any(Char::isLetter) &&
        value.any(Char::isDigit) && value.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    fun defaults(role: Role): Set<Permission> = when (role) {
        Role.OPERADOR -> setOf(Permission.WRITE_INVENTORY, Permission.READ_INVENTORY,
            Permission.READ_SHIPMENTS, Permission.CREATE_SHIPMENT, Permission.DELIVER)
        Role.SUPERVISOR -> setOf(Permission.SUMMARY, Permission.RISKS, Permission.ALERTS, Permission.RESOLVE)
        Role.ADMIN -> emptySet()
    }
}
