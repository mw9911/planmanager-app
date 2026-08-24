package com.example.planmanagerapp.ui.plan

import app.cash.turbine.test
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import com.example.planmanagerapp.data.repository.PlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private lateinit var repository: PlanRepository
    private lateinit var viewModel: PlanViewModel

    // 💡 핵심 1: 코루틴을 대기열에 넣지 않고 즉시 실행시켜 구독(Collect) 유실을 방지합니다.
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel 초기화 시 Repository의 DB 스트림을 구독하여 상태에 반영한다`() = runTest {
        val plansFlow = MutableSharedFlow<List<PlanResponse>>()
        every { repository.getPlansStream(any()) } returns plansFlow

        // UnconfinedTestDispatcher 덕분에 생성 즉시 init 블록의 collect가 활성화됩니다.
        viewModel = PlanViewModel(repository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(emptyList<PlanResponse>(), initialState.plans)

            val mockData = listOf(PlanResponse(1L, "오프라인 계획", "2026-08-23", false))
            // 구독자가 준비된 상태이므로 데이터가 유실되지 않고 정상 전달됩니다.
            plansFlow.emit(mockData)

            val updatedState = awaitItem()
            assertEquals(1, updatedState.plans.size)
            assertEquals("오프라인 계획", updatedState.plans[0].title)

            expectNoEvents()
        }
    }

    @Test
    fun `FetchPlans 인텐트 수신 시 로딩 상태가 변경되고 동기화 로직이 실행된다`() = runTest {
        every { repository.getPlansStream(any()) } returns flowOf(emptyList())

        // 💡 핵심 2: 즉시 실행 환경에서 StateFlow가 중간 상태(Loading=true)를 무시하고
        // 최종 결과만 방출(Conflate)하는 것을 막기 위해 강제 suspension(delay) 부여
        coEvery { repository.refreshPlans(any()) } coAnswers {
            delay(100)
        }

        viewModel = PlanViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // 초기 상태 무시

            viewModel.handleIntent(PlanIntent.FetchPlans)

            // 로딩 시작 검증
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            // 로딩 종료 검증
            val successState = awaitItem()
            assertEquals(false, successState.isLoading)

            coVerify(exactly = 1) { repository.refreshPlans(any()) }
        }
    }

    @Test
    fun `TogglePlan 실패 시 에러 메시지가 상태에 반영된다`() = runTest {
        every { repository.getPlansStream(any()) } returns flowOf(emptyList())

        coEvery { repository.togglePlan(any(), any()) } coAnswers {
            delay(100)
            throw RuntimeException("Network Error")
        }

        viewModel = PlanViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // 초기 상태 무시

            viewModel.handleIntent(PlanIntent.TogglePlan(id = 1L, currentStatus = false))

            val errorState = awaitItem()
            assertEquals("네트워크 오류로 상태가 복구되었습니다.", errorState.errorMessage)
        }
    }
}