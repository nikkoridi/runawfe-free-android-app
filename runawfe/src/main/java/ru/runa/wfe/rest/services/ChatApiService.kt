package ru.runa.wfe.rest.services

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.runa.wfe.rest.dto.WfChatRoom

interface ChatApiService {

    @GET
    suspend fun getChatRooms(): Response<Collection<WfChatRoom>>

    @GET("chat/{processId}")
    suspend fun getChatMessages(@Path("processId") processId: Long)
}