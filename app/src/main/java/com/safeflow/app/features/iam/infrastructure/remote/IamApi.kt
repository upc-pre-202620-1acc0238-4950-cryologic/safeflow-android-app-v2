package com.safeflow.app.features.iam.infrastructure.remote

import retrofit2.http.*

data class SignInDto(val email: String, val password: String)
data class SignUpDto(val email: String, val password: String, val terms: Boolean)
data class RecoveryDto(val email: String)
data class UserDto(val id: Int, val email: String?, val role: String, val permissions: List<String>?)
data class SessionDto(val id: Int, val email: String?, val role: String, val permissions: List<String>?, val token: String, val expiresAt: String)
data class RolePermissionsDto(val role: String, val granted: Set<String>?)
data class ReplacePermissionsDto(val granted: Set<String>)
interface IamApi {
    @POST("auth/sign-up") suspend fun signUp(@Body body: SignUpDto): UserDto
    @POST("auth/sign-in") suspend fun signIn(@Body body: SignInDto): SessionDto
    @POST("auth/sign-out") suspend fun signOut()
    @POST("recovery-requests") suspend fun recover(@Body body: RecoveryDto)
    @GET("users/me") suspend fun me(): UserDto
    @GET("role-permissions") suspend fun permissions(): List<RolePermissionsDto>
    @PUT("role-permissions/{role}") suspend fun permissions(@Path("role") role: String, @Body body: ReplacePermissionsDto): RolePermissionsDto
}
