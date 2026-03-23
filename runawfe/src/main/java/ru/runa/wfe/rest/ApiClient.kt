package ru.runa.wfe.rest

import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import ru.runa.wfe.restapi.client.AuthControllerApi
import ru.runa.wfe.restapi.client.ChatControllerApi
import ru.runa.wfe.restapi.client.TaskControllerApi
import ru.runa.wfe.restapi.infrastructure.ApiClient
import java.util.concurrent.TimeUnit

object ApiClient {
    private var BASE_URL: String = "http://10.0.2.2:8080/"

    private var basicApiClient: ApiClient = ApiClient()

    private val okHttpClientBuilder = OkHttpClient.Builder()
        .callTimeout(1, TimeUnit.MINUTES)

    private val mapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModules(JavaTimeModule())
        .findAndRegisterModules()

    fun setServerUrl(url: String) {
        val baseUrl = getBaseUrl(url)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (checkServer(baseUrl)) {
                    BASE_URL = baseUrl
                    initBasicApiClient()
                } else {
                    Log.e(this::class.simpleName, "Given URL is not a RunaWFE server")
                }
            } catch (ex: Exception) {
                Log.e(this::class.simpleName, ex.message.toString())
            }
        }
    }

    private fun initBasicApiClient() {
        basicApiClient = ApiClient(
            BASE_URL,
            okHttpClientBuilder,
            mapper,
            null,
            listOf(),
            listOf(JacksonConverterFactory.create(mapper))
        )
        basicApiClient.addAuthorization("token", ApiInterceptor())
    }

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
            val response = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.MILLISECONDS)
                .build()
                .newCall(versionRequest)
                .execute()
            !response.body?.string().isNullOrBlank()
        }
    }

    val authService: AuthControllerApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClientBuilder.build())
            // Jackson doesn't work well with text/plain responses
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthControllerApi::class.java)
    }

    val chatService: ChatControllerApi by lazy {
        basicApiClient.createService(ChatControllerApi::class.java)
    }

    val taskService: TaskControllerApi by lazy {
        basicApiClient.createService(TaskControllerApi::class.java)
    }
}