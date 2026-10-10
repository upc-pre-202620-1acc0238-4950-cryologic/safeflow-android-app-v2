package com.safeflow.app.features.inventory.infrastructure.remote

import com.safeflow.app.core.infrastructure.remote.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.iam.domain.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProductRepository @Inject constructor(private val api: InventoryApi, private val call: RemoteCall, sessions: SessionRepository) :
    MemoryRecords<Product>(sessions, { it.id }), ProductRepository {
    override suspend fun refresh() = load { call { api.products().map { it.domain() } } }
    override suspend fun find(id: String) = remember { call { api.product(id.backendId()).domain() } }
    override suspend fun create(name: String, type: String?, minimum: Double?, maximum: Double?) =
        remember { call { api.create(CreateProductDto(name, type, minimum, maximum)).domain() } }.id
}
@Singleton
class RemoteLotRepository @Inject constructor(private val api: InventoryApi, private val call: RemoteCall,
    private val products: ProductRepository, sessions: SessionRepository) : MemoryRecords<Lot>(sessions, { it.id }), LotRepository {
    override suspend fun refresh() = load { call { products.all().flatMap { api.lots(it.id.backendId()).map { dto -> dto.domain() } } } }
    override suspend fun find(id: String) = remember { call { api.lot(id.backendId()).domain() } }
    override suspend fun create(productId: String, code: String) = remember { call { api.createLot(productId.backendId(), CreateLotDto(code)).domain() } }.id
    override suspend fun income(lotId: String, quantity: Int?) { call { api.income(lotId.backendId(), IncomeRequestDto(quantity)) } }
}
