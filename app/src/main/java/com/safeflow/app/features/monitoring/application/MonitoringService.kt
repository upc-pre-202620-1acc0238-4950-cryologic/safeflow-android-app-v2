package com.safeflow.app.features.monitoring.application

import com.safeflow.app.features.monitoring.domain.*
import javax.inject.Inject

class MonitoringService @Inject constructor(val sensors: SensorRepository, val readings: ReadingRepository,
    val rejections: RejectionRepository, val risks: RiskRepository) {
    suspend fun history(productId: String?, shipmentId: String?, from: Long, to: Long) = readings.history(productId, shipmentId, from, to)
}
