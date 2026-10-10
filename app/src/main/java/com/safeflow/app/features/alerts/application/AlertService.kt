package com.safeflow.app.features.alerts.application

import com.safeflow.app.core.application.refreshAfterMutation
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.iam.domain.*
import javax.inject.Inject

class AlertService @Inject constructor(val incidents: IncidentRepository, private val access: Access) {
    suspend fun resolve(id: String, action: String) {
        access.require(Permission.RESOLVE)
        incidents.resolve(id, action.trim())
        refreshAfterMutation { incidents.refresh() }
    }
    suspend fun incident(id: String) = incidents.find(id)
}
