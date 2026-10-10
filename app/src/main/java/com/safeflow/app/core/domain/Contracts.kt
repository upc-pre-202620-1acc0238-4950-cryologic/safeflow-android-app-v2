package com.safeflow.app.core.domain

import kotlinx.coroutines.flow.Flow

interface Records<T> {
    fun observe(): Flow<List<T>>
    suspend fun all(): List<T>
    suspend fun find(id: String): T?
    suspend fun refresh()
    fun clear()
}
fun interface Clock { fun now(): Long }
class BusinessException(message: String) : IllegalArgumentException(message)
fun ensure(condition: Boolean, message: String) { if (!condition) throw BusinessException(message) }
fun positiveQuantity(text: String): Int = text.toIntOrNull()?.takeIf { it > 0 }
    ?: throw BusinessException("La cantidad debe ser un entero mayor que cero.")
