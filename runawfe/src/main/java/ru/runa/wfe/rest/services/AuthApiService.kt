package ru.runa.wfe.rest.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import ru.runa.wfe.rest.dto.WfeCredentials

interface AuthApiService {

    @POST("auth/basic")
    @Headers("Content-Type: application/json")
    suspend fun basic(@Body credentials: WfeCredentials): Response<String>
}