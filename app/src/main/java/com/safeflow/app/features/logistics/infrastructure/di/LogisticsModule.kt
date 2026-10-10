package com.safeflow.app.features.logistics.infrastructure.di

import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.logistics.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogisticsModule {
    @Provides @Singleton fun api(retrofit: Retrofit): LogisticsApi = retrofit.create(LogisticsApi::class.java)
    @Provides fun shipmentRepository(impl: RemoteShipmentRepository): ShipmentRepository = impl
}
