package ru.runa.wfe.rest.services

import androidx.paging.PagingData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import ru.runa.wfe.rest.dto.WfePagedList
import ru.runa.wfe.rest.dto.WfeTask

interface TaskApiService {

    @POST("task/my/")
    suspend fun getMyTasks(@Body pagedList: PagingData<WfeTask>): Response<WfePagedList<WfeTask>>

    @POST("task/process/{processId}/")
    suspend fun getProcessTasks(@Path("processId") processId: Long): Response<List<WfeTask>>

    @POST("task/{id}/")
    suspend fun getTask(@Path("id") id: Long): Response<List<WfeTask>>
}