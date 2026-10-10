package com.safeflow.app.features.inventory.infrastructure.remote

import retrofit2.http.*
import com.safeflow.app.features.inventory.domain.*

data class ProductDto(val id: Int, val name: String?, val type: String?, val minimum: Double, val maximum: Double,
    val stock: Long, val reserved: Long, val available: Long, val condition: String) {
    fun domain() = Product(id.toString(), name.orEmpty(), type, minimum, maximum)
}
data class LotDto(val id: Int, val productId: Int, val code: String?, val stock: Int, val reserved: Int, val available: Int) {
    fun domain() = Lot(id.toString(), productId.toString(), code.orEmpty(), stock, reserved)
}
data class IncomeDto(val id: Int, val lotId: Int, val productId: Int, val quantity: Int, val author: String?, val recordedAt: String)
data class CreateProductDto(val name: String, val type: String?, val minimum: Double?, val maximum: Double?)
data class CreateLotDto(val code: String)
data class IncomeRequestDto(val quantity: Int?)
interface InventoryApi {
    @GET("products") suspend fun products(): List<ProductDto>
    @GET("products/{id}") suspend fun product(@Path("id") id: Int): ProductDto
    @POST("products") suspend fun create(@Body body: CreateProductDto): ProductDto
    @GET("products/{id}/lots") suspend fun lots(@Path("id") id: Int): List<LotDto>
    @GET("lots/{id}") suspend fun lot(@Path("id") id: Int): LotDto
    @POST("products/{id}/lots") suspend fun createLot(@Path("id") id: Int, @Body body: CreateLotDto): LotDto
    @POST("lots/{id}/incomes") suspend fun income(@Path("id") id: Int, @Body body: IncomeRequestDto): IncomeDto
}
