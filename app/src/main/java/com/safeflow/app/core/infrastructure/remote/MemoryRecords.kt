package com.safeflow.app.core.infrastructure.remote

import com.safeflow.app.core.domain.Records
import com.safeflow.app.features.iam.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class MemoryRecords<T>(protected val sessions: SessionRepository, private val id: (T) -> String) : Records<T> {
    private val values = MutableStateFlow<List<T>>(emptyList())
    private val lock = Any()
    private var revision = 0L
    override fun observe() = values.asStateFlow()
    override suspend fun all() = values.value
    override suspend fun find(id: String) = values.value.find { this.id(it) == id }
    override fun clear() { synchronized(lock) { revision++; values.value = emptyList() } }
    protected suspend fun remember(block: suspend () -> T): T {
        val token = sessions.current()?.token
        val result = block()
        if (token != null && sessions.current()?.token == token) {
            synchronized(lock) {
                revision++
                values.value = values.value.filterNot { id(it) == id(result) } + result
            }
        }
        return result
    }
    protected suspend fun load(block: suspend () -> List<T>) {
        val token = sessions.current()?.token ?: return clear()
        val version = synchronized(lock) { revision }
        val result = block()
        if (sessions.current()?.token == token) {
            synchronized(lock) {
                // A slow read cannot undo a confirmed mutation or a session reset.
                if (revision == version) { revision++; values.value = result }
            }
        }
    }
}
