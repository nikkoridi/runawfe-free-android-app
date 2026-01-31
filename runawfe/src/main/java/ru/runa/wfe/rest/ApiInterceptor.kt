package ru.runa.wfe.rest

import okhttp3.Interceptor
import okhttp3.Response

class ApiInterceptor: Interceptor {
    // TODO: SocketTimeout Exception
    override fun intercept(chain: Interceptor.Chain): Response {
        val modifiedRequest = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${ApiClient.tokenManager.getToken()}")
            .build()
        return  chain.proceed(modifiedRequest)
    }
}