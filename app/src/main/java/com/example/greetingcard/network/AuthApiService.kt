package com.example.greetingcard.network

import com.example.greetingcard.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

private val retrofit =
        Retrofit.Builder()
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .baseUrl(BuildConfig.AUTH_SERVER_ENDPOINT)
                .build()

@Serializable data class AuthServiceResponse(val result: Boolean)

object AuthApi {
    val retrofitService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
}

@Serializable data class TokenRequest(val token: String)

interface AuthApiService {
    @POST("api/verifytoken")
    suspend fun postValidityToken(@Body request: TokenRequest): AuthServiceResponse
}
