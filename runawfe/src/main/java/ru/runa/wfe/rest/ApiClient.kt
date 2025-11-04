package ru.runa.wfe.rest

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import ru.runa.wfe.rest.services.AuthApiService
import ru.runa.wfe.rest.services.ChatApiService
import ru.runa.wfe.rest.services.TaskApiService
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL: String = "http://10.0.2.2:8080/restapi/" // DEBUG DRAFT

    val tokenManager: TokenManager = TokenManager

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiInterceptor())
        .callTimeout(1, TimeUnit.MINUTES)
        .build()

    private val retrofit : Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    val chatService: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }

    val taskService: TaskApiService by lazy {
        retrofit.create(TaskApiService::class.java)
    }
}