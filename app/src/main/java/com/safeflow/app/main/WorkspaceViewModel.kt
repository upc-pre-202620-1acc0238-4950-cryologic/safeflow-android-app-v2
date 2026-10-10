package com.safeflow.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeflow.app.core.domain.BusinessException
import com.safeflow.app.main.application.RefreshWorkspace
import com.safeflow.app.main.application.ObserveWorkspace
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.application.InventoryService
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.application.MonitoringService
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.application.AlertService
import com.safeflow.app.features.alerts.domain.Incident
import com.safeflow.app.features.logistics.application.LogisticsService
import com.safeflow.app.features.logistics.domain.ShipmentValidation
import com.safeflow.app.features.logistics.domain.Shipment
import com.safeflow.app.features.reporting.domain.Summary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.safeflow.app.features.logistics.presentation.ShipmentDraft

data class Destination(val name: String, val id: String = "", val shipmentHistory: Boolean = false)
data class WorkspaceState(val now: Long = 0, val loading: Boolean = true, val busy: Boolean = false,
    val account: Account? = null, val permissions: Set<Permission> = emptySet(),
    val destination: Destination = Destination("login"), val message: String? = null,
    val products: List<Product> = emptyList(), val lots: List<Lot> = emptyList(),
    val sensors: List<Sensor> = emptyList(), val readings: List<Reading> = emptyList(),
    val rejections: List<RejectedReading> = emptyList(), val incidents: List<Incident> = emptyList(),
    val shipments: List<Shipment> = emptyList(), val roles: List<RolePermissions> = emptyList(),
    val summary: Summary? = null, val risks: List<Risk> = emptyList(),
    val validation: ShipmentValidation? = null, val historyReadings: List<Reading>? = null, val shipmentDraft: ShipmentDraft = ShipmentDraft())
