package com.safeflow.app.features.reporting.infrastructure.remote

import retrofit2.http.GET
import com.safeflow.app.features.reporting.domain.Summary

data class SummaryDto(val products: Int, val units: Long, val risks: Int, val activeIncidents: Int, val inTransit: Int,
    val critical: Int, val warnings: Int, val shipments: Int, val preparation: Int, val delivered: Int, val empty: Boolean) {
    fun domain() = Summary(products, units, risks, activeIncidents, inTransit, critical, warnings, shipments, preparation, delivered)
}
interface ReportingApi { @GET("operational-summary") suspend fun summary(): SummaryDto }
