package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.PlanCreateRequest
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import com.example.planmanagerapp.data.remote.dto.PlanSyncRequest
import com.example.planmanagerapp.data.remote.dto.ToggleRequest
import com.example.planmanagerapp.data.remote.dto.ToggleResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface PlanApiService {
    @GET("/api/v1/plans")
    suspend fun getAllPlans(): List<PlanResponse>

    @POST("/api/v1/plans")
    suspend fun createPlan(@Body request: PlanCreateRequest): Response<PlanResponse>

    // 409·404도 예외 없이 Response로 돌아오므로 호출하는 쪽에서 code()를 확인한다.
    @PATCH("/api/v1/plans/{planId}/toggle")
    suspend fun togglePlan(
        @Path("planId") planId: Long,
        @Body request: ToggleRequest
    ): Response<ToggleResponse>

    @DELETE("/api/v1/plans/{planId}")
    suspend fun deletePlan(@Path("planId") planId: Long): Response<Unit>

    // PlanSyncWorker에서 쓰는 동기 호출
    @POST("/api/v1/plans/sync")
    fun syncPlansSync(@Body requests: List<PlanSyncRequest>): Call<Void>
}