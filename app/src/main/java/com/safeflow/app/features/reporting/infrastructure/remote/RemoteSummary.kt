package com.safeflow.app.features.reporting.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.RemoteCall
import com.safeflow.app.features.reporting.domain.*
import com.safeflow.app.features.iam.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSummaryRepository @Inject constructor(private val api: ReportingApi, private val call: RemoteCall,
    private val sessions: SessionRepository) : SummaryRepository {
    private val value = MutableStateFlow<Summary?>(null)
    override val summaries = value.asStateFlow()
    override fun clear() { value.value = null }
    override suspend fun refresh() {
        val token = sessions.current()?.token ?: return clear()
        val result = call { api.summary().domain() }
        if (sessions.current()?.token == token) value.value = result
    }
}
