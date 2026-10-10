package com.safeflow.app.features.monitoring.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.iam.domain.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSensorRepository @Inject constructor(private val api: MonitoringApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Sensor>(sessions, { it.id }), SensorRepository {
    override suspend fun refresh() = load { call { api.sensors().map { it.domain() } } }
}
@Singleton
class RemoteReadingRepository @Inject constructor(private val api: MonitoringApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Reading>(sessions, { it.id }), ReadingRepository {
    override suspend fun refresh() = load { call { api.readings().map { it.domain() } } }
    override suspend fun history(productId: String?, shipmentId: String?, from: Long, to: Long) = call {
        api.readings(productId?.backendId(), shipmentId?.backendId(), from.isoDate(), to.isoDate()).map { it.domain() }
    }
}
@Singleton
class RemoteRejectionRepository @Inject constructor(private val api: MonitoringApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<RejectedReading>(sessions, { it.id }), RejectionRepository {
    override suspend fun refresh() = load { call { api.rejections().map { it.domain() } } }
}
@Singleton
class RemoteRiskRepository @Inject constructor(private val api: MonitoringApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Risk>(sessions, { it.sensorId }), RiskRepository {
    override suspend fun refresh() = load { call { api.risks().map { it.domain() } } }
}
