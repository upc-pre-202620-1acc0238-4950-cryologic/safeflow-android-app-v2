package com.safeflow.app.features.iam.infrastructure.di

import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.iam.infrastructure.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton
import com.safeflow.app.features.iam.application.IdentityService

@Module
@InstallIn(SingletonComponent::class)
object IamModule {
    @Provides @Singleton fun api(retrofit: Retrofit): IamApi = retrofit.create(IamApi::class.java)
    @Provides fun identityRepository(impl: RemoteIdentityRepository): IdentityRepository = impl
    @Provides fun permissionsRepository(impl: RemotePermissionsRepository): PermissionsRepository = impl
    @Provides fun access(impl: IdentityService): Access = impl
}
