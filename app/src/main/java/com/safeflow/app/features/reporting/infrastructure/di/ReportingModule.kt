package com.safeflow.app.features.reporting.infrastructure.di

import com.safeflow.app.features.reporting.domain.*
import com.safeflow.app.features.reporting.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReportingModule {
    @Provides @Singleton fun api(retrofit: Retrofit): ReportingApi = retrofit.create(ReportingApi::class.java)
    @Provides fun summaryRepository(impl: RemoteSummaryRepository): SummaryRepository = impl
}
