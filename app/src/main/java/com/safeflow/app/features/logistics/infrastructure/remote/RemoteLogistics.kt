package com.safeflow.app.features.logistics.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.*
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.iam.domain.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteShipmentRepository @Inject constructor(private val api: LogisticsApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Shipment>(sessions, { it.id }), ShipmentRepository {
    override suspend fun refresh() = load { call { api.shipments().map { it.domain() } } }
    override suspend fun find(id: String) = remember { call { api.shipment(id.backendId()).domain() } }
    override suspend fun create(productId: String, lotId: String, quantity: Int, destination: String, sensorId: String) = remember { call {
        api.create(CreateShipmentDto(productId.backendId(), lotId.backendId(), quantity, destination, sensorId.backendId())).domain()
    } }.id
    override suspend fun validation(id: String) = call { api.validation(id.backendId()).domain() }
    override suspend fun depart(id: String, acceptRisk: Boolean) { remember { call { api.depart(id.backendId(), DepartureDto(acceptRisk)).domain() } } }
    override suspend fun deliver(id: String) { remember { call { api.deliver(id.backendId()).domain() } } }
}
