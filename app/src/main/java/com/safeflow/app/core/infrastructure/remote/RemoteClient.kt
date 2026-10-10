package com.safeflow.app.core.infrastructure.remote

import com.google.gson.Gson
import com.safeflow.app.core.domain.BusinessException
import com.safeflow.app.features.iam.domain.SessionRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

private data class ErrorDto(val error: String?, val message: String?)

@Singleton
class RemoteCall @Inject constructor(private val gson: Gson) {
    suspend operator fun <T> invoke(block: suspend () -> T): T = try { block() }
    catch (error: HttpException) {
        val message = try { gson.fromJson(error.response()?.errorBody()?.string(), ErrorDto::class.java)?.message }
            catch (_: Exception) { null }
        throw BusinessException(message ?: if (error.code() == 401) "Tu sesión terminó. Inicia sesión nuevamente."
            else "No se pudo completar la solicitud (${error.code()}).")
    }
}

class BearerInterceptor(private val sessions: SessionRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { sessions.current() }
        val token = session?.takeIf { it.expiresAt > System.currentTimeMillis() }?.token
        if (session != null && token == null) runBlocking { sessions.clearIfToken(session.token) }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401 && token != null) runBlocking { sessions.clearIfToken(token) }
        return response
    }
}

fun String.epochMillis(): Long {
    // .NET emits up to seven fractional digits; SimpleDateFormat consumes milliseconds.
    val match = Regex("^(.*T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:\\d{2})$").matchEntire(this)
        ?: throw BusinessException("La API devolvió una fecha inválida.")
    val normalized = match.groupValues[1] + "." + match.groupValues[2].padEnd(3, '0').take(3) + match.groupValues[3]
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { isLenient = false }.parse(normalized)!!.time
}
fun Long.isoDate(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.format(Date(this))
fun String.backendId(): Int = toIntOrNull()?.takeIf { it > 0 }
    ?: throw BusinessException("Selecciona un registro existente.")
