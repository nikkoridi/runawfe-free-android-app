package ru.runa.wfe.rest

import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import ru.runa.wfe.rest.dto.CustomLongDeserializer
import ru.runa.wfe.rest.services.AuthApiService
import ru.runa.wfe.rest.services.ChatApiService
import ru.runa.wfe.rest.services.TaskApiService
import java.util.concurrent.TimeUnit

object ApiClient {
    private var BASE_URL: String = "http://10.0.2.2:8080/restapi/"

    val tokenManager: TokenManager = TokenManager

    fun setServerUrl(url: String) {
        val baseUrl = getBaseUrl(url)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (checkServer(baseUrl)) {
                    BASE_URL = "$baseUrl/restapi/"
                }
                else {
                    Log.e(this::class.simpleName, "Given URL is not a RunaWFE server")
                }
            } catch (ex: Exception) {
                Log.e(this::class.simpleName, ex.message.toString())
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiInterceptor())
        .callTimeout(1, TimeUnit.MINUTES)
        .build()

    fun getBaseUrl(url: String): String {
        try {
            val baseUrl = Uri.parse(url)
            val port = if (baseUrl.port != -1) ":${baseUrl.port}" else ""
            return "${baseUrl.scheme}://${baseUrl.host}$port"
        } catch (e: Exception) {
            Log.e("API Client", "Invalid URL")
            return ""
        }
    }

    private suspend fun checkServer(url: String): Boolean {
        val versionRequest = Request.Builder()
            .url("$url/wfe/version/")
            .build()
        return withContext(Dispatchers.IO) {
            val response  = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .build()
                    .newCall(versionRequest)
                    .execute()
            !response.body?.string().isNullOrBlank()
        }
    }

    private val module = SimpleModule().apply {
        addDeserializer(Long::class.javaPrimitiveType, CustomLongDeserializer())
        addDeserializer(Long::class.java, CustomLongDeserializer())
    }
    private val mapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(module)


   private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build()
    }

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