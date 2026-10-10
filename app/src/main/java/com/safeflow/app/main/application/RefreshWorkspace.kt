package com.safeflow.app.main.application

import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.reporting.application.ReportingService
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class RefreshWorkspace @Inject constructor(private val identity: IdentityService,
    private val products: ProductRepository, private val lots: LotRepository,
    private val sensors: SensorRepository, private val readings: ReadingRepository, private val rejections: RejectionRepository,
    private val risks: RiskRepository, private val incidents: IncidentRepository, private val shipments: ShipmentRepository,
    private val reporting: ReportingService) {
    fun clear() {
        products.clear(); lots.clear(); sensors.clear(); readings.clear(); rejections.clear()
        risks.clear(); incidents.clear(); shipments.clear(); identity.roles.clear(); reporting.clear()
    }
    suspend operator fun invoke() {
        if (identity.account() == null) { clear(); return }
        identity.refresh()
        val permissions = identity.permissions()
        fun can(vararg required: Permission) = required.any { it in permissions }
        // Match endpoint authorization rather than widening access from a screen's needs.
        val jobs = listOf<suspend () -> Unit>(
            { if (can(Permission.READ_INVENTORY, Permission.WRITE_INVENTORY, Permission.CREATE_SHIPMENT)) { products.refresh(); lots.refresh() }
                else { lots.clear(); if (can(Permission.RISKS)) products.refresh() else products.clear() } },
            { if (can(Permission.READ_INVENTORY, Permission.WRITE_INVENTORY, Permission.CREATE_SHIPMENT, Permission.READ_SHIPMENTS, Permission.DELIVER, Permission.RISKS)) { sensors.refresh(); readings.refresh() } else { sensors.clear(); readings.clear() } },
            { if (can(Permission.READ_INVENTORY, Permission.RISKS)) rejections.refresh() else rejections.clear() },
            { if (can(Permission.RISKS)) risks.refresh() else risks.clear() },
            { if (can(Permission.ALERTS, Permission.RESOLVE)) incidents.refresh() else incidents.clear() },
            { if (can(Permission.READ_SHIPMENTS, Permission.CREATE_SHIPMENT, Permission.DELIVER)) shipments.refresh() else shipments.clear() },
            { if (identity.account()?.role == Role.ADMIN) identity.roles.refresh() else identity.roles.clear() },
            { if (can(Permission.SUMMARY)) reporting.refresh() else reporting.clear() }
        )
        var failure: Exception? = null
        for (job in jobs) {
            if (identity.account() == null) { clear(); return }
            try { job() } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (failure == null) failure = error }
        }
        failure?.let { throw it }
    }
}
