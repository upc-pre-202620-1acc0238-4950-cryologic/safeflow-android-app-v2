package com.safeflow.app.features.iam.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.*
import com.safeflow.app.features.iam.domain.*
import javax.inject.Inject
import javax.inject.Singleton

private fun List<String>?.permissions() = orEmpty().map { Permission.valueOf(it) }.toSet()
@Singleton
class RemoteIdentityRepository @Inject constructor(private val api: IamApi, private val call: RemoteCall) : IdentityRepository {
    override suspend fun register(email: String, password: String, terms: Boolean) { call { api.signUp(SignUpDto(email, password, terms)) } }
    override suspend fun login(email: String, password: String): Session = call {
        api.signIn(SignInDto(email, password)).let {
            Session(Account(it.id.toString(), it.email.orEmpty(), Role.valueOf(it.role)), it.expiresAt.epochMillis(), it.token, it.permissions.permissions())
        }
    }
    override suspend fun me() = call { api.me().let { Account(it.id.toString(), it.email.orEmpty(), Role.valueOf(it.role)) to it.permissions.permissions() } }
    override suspend fun recover(email: String) { call { api.recover(RecoveryDto(email)) } }
    override suspend fun logout() { call { api.signOut() } }
}
@Singleton
class RemotePermissionsRepository @Inject constructor(private val api: IamApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<RolePermissions>(sessions, { it.role.name }), PermissionsRepository {
    override suspend fun refresh() = load { call { api.permissions().map { RolePermissions(Role.valueOf(it.role), it.granted.orEmpty().map(Permission::valueOf).toSet()) } } }
    override suspend fun replace(role: Role, granted: Set<Permission>) { remember { call { api.permissions(role.name, ReplacePermissionsDto(granted.map { it.name }.toSet())).let { RolePermissions(Role.valueOf(it.role), it.granted.orEmpty().map(Permission::valueOf).toSet()) } } } }
}
