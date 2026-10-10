package com.safeflow.app.features.inventory.infrastructure.di

import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.inventory.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InventoryModule {
    @Provides @Singleton fun api(retrofit: Retrofit): InventoryApi = retrofit.create(InventoryApi::class.java)
    @Provides fun productRepository(impl: RemoteProductRepository): ProductRepository = impl
    @Provides fun lotRepository(impl: RemoteLotRepository): LotRepository = impl
}
