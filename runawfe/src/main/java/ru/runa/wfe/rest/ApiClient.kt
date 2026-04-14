package ru.runa.wfe.rest

import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object ApiClient {
    private var BASE_URL: String = "http://10.0.2.2:8080/"

    private lateinit var basicApiClient: ApiClient

    private val okHttpClientBuilder = OkHttpClient.Builder()
        .callTimeout(1, TimeUnit.MINUTES)

    private val mapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModules(JavaTimeModule())
        .registerKotlinModule()
        .findAndRegisterModules()
        .addMixIn(MessageAddedBroadcast::class.java, MessageAddedBroadcastMixin::class.java)


    fun isApiClientInitialized(): Boolean = this::basicApiClient.isInitialized


    fun setServerUrl(checkedUrl: ServerCheckResult) {
        CoroutineScope(Dispatchers.IO).launch {
            if (checkedUrl is ServerCheckResult.Valid) {
                BASE_URL = checkedUrl.baseUrl
                initBasicApiClient()
            } else {
                Log.e(this::class.simpleName, "Given URL is not a RunaWFE server")
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
            var url = url.trim()
            if (!(url.startsWith("https://") ||
                url.startsWith("http://"))) {
                url = "http://$url"
            }
            val baseUrl = Uri.parse(url)
            val port = if (baseUrl.port != -1) ":${baseUrl.port}" else ""
            if (baseUrl.host != null) {
                return "${baseUrl.scheme}://${baseUrl.host}$port"
            }
        } catch (e: Exception) {
            Log.e("API Client", "Invalid URL")
        }
        return ""
    }

    suspend fun checkServer(url: String): ServerCheckResult = withContext(Dispatchers.IO) {
        val clearBaseUrl = getBaseUrl(url)
        if (clearBaseUrl.isEmpty()) {
            return@withContext ServerCheckResult.Invalid
        }
        val versionRequest = Request.Builder()
            .url("${clearBaseUrl}/wfe/version")
            .get()
            .build()
        return@withContext try {
            val response = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()
                .newCall(versionRequest)
                .execute()
            return@withContext if (!response.isSuccessful) {
                Log.e(this::class.simpleName, "Failed server check: ${response.code}")
                ServerCheckResult.Invalid
            } else if (response.body?.string().isNullOrBlank()) {
                Log.e(this::class.simpleName, "Failed server check: empty response")
                ServerCheckResult.Invalid
            } else {
                ServerCheckResult.Valid(clearBaseUrl)
            }
        } catch (ex: Exception) {
            when (ex) {
                is SocketTimeoutException, is IOException -> {
                    Log.e(this::class.simpleName, "Network exception: ${ex.message.toString()}")
                    return@withContext ServerCheckResult.NetworkError
                }
                else -> {
                    Log.e(this::class.simpleName, "Failed server check: ${ex.message.toString()}")
                    return@withContext ServerCheckResult.Invalid
                }
            }
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

sealed class ServerCheckResult {
    data class Valid(val baseUrl: String) : ServerCheckResult()
    data object Invalid : ServerCheckResult()
    data object NetworkError : ServerCheckResult()
}