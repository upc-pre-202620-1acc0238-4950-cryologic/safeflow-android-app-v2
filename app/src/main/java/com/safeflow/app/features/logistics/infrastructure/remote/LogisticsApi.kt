package com.safeflow.app.features.logistics.infrastructure.remote

import retrofit2.http.*
import com.safeflow.app.core.infrastructure.remote.epochMillis
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.monitoring.domain.*

data class ShipmentDto(val id: Int, val productId: Int, val lotId: Int, val sensorId: Int, val quantity: Int,
    val destination: String?, val status: String, val createdAt: String, val createdBy: String?, val departedAt: String?,
    val departedBy: String?, val riskAccepted: Boolean, val deliveredAt: String?, val deliveredBy: String?,
    val productName: String?, val temperature: Double?) {
    fun domain() = Shipment(id.toString(), productId.toString(), lotId.toString(), quantity, destination.orEmpty(), sensorId.toString(),
        createdAt.epochMillis(), createdBy.orEmpty(), ShipmentStatus.valueOf(status), departedAt?.epochMillis(), departedBy,
        riskAccepted, deliveredAt?.epochMillis(), deliveredBy)
}
data class CreateShipmentDto(val productId: Int, val lotId: Int, val quantity: Int, val destination: String, val sensorId: Int)
data class DepartureDto(val acceptRisk: Boolean)
data class ValidationReadingDto(val id: Int, val sensorId: Int, val shipmentId: Int?, val recordedAt: String, val temperature: Double, val condition: String) {
    fun domain() = Reading(id.toString(), sensorId.toString(), recordedAt.epochMillis(), temperature, ThermalCondition.valueOf(condition), shipmentId?.toString())
}
data class ValidationDto(val sensorConnected: Boolean, val latestValidReading: ValidationReadingDto?, val condition: String,
    val canDepart: Boolean, val requiresRiskConfirmation: Boolean, val blockedReason: String?) {
    fun domain() = ShipmentValidation(sensorConnected, latestValidReading?.domain(), ThermalCondition.valueOf(condition), canDepart, requiresRiskConfirmation, blockedReason)
}
interface LogisticsApi {
    @GET("shipments") suspend fun shipments(): List<ShipmentDto>
    @GET("shipments/{id}") suspend fun shipment(@Path("id") id: Int): ShipmentDto
    @POST("shipments") suspend fun create(@Body body: CreateShipmentDto): ShipmentDto
    @GET("shipments/{id}/validation") suspend fun validation(@Path("id") id: Int): ValidationDto
    @POST("shipments/{id}/departure") suspend fun depart(@Path("id") id: Int, @Body body: DepartureDto): ShipmentDto
    @POST("shipments/{id}/delivery") suspend fun deliver(@Path("id") id: Int): ShipmentDto
}
