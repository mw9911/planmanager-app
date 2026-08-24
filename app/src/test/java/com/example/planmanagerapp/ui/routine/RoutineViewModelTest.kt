package com.example.planmanagerapp.ui.routine

import app.cash.turbine.test
import com.example.planmanagerapp.data.remote.dto.RoutineRecordResponse
import com.example.planmanagerapp.data.repository.RoutineRepository
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineViewModelTest {

    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: RoutineViewModel

    // 코루틴 지연 없이 즉시 실행시켜 구독(Collect)과 방출(Emit) 간의 타이밍 불일치를 방지합니다.
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
    fun `ViewModel 초기화 시 Repository의 DB 스트림을 구독하여 초기 상태에 반영한다`() = runTest {
        val recordsFlow = MutableSharedFlow<List<RoutineRecordResponse>>()
        every { repository.getRoutineRecordsStream(any()) } returns recordsFlow

        viewModel = RoutineViewModel(repository)

        viewModel.uiState.test {
            // 1. 초기 상태 검증
            val initialState = awaitItem()
            assertEquals(emptyList<RoutineRecordResponse>(), initialState.records)
            assertEquals(LocalDate.now().toString(), initialState.selectedDate)

            // 2. 로컬 DB에서 데이터가 방출(Emit)되었을 때의 상태 갱신 검증
            val mockData = listOf(RoutineRecordResponse(1L, 10L, "물 마시기", LocalDate.now().toString(), false))
            recordsFlow.emit(mockData)

            val updatedState = awaitItem()
            assertEquals(1, updatedState.records.size)
            assertEquals("물 마시기", updatedState.records[0].title)

            expectNoEvents()
        }
    }
    @Test
    fun `ChangeDate 인텐트 수신 시 선택된 날짜가 변경되고 서버 동기화가 실행된다`() = runTest {
        every { repository.getRoutineRecordsStream(any()) } returns flowOf(emptyList())
        coEvery { repository.refreshRoutineRecords(any()) } coAnswers { delay(100) }

        viewModel = RoutineViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // 1. 초기 상태 (init 완료 직후) 통과

            val targetDate = "2026-08-25"
            viewModel.handleIntent(RoutineIntent.ChangeDate(targetDate))

            // 💡 수정됨: UnconfinedTestDispatcher로 인해 '날짜 변경'과 '로딩 시작(true)' 상태가
            // 하나의 아이템으로 병합(Conflate)되어 방출되므로 이를 한 번에 검증합니다.
            val loadingState = awaitItem()
            assertEquals(targetDate, loadingState.selectedDate)
            assertEquals(true, loadingState.isLoading) // 로딩 시작 검증

            // 💡 수정됨: delay(100) 이후 로딩이 종료(false)된 상태를 검증합니다.
            val successState = awaitItem()
            assertEquals(false, successState.isLoading)

            // init 시점 1회 + ChangeDate 시점 1회 = 총 2회 호출 검증
            coVerify(exactly = 2) { repository.refreshRoutineRecords(any()) }
        }
    }
    @Test
    fun `ToggleRecord 통신 실패 시 에러 메시지가 상태에 반영된다(낙관적 업데이트 실패 롤백)`() = runTest {
        every { repository.getRoutineRecordsStream(any()) } returns flowOf(emptyList())
        coEvery { repository.toggleRecord(any(), any()) } coAnswers {
            delay(100)
            throw RuntimeException("Network Error")
        }

        viewModel = RoutineViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // 초기 상태 무시

            viewModel.handleIntent(RoutineIntent.ToggleRecord(recordId = 1L, currentStatus = false))

            // 에러 메시지 갱신 확인
            val errorState = awaitItem()
            assertEquals("네트워크 오류로 상태가 복구되었습니다.", errorState.errorMessage)
        }
    }
}