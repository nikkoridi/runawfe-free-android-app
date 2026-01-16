package ru.runa.wfe.rest.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import ru.runa.wfe.restapi.model.WfePagedListFilter
import ru.runa.wfe.restapi.model.WfePagedListOfWfeTask
import ru.runa.wfe.restapi.model.WfeTask

interface TaskApiService {

    @POST("task/my/")
    suspend fun getMyTasks(@Body wfePagedListFilter: WfePagedListFilter): Response<WfePagedListOfWfeTask>

    @POST("task/process/{processId}/")
    suspend fun getProcessTasks(@Path("processId") processId: Long): Response<List<WfeTask>>

    @POST("task/{id}/")
    suspend fun getTask(@Path("id") id: Long): Response<List<WfeTask>>
}