package com.safeflow.app.core.infrastructure.remote

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.safeflow.app.BuildConfig
import com.safeflow.app.core.domain.Clock
import com.safeflow.app.features.iam.domain.SessionRepository
import com.safeflow.app.features.iam.infrastructure.local.DataStoreSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.sessionStore by preferencesDataStore("safeflow_session")
@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {
    @Provides fun clock(): Clock = Clock { System.currentTimeMillis() }
    @Provides @Singleton fun sessions(@ApplicationContext context: Context): SessionRepository = DataStoreSession(context.sessionStore)
    @Provides @Singleton fun gson(): Gson = Gson()
    @Provides @Singleton fun client(sessions: SessionRepository): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BearerInterceptor(sessions)).connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).build()
    @Provides @Singleton fun retrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create(gson)).build()
}
