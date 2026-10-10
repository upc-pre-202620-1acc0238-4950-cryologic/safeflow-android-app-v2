package com.safeflow.app.features.inventory.domain
import com.safeflow.app.core.domain.Records

data class Product(val id: String, val name: String, val type: String?,  val minimum: Double,
    val maximum: Double)
data class Lot(val id: String, val productId: String, val code: String, val stock: Int = 0, val reserved: Int = 0) {
    val available: Int get() = stock - reserved
}
data class InventoryTotals(val stock: Long = 0, val reserved: Long = 0) {
    val available: Long get() = stock - reserved
}
fun productTotals(productId: String, lots: List<Lot>): InventoryTotals {
    val own = lots.filter { it.productId == productId }
    return InventoryTotals(own.sumOf { it.stock.toLong() }, own.sumOf { it.reserved.toLong() })
}
interface ProductRepository : Records<Product> {
    suspend fun create(name: String, type: String?, minimum: Double?, maximum: Double?): String
}
interface LotRepository : Records<Lot> {
    suspend fun create(productId: String, code: String): String
    suspend fun income(lotId: String, quantity: Int?)
}
