package com.example.planmanagerapp.data.repository

import com.example.planmanagerapp.data.remote.api.CalendarApiService
import com.example.planmanagerapp.data.remote.dto.CalendarItemResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val apiService: CalendarApiService
) {
    suspend fun getCalendarItems(startDate: String, endDate: String): Result<List<CalendarItemResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCalendarItems(startDate, endDate)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}