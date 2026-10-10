package com.safeflow.app.features.logistics.domain
import com.safeflow.app.core.domain.Records
import com.safeflow.app.features.monitoring.domain.Reading
import com.safeflow.app.features.monitoring.domain.ThermalCondition

enum class ShipmentStatus(val label: String) { PREPARATION("En preparación"), TRANSIT("En tránsito"), DELIVERED("Entregado") }
data class Shipment(val id: String, val productId: String, val lotId: String, val quantity: Int,
    val destination: String, val sensorId: String, val createdAt: Long, val createdBy: String,
    val status: ShipmentStatus = ShipmentStatus.PREPARATION, val departedAt: Long? = null,
    val departedBy: String? = null, val riskAccepted: Boolean = false,
    val deliveredAt: Long? = null, val deliveredBy: String? = null)
data class ShipmentValidation(val sensorConnected: Boolean, val latestValidReading: Reading?,
    val condition: ThermalCondition, val canDepart: Boolean, val requiresRiskConfirmation: Boolean,
    val blockedReason: String?)
interface ShipmentRepository : Records<Shipment> {
    suspend fun create(productId: String, lotId: String, quantity: Int, destination: String, sensorId: String): String
    suspend fun validation(id: String): ShipmentValidation
    suspend fun depart(id: String, acceptRisk: Boolean)
    suspend fun deliver(id: String)
}
