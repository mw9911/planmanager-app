package com.example.planmanagerapp.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.planmanagerapp.data.remote.api.RoutineApiService
import com.example.planmanagerapp.data.remote.dto.RoutineGroupCreateRequest
import com.example.planmanagerapp.data.remote.dto.RoutineGroupResponse
import com.example.planmanagerapp.data.remote.dto.RoutineItemCreateRequest
import com.example.planmanagerapp.data.remote.dto.RoutineSyncRequest
import com.example.planmanagerapp.data.remote.dto.SyncResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

// 1. MockK 대신 사용할 Fake(가짜) 서비스 객체 생성
class FakeRoutineApiService : RoutineApiService {
    // 워커 테스트에서 호출되는 핵심 메서드: 무조건 성공 반환
    override suspend fun syncRoutines(request: RoutineSyncRequest): SyncResponse {
        return SyncResponse(success = true, message = "Fake 동기화 완료")
    }

    // 아래 메서드들은 워커 테스트에서 사용되지 않으므로 임의의 값 반환
    override suspend fun getRoutines(): List<RoutineGroupResponse> = emptyList()
    override suspend fun createRoutineGroup(request: RoutineGroupCreateRequest): Response<Unit> = Response.success(Unit)
    override suspend fun addRoutineItem(groupId: Long, request: RoutineItemCreateRequest): Response<Unit> = Response.success(Unit)
    override suspend fun toggleItem(itemId: Long): Response<Unit> = Response.success(Unit)
    override suspend fun deleteRoutineGroup(groupId: Long): Response<Unit> = Response.success(Unit)
    override suspend fun deleteRoutineItem(itemId: Long): Response<Unit> = Response.success(Unit)
}

@RunWith(AndroidJUnit4::class)
class RoutineSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeApiService: RoutineApiService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 2. Fake 객체 초기화
        fakeApiService = FakeRoutineApiService()
    }

    @Test
    fun testWorkerSyncSuccess() = runBlocking {
        // given: 워커 빌드 및 Fake API 서비스 주입
        val worker = TestListenableWorkerBuilder<RoutineSyncWorker>(context).build()
        worker.apiService = fakeApiService

        // when: 백그라운드 작업 수행
        val result = worker.doWork()

        // then: 성공 결과 반환 검증
        assertEquals(ListenableWorker.Result.success(), result)
    }
}