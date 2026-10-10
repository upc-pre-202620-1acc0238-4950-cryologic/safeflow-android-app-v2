package com.safeflow.app.features.reporting.application

import com.safeflow.app.features.reporting.domain.SummaryRepository
import com.safeflow.app.features.iam.domain.*
import javax.inject.Inject

class ReportingService @Inject constructor(private val access: Access, private val repository: SummaryRepository) {
    val summaries = repository.summaries
    suspend fun refresh() { access.require(Permission.SUMMARY); repository.refresh() }
    fun clear() = repository.clear()
}
