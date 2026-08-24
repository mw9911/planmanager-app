package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.CalendarItemResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApiService {
    @GET("/api/v1/calendar")
    suspend fun getCalendarItems(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): List<CalendarItemResponse>
}