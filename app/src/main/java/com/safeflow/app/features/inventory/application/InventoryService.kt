package com.safeflow.app.features.inventory.application

import com.safeflow.app.core.application.refreshAfterMutation
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import javax.inject.Inject

class InventoryService @Inject constructor(val products: ProductRepository, val lots: LotRepository, private val access: Access) {
    suspend fun create(name: String, type: String, minimum: String, maximum: String): String {
        access.require(Permission.WRITE_INVENTORY)
        val id = products.create(name.trim(), type.trim().ifEmpty { null }, minimum.toDoubleOrNull()?.takeIf { it.isFinite() }, maximum.toDoubleOrNull()?.takeIf { it.isFinite() })
        refreshAfterMutation { products.refresh(); lots.refresh() }
        return id
    }
    suspend fun addLot(productId: String, code: String): String {
        access.require(Permission.WRITE_INVENTORY)
        val id = lots.create(productId, code.trim())
        refreshAfterMutation { products.refresh(); lots.refresh() }
        return id
    }
    suspend fun income(lotId: String, quantity: String) {
        access.require(Permission.WRITE_INVENTORY)
        lots.income(lotId, quantity.toIntOrNull())
        refreshAfterMutation { products.refresh(); lots.refresh() }
    }
    suspend fun product(id: String) = products.find(id)
    suspend fun lot(id: String) = lots.find(id)
}
