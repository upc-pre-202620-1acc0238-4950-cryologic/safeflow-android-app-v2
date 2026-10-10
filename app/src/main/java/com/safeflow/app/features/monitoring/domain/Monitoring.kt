package com.safeflow.app.features.monitoring.domain
import com.safeflow.app.core.domain.Records

enum class ThermalCondition(val label: String) {
    NORMAL("Normal"), WARNING("Advertencia"), RISK("Riesgo"), CRITICAL("Crítico"),
    DISCONNECTED("Desconectado"), UNKNOWN("Sin lecturas")
}
data class Sensor(val id: String, val productId: String, val location: String, val connected: Boolean = true,
    val condition: ThermalCondition = ThermalCondition.UNKNOWN, val latestReading: Reading? = null)
data class Reading(val id: String, val sensorId: String, val at: Long, val temperature: Double,
    val condition: ThermalCondition, val shipmentId: String? = null)
data class RejectedReading(val id: String, val sensorId: String, val at: Long, val raw: String, val reason: String)
interface SensorRepository : Records<Sensor>
interface ReadingRepository : Records<Reading> {
    suspend fun history(productId: String?, shipmentId: String?, from: Long, to: Long): List<Reading>
}
interface RejectionRepository : Records<RejectedReading>
data class Risk(val productId: String, val sensorId: String, val temperature: Double?,
    val condition: ThermalCondition, val at: Long?)
interface RiskRepository : Records<Risk>