@HiltViewModel
class WorkspaceViewModel @Inject constructor(private val identity: IdentityService,
    private val inventory: InventoryService, private val monitoring: MonitoringService,
    private val alerts: AlertService, private val logistics: LogisticsService,
    private val workspace: ObserveWorkspace, private val refreshWorkspace: RefreshWorkspace) : ViewModel() {
    private val mutable = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = mutable.asStateFlow()
    private val history = mutableListOf<Destination>()
    init {
        viewModelScope.launch {
            try {
                workspace().collect { data ->
                    mutable.update { current ->
                        val changed = current.account?.id != data.account?.id
                        val requested = if (changed) Destination(home(data.account, data.permissions)) else current.destination
                        val destination = if (allowed(requested.name, data.account, data.permissions)) requested else Destination(home(data.account, data.permissions))
                        if (changed || destination != requested) history.clear()
                        current.copy(now = data.now, loading = false, account = data.account,
                            permissions = data.permissions, destination = destination, shipmentDraft = if (changed) ShipmentDraft() else current.shipmentDraft, products = data.products,
                            lots = data.lots, sensors = data.sensors, readings = data.readings,
                            rejections = data.rejections, incidents = data.incidents, shipments = data.shipments,
                            roles = data.roles, summary = data.summary, risks = data.risks,
                            validation = if (changed) null else current.validation, historyReadings = if (changed) null else current.historyReadings,
                            message = if (changed && current.account != null && data.account == null) "Tu sesión terminó. Inicia sesión nuevamente." else current.message)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutable.update { it.copy(loading = false, message = error.message ?: "No se pudo conectar con SafeFlow. Intenta nuevamente.") } }
        }
        viewModelScope.launch {
            identity.sessions.sessions.map { it?.token to it?.permissions }.distinctUntilChanged().collectLatest { (token, _) ->
                refreshWorkspace.clear()
                if (token != null) {
                    while (currentCoroutineContext().isActive) {
                        try { withContext(Dispatchers.IO) { refreshWorkspace() } }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { refreshFailure(error) }
                        delay(30_000)
                    }
                }
            }
        }
    }
    private var historyJob: Job? = null
    private fun refreshFailure(error: Exception) = message(if (error is BusinessException) error.message else "No se pudo actualizar la información. Revisa tu conexión.")
    private fun home(account: Account?, permissions: Set<Permission>) = when {
        account == null -> "login"
        account.role == Role.ADMIN -> "permissions"
        Permission.SUMMARY in permissions -> "summary"
        Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions -> "inventory"
        Permission.RISKS in permissions -> "risks"
        Permission.ALERTS in permissions || Permission.RESOLVE in permissions -> "alerts"
        Permission.READ_SHIPMENTS in permissions || Permission.CREATE_SHIPMENT in permissions || Permission.DELIVER in permissions -> "shipments"
        else -> "account"
    }
    private fun allowed(name: String, account: Account?, permissions: Set<Permission>): Boolean = when(name) {
        "login", "register", "verify", "recover" -> account == null
        "account" -> account != null
        "permissions" -> account?.role == Role.ADMIN
        "product-form", "lot-form", "income-form" -> Permission.WRITE_INVENTORY in permissions
        "inventory", "product", "lot", "lot-success" -> Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions
        "history" -> Permission.RISKS in permissions || Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions || Permission.READ_SHIPMENTS in permissions || Permission.DELIVER in permissions || Permission.CREATE_SHIPMENT in permissions
        "risks", "history-list" -> Permission.RISKS in permissions
        "alerts", "incident" -> Permission.ALERTS in permissions || Permission.RESOLVE in permissions
        "delivery" -> Permission.DELIVER in permissions
        "risk-confirm" -> Permission.READ_SHIPMENTS in permissions
        "shipment-form", "shipment-product", "shipment-lot", "shipment-sensor" -> Permission.CREATE_SHIPMENT in permissions
        "validation" -> Permission.READ_SHIPMENTS in permissions
        "shipments", "shipment" -> Permission.READ_SHIPMENTS in permissions || Permission.DELIVER in permissions || Permission.CREATE_SHIPMENT in permissions
        "summary" -> Permission.SUMMARY in permissions
        else -> false
    }
    fun navigate(name: String, id: String = "", root: Boolean = false, shipmentHistory: Boolean = false) {
        val current = mutable.value
        if (!allowed(name, current.account, current.permissions)) { message("No tienes acceso a esta operación."); return }
        val target = Destination(name, id, shipmentHistory)
        if (current.destination == target) return
        if (root) history.clear() else history.add(current.destination)
        mutable.update { it.copy(destination = target, message = null, validation = if (name == "risk-confirm") it.validation else null, historyReadings = null) }
        when (name) {
            "validation" -> perform {
                val result = logistics.validation(id)
                mutable.update { if (it.destination == Destination(name, id)) it.copy(validation = result) else it }
            }
            "product" -> perform { inventory.product(id) }
            "lot", "lot-success" -> perform { inventory.lot(id) }
            "incident" -> perform { alerts.incident(id) }
            "shipment" -> perform { logistics.shipment(id) }
        }
    }
    fun back() {
        var previous = history.removeLastOrNull()
        while (previous != null && !allowed(previous.name, state.value.account, state.value.permissions)) previous = history.removeLastOrNull()
        val destination = previous ?: Destination(home(state.value.account, state.value.permissions))
        mutable.update { it.copy(destination = destination, message = null, historyReadings = null) }
    }
    fun message(value: String?) { mutable.update { it.copy(message = value) } }
    fun perform(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                try { withContext(Dispatchers.IO) { refreshWorkspace() } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { refreshFailure(error) }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: BusinessException) { message(error.message) }
            catch (_: Exception) { message("No se pudo completar la solicitud. Revisa tu conexión e intenta nuevamente.") }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun history(from: Long, to: Long) {
        val route = state.value.destination
        if (route.name != "history") return
        val token = state.value.account?.id ?: return
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { monitoring.history(if (route.shipmentHistory) null else route.id, if (route.shipmentHistory) route.id else null, from, to) }
                mutable.update { if (it.destination == route && it.account?.id == token) it.copy(historyReadings = result) else it }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { refreshFailure(error) }
        }
    }
    fun login(email: String, password: String) = perform { refreshWorkspace.clear(); identity.login(email, password) }
    fun register(email: String, password: String, terms: Boolean) = perform {
        identity.register(email, password, terms); withContext(Dispatchers.Main) { navigate("verify") }
    }
    fun recover(email: String) = perform { identity.recover(email); message("Solicitud registrada. El envío de correos está pendiente; tu contraseña no ha cambiado.") }
    fun logout() = perform { identity.logout() }
    fun permissions(role: Role, granted: Set<Permission>) = perform { identity.update(role, granted); message("Permisos guardados.") }
    fun product(name: String, type: String, min: String, max: String) = perform {
        val id = inventory.create(name, type, min, max); withTimeout(5_000) { state.first { data -> data.products.any { it.id == id } } }; withContext(Dispatchers.Main) { navigate("product", id) }; message("Producto registrado.")
    }
    fun lot(product: String, code: String) = perform { val id = inventory.addLot(product, code); withTimeout(5_000) { state.first { data -> data.lots.any { it.id == id } } }; withContext(Dispatchers.Main) { navigate("lot-success", id) } }
    fun income(lot: String, quantity: String) = perform { inventory.income(lot, quantity); withContext(Dispatchers.Main) { back() }; message("Ingreso registrado.") }
    fun shipmentDraft(draft: ShipmentDraft) { mutable.update { it.copy(shipmentDraft = draft, message = null) } }
    fun newShipment() { shipmentDraft(ShipmentDraft()); navigate("shipment-form") }
    // Wireflow 17: product → lot → sensor chain; the form stays below so Cancel returns to it.
    fun selectShipment(value: String) {
        val draft = state.value.shipmentDraft
        when (state.value.destination.name) {
            "shipment-product" -> { shipmentDraft(draft.copy(productId = value, lotId = "", sensorId = "")); replace("shipment-lot") }
            "shipment-lot" -> { shipmentDraft(draft.copy(lotId = value)); if (draft.sensorId.isEmpty()) replace("shipment-sensor") else back() }
            else -> { shipmentDraft(draft.copy(sensorId = value)); back() }
        }
    }
    private fun replace(name: String) {
        val current = mutable.value
        if (!allowed(name, current.account, current.permissions)) { message("No tienes acceso a esta operación."); return }
        mutable.update { it.copy(destination = Destination(name), message = null) }
    }
    fun submitShipment() { val draft = state.value.shipmentDraft; shipment(draft.productId, draft.lotId, draft.quantity, draft.destination, draft.sensorId) }
    fun shipment(product: String, lot: String, quantity: String, destination: String, sensor: String) = perform {
        val id = logistics.create(product, lot, quantity, destination, sensor)
        withTimeout(5_000) { state.first { data -> data.shipments.any { it.id == id } } }
        withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Despacho registrado en preparación.")
    }
    fun depart(id: String, accepted: Boolean) = perform { logistics.depart(id, accepted); withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Despacho en tránsito.") }
    fun deliver(id: String) = perform { logistics.deliver(id); withTimeout(5_000) { state.first { data -> data.shipments.any { it.id == id && it.status == com.safeflow.app.features.logistics.domain.ShipmentStatus.DELIVERED } } }; withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Entrega confirmada.") }
    fun resolve(id: String, action: String) = perform { alerts.resolve(id, action); message("Incidencia resuelta. Se conserva su historial.") }
}
