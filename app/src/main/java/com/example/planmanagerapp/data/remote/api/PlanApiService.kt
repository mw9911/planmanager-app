package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.PlanCreateRequest
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import retrofit2.Response
import retrofit2.http.*

interface PlanApiService {
    // 💡 Query 파라미터 제거
    @GET("/api/v1/plans")
    suspend fun getAllPlans(): List<PlanResponse>

    @POST("/api/v1/plans")
    suspend fun createPlan(@Body request: PlanCreateRequest): Response<PlanResponse>

    @PATCH("/api/v1/plans/{planId}/toggle")
    suspend fun togglePlan(@Path("planId") planId: Long): Response<Unit>

    @DELETE("/api/v1/plans/{planId}")
    suspend fun deletePlan(@Path("planId") planId: Long): Response<Unit>
}