package com.example.Roomie.presentation.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.Roomie.data.local.datastore.UserPreferences
import com.example.Roomie.data.remote.SupabaseService
import com.example.Roomie.data.repository.FakeAuthRepository
import com.example.Roomie.data.repository.FakeReportRepository
import com.example.Roomie.domain.model.Report
import com.example.Roomie.domain.model.ReportStatus
import com.example.Roomie.domain.model.UrgencyLevel
import com.example.Roomie.domain.usecase.GetAllReportsUseCase
import com.example.Roomie.domain.usecase.GetCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    private class MockSupabaseService : SupabaseService {
        var urlToReturn: String? = "http://avatar.jpg"
        override suspend fun uploadReportImage(imageBytes: ByteArray): String? = null
        override suspend fun uploadAvatar(imageBytes: ByteArray): String? = urlToReturn
    }
    
    private val supabaseService = MockSupabaseService()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        reportRepository = FakeReportRepository()
        authRepository = FakeAuthRepository()
        
        val mockDataStore = object : DataStore<Preferences> {
            private val _data = MutableStateFlow(emptyPreferences())
            override val data = _data.asStateFlow()
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                val next = transform(_data.value)
                _data.value = next
                return next
            }
        }
        userPreferences = UserPreferences(mockDataStore)

        viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(authRepository),
            getAllReportsUseCase = GetAllReportsUseCase(reportRepository),
            userPreferences = userPreferences,
            supabaseService = supabaseService
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stats should accurately reflect report status distribution`() = runTest {
        val reports = listOf(
            Report("1", "A", "L1", "D1", UrgencyLevel.LOW, ReportStatus.DONE, 0),
            Report("2", "B", "L2", "D2", UrgencyLevel.LOW, ReportStatus.DONE, 0),
            Report("3", "C", "L3", "D3", UrgencyLevel.LOW, ReportStatus.IN_PROGRESS, 0),
            Report("4", "D", "L4", "D4", UrgencyLevel.LOW, ReportStatus.PENDING, 0)
        )
        reportRepository.setReports(reports)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        val successState = state as ProfileUiState.Success
        assertEquals(2, successState.stats[ReportStatus.DONE])
        assertEquals(1, successState.stats[ReportStatus.IN_PROGRESS])
        assertEquals(1, successState.stats[ReportStatus.PENDING])
    }

    @Test
    fun `reports in Success state should be in reverse order`() = runTest {
        val reports = listOf(
            Report("1", "A", "L1", "D1", UrgencyLevel.LOW, ReportStatus.PENDING, 100),
            Report("2", "B", "L2", "D2", UrgencyLevel.LOW, ReportStatus.PENDING, 200)
        )
        reportRepository.setReports(reports)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertEquals("2", state.reports[0].id)
        assertEquals("1", state.reports[1].id)
    }

    @Test
    fun `updateAvatar should upload and save to preferences`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle() // Load initial success
        
        viewModel.updateAvatar(byteArrayOf(1, 2, 3))
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as ProfileUiState.Success
        assertEquals("http://avatar.jpg", state.avatarUrl)
    }
}
