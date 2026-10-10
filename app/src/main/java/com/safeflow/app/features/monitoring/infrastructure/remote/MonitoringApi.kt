package com.safeflow.app.features.monitoring.infrastructure.remote

import retrofit2.http.*
import com.safeflow.app.core.infrastructure.remote.epochMillis
import com.safeflow.app.features.monitoring.domain.*

data class ReadingDto(val id: Int, val sensorId: Int, val shipmentId: Int?, val recordedAt: String, val temperature: Double, val condition: String) {
    fun domain() = Reading(id.toString(), sensorId.toString(), recordedAt.epochMillis(), temperature, ThermalCondition.valueOf(condition), shipmentId?.toString())
}
data class SensorDto(val id: Int, val productId: Int, val location: String?, val connected: Boolean, val condition: String, val latestReading: ReadingDto?) {
    fun domain() = Sensor(id.toString(), productId.toString(), location.orEmpty(), connected, ThermalCondition.valueOf(condition), latestReading?.domain())
}
data class RejectionDto(val id: Int, val sensorId: Int, val recordedAt: String, val raw: String?, val reason: String?) {
    fun domain() = RejectedReading(id.toString(), sensorId.toString(), recordedAt.epochMillis(), raw.orEmpty(), reason.orEmpty())
}
data class RiskDto(val productId: Int, val sensorId: Int, val temperature: Double?, val condition: String, val recordedAt: String?) {
    fun domain() = Risk(productId.toString(), sensorId.toString(), temperature, ThermalCondition.valueOf(condition), recordedAt?.epochMillis())
}
interface MonitoringApi {
    @GET("sensors") suspend fun sensors(): List<SensorDto>
    @GET("readings") suspend fun readings(@Query("productId") productId: Int? = null, @Query("shipmentId") shipmentId: Int? = null,
        @Query("from") from: String? = null, @Query("to") to: String? = null): List<ReadingDto>
    @GET("rejected-readings") suspend fun rejections(): List<RejectionDto>
    @GET("risks") suspend fun risks(): List<RiskDto>
}
