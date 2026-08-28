package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.PlanCreateRequest
import com.example.planmanagerapp.data.remote.dto.PlanResponse
// 💡 PlanSyncRequest DTO 임포트 (본인의 DTO 파일명/경로와 일치하는지 확인하십시오)
import com.example.planmanagerapp.data.remote.dto.PlanSyncRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface PlanApiService {
    @GET("/api/v1/plans")
    suspend fun getAllPlans(): List<PlanResponse>

    @POST("/api/v1/plans")
    suspend fun createPlan(@Body request: PlanCreateRequest): Response<PlanResponse>

    @PATCH("/api/v1/plans/{planId}/toggle")
    suspend fun togglePlan(@Path("planId") planId: Long): Response<Unit>

    @DELETE("/api/v1/plans/{planId}")
    suspend fun deletePlan(@Path("planId") planId: Long): Response<Unit>

    // 💡 에러 해결: PlanSyncWorker가 사용할 동기식 일괄 동기화 API 호출 정의
    @POST("/api/v1/plans/sync")
    fun syncPlansSync(@Body requests: List<PlanSyncRequest>): Call<Void>
}