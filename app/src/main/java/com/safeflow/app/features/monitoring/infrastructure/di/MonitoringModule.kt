package com.safeflow.app.features.monitoring.infrastructure.di

import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.monitoring.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MonitoringModule {
    @Provides @Singleton fun api(retrofit: Retrofit): MonitoringApi = retrofit.create(MonitoringApi::class.java)
    @Provides fun sensorRepository(impl: RemoteSensorRepository): SensorRepository = impl
    @Provides fun readingRepository(impl: RemoteReadingRepository): ReadingRepository = impl
    @Provides fun rejectionRepository(impl: RemoteRejectionRepository): RejectionRepository = impl
    @Provides fun riskRepository(impl: RemoteRiskRepository): RiskRepository = impl
}
