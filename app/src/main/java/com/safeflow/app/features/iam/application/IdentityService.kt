package com.safeflow.app.features.iam.application

import com.safeflow.app.core.application.refreshAfterMutation
import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityService @Inject constructor(private val identities: IdentityRepository, val roles: PermissionsRepository,
    val sessions: SessionRepository, private val clock: Clock) : Access {
    suspend fun initialize() { if (account() != null) refresh() }
    suspend fun refresh() {
        val previous = sessions.current() ?: return
        if (account() == null) return
        val (account, permissions) = identities.me()
        if (sessions.current()?.token == previous.token) sessions.save(previous.copy(account = account, permissions = permissions))
    }
    suspend fun register(email: String, password: String, terms: Boolean) { identities.register(email.trim().lowercase(), password, terms) }
    suspend fun login(email: String, password: String) { sessions.save(identities.login(email.trim().lowercase(), password)) }
    override suspend fun account(): Account? {
        val session = sessions.current() ?: return null
        if (session.expiresAt <= clock.now()) { sessions.clearIfToken(session.token); return null }
        return session.account
    }
    override suspend fun permissions(): Set<Permission> = if (account() == null) emptySet() else sessions.current()?.permissions.orEmpty()
    override suspend fun require(permission: Permission): Account {
        val account = account() ?: throw BusinessException("Tu sesión terminó. Inicia sesión nuevamente.")
        ensure(permission in permissions(), "Tu rol no tiene permiso para realizar esta operación.")
        return account
    }
    override suspend fun requireAdmin(): Account {
        val account = account() ?: throw BusinessException("Inicia sesión nuevamente.")
        ensure(account.role == Role.ADMIN, "Solo ADMIN puede administrar permisos.")
        return account
    }
    suspend fun update(role: Role, granted: Set<Permission>) {
        requireAdmin()
        ensure(role != Role.ADMIN, "La capacidad administrativa de ADMIN no se puede modificar ni delegar.")
        roles.replace(role, granted)
        refreshAfterMutation { roles.refresh(); refresh() }
    }
    suspend fun recover(email: String) { identities.recover(email.trim().lowercase()) }
    suspend fun logout() {
        val token = sessions.current()?.token ?: return
        try { identities.logout() }
        finally { withContext(NonCancellable) { sessions.clearIfToken(token) } }
    }
}
