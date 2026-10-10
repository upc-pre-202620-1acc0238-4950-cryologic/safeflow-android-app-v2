package com.safeflow.app.main.application

import com.safeflow.app.core.domain.Clock
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.reporting.application.ReportingService
import com.safeflow.app.features.reporting.domain.Summary
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class WorkspaceData(val now: Long, val account: Account?, val permissions: Set<Permission>,
    val products: List<Product>, val lots: List<Lot>, val sensors: List<Sensor>, val readings: List<Reading>,
    val rejections: List<RejectedReading>, val incidents: List<Incident>, val shipments: List<Shipment>,
    val roles: List<RolePermissions>, val summary: Summary?, val risks: List<Risk> = emptyList())
private data class InventoryData(val products: List<Product>, val lots: List<Lot>)
private data class MonitoringData(val sensors: List<Sensor>, val readings: List<Reading>, val rejections: List<RejectedReading>, val risks: List<Risk>)
private data class OperationData(val incidents: List<Incident>, val shipments: List<Shipment>, val roles: List<RolePermissions>)

class ObserveWorkspace @Inject constructor(private val identity: IdentityService,
    private val products: ProductRepository, private val lots: LotRepository,
    private val sensors: SensorRepository, private val readings: ReadingRepository, private val rejections: RejectionRepository,
    private val risks: RiskRepository, private val incidents: IncidentRepository, private val shipments: ShipmentRepository,
    private val reporting: ReportingService, private val clock: Clock) {
    operator fun invoke(): Flow<WorkspaceData> {
        val inventory = combine(products.observe(), lots.observe(), ::InventoryData)
        val monitoring = combine(sensors.observe(), readings.observe(), rejections.observe(), risks.observe(), ::MonitoringData)
        val operations = combine(incidents.observe(), shipments.observe(), identity.roles.observe(), ::OperationData)
        val ticker = flow { while (currentCoroutineContext().isActive) { emit(clock.now()); delay(1000) } }
        val access = combine(identity.sessions.sessions, ticker) { _, _ -> identity.account() to identity.permissions() }
        return combine(inventory, monitoring, operations, access, reporting.summaries) { inv, mon, ops, auth, summary ->
            val (account, permissions) = auth
            if (account == null) WorkspaceData(clock.now(), null, emptySet(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null)
            else {
                fun can(vararg required: Permission) = required.any { it in permissions }
                val canInventory = can(Permission.READ_INVENTORY, Permission.WRITE_INVENTORY, Permission.CREATE_SHIPMENT)
                val canMonitoring = can(Permission.READ_INVENTORY, Permission.WRITE_INVENTORY, Permission.CREATE_SHIPMENT, Permission.READ_SHIPMENTS, Permission.DELIVER, Permission.RISKS)
                WorkspaceData(clock.now(), account, permissions,
                    if (canInventory || can(Permission.RISKS)) inv.products else emptyList(), if (canInventory) inv.lots else emptyList(),
                    if (canMonitoring) mon.sensors else emptyList(), if (canMonitoring) mon.readings else emptyList(),
                    if (can(Permission.READ_INVENTORY, Permission.RISKS)) mon.rejections else emptyList(),
                    if (can(Permission.ALERTS, Permission.RESOLVE)) ops.incidents else emptyList(),
                    if (can(Permission.READ_SHIPMENTS, Permission.CREATE_SHIPMENT, Permission.DELIVER)) ops.shipments else emptyList(),
                    if (account.role == Role.ADMIN) ops.roles else emptyList(), if (can(Permission.SUMMARY)) summary else null,
                    if (can(Permission.RISKS)) mon.risks else emptyList())
            }
        }
    }
}
