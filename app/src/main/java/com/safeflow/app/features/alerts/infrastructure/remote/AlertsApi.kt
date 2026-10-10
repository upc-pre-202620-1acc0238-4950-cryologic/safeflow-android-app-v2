package com.safeflow.app.features.alerts.infrastructure.remote

import retrofit2.http.*
import com.safeflow.app.core.infrastructure.remote.epochMillis
import com.safeflow.app.features.alerts.domain.Incident
import com.safeflow.app.features.monitoring.domain.ThermalCondition

data class IncidentDto(val id: Int, val sensorId: Int, val productId: Int, val location: String?, val type: String?,
    val severity: String, val description: String?, val openedAt: String, val lastSeenAt: String, val repetitions: Int,
    val action: String?, val author: String?, val resolvedAt: String?, val active: Boolean) {
    fun domain() = Incident(id.toString(), sensorId.toString(), productId.toString(), location.orEmpty(), type.orEmpty(),
        ThermalCondition.valueOf(severity), description.orEmpty(), openedAt.epochMillis(), lastSeenAt.epochMillis(), repetitions,
        action, author, resolvedAt?.epochMillis())
}
data class ResolutionDto(val action: String)
interface AlertsApi {
    @GET("incidents") suspend fun incidents(): List<IncidentDto>
    @GET("incidents/{id}") suspend fun incident(@Path("id") id: Int): IncidentDto
    @POST("incidents/{id}/resolution") suspend fun resolve(@Path("id") id: Int, @Body body: ResolutionDto): IncidentDto
}
