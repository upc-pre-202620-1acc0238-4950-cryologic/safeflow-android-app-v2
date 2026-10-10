package com.safeflow.app.features.alerts.infrastructure.di

import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.alerts.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlertsModule {
    @Provides @Singleton fun api(retrofit: Retrofit): AlertsApi = retrofit.create(AlertsApi::class.java)
    @Provides fun incidentRepository(impl: RemoteIncidentRepository): IncidentRepository = impl
}
