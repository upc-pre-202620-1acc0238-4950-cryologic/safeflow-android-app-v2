package com.safeflow.app.core.application

import com.safeflow.app.core.domain.BusinessException
import kotlinx.coroutines.CancellationException

suspend fun refreshAfterMutation(block: suspend () -> Unit) {
    try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) {
        throw BusinessException("La operación quedó registrada, pero no se pudieron actualizar los datos. Vuelve a abrir la pantalla para consultarla.")
    }
}
