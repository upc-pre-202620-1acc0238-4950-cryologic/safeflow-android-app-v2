package com.safeflow.app.features.logistics.application

import com.safeflow.app.core.application.refreshAfterMutation
import com.safeflow.app.core.domain.positiveQuantity
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.logistics.domain.*
import javax.inject.Inject

class LogisticsService @Inject constructor(val shipments: ShipmentRepository, private val products: ProductRepository,
    private val lots: LotRepository, private val access: Access) {
    suspend fun create(productId: String, lotId: String, quantity: String, destination: String, sensorId: String): String {
        access.require(Permission.CREATE_SHIPMENT)
        val id = shipments.create(productId, lotId, positiveQuantity(quantity), destination.trim(), sensorId)
        refreshAfterMutation { refresh() }
        return id
    }
    suspend fun validation(id: String): ShipmentValidation { access.require(Permission.READ_SHIPMENTS); return shipments.validation(id) }
    suspend fun depart(id: String, acceptRisk: Boolean) { access.require(Permission.READ_SHIPMENTS); shipments.depart(id, acceptRisk); refreshAfterMutation { refresh() } }
    suspend fun deliver(id: String) { access.require(Permission.DELIVER); shipments.deliver(id); refreshAfterMutation { refresh() } }
    suspend fun shipment(id: String) = shipments.find(id)
    private suspend fun refresh() {
        shipments.refresh()
        if (access.permissions().any { it in setOf(Permission.READ_INVENTORY, Permission.WRITE_INVENTORY, Permission.CREATE_SHIPMENT) }) {
            products.refresh(); lots.refresh()
        }
    }
}
