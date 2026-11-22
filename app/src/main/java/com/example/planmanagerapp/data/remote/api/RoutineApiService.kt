package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface RoutineApiService {
    @GET("/api/v1/routines")
    suspend fun getRoutineGroups(@Query("date") date: String): Response<List<RoutineGroupResponse>>
    @POST("/api/v1/routines/groups")
    suspend fun createGroup(@Body request: RoutineGroupCreateRequest): Response<RoutineGroupResponse>

    @POST("/api/v1/routines/groups/{groupId}/items")
    suspend fun addItem(@Path("groupId") groupId: Long, @Body request: RoutineItemCreateRequest): Response<RoutineItemResponse>

    @PATCH("/api/v1/routines/items/{itemId}/toggle")
    suspend fun toggleItem(@Path("itemId") itemId: Long): Response<Unit>

    @DELETE("/api/v1/routines/groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: Long): Response<Unit>

    @DELETE("/api/v1/routines/items/{itemId}")
    suspend fun deleteItem(@Path("itemId") itemId: Long): Response<Unit>


}