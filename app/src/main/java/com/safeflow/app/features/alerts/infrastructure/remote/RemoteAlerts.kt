package com.safeflow.app.features.alerts.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.iam.domain.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteIncidentRepository @Inject constructor(private val api: AlertsApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Incident>(sessions, { it.id }), IncidentRepository {
    override suspend fun refresh() = load { call { api.incidents().map { it.domain() } } }
    override suspend fun find(id: String) = remember { call { api.incident(id.backendId()).domain() } }
    override suspend fun resolve(id: String, action: String) { remember { call { api.resolve(id.backendId(), ResolutionDto(action)).domain() } } }
}
