package com.safeflow.app.features.iam.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.safeflow.app.features.iam.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSession @Inject constructor(private val store: DataStore<Preferences>) : SessionRepository {
    private val account = stringPreferencesKey("account")
    private val email = stringPreferencesKey("email")
    private val role = stringPreferencesKey("role")
    private val permissions = stringSetPreferencesKey("permissions")
    private val token = stringPreferencesKey("token")
    private val expires = longPreferencesKey("expires")
    override val sessions = store.data.map { p ->
        val id = p[account]; val bearer = p[token]; val userRole = Role.entries.find { it.name == p[role] }
        if (id == null || bearer.isNullOrBlank() || userRole == null) null
        else Session(Account(id, p[email].orEmpty(), userRole), p[expires] ?: 0, bearer,
            p[permissions].orEmpty().mapNotNull { name -> Permission.entries.find { it.name == name } }.toSet())
    }
    override suspend fun current() = sessions.first()
    override suspend fun save(session: Session?) {
        store.edit { p ->
            if (session == null) p.clear()
            else {
                p[account] = session.account.id; p[email] = session.account.email; p[role] = session.account.role.name
                p[token] = session.token; p[expires] = session.expiresAt; p[permissions] = session.permissions.map { it.name }.toSet()
            }
        }
    }
    override suspend fun clearIfToken(token: String) { store.edit { if (it[this.token] == token) it.clear() } }
}
