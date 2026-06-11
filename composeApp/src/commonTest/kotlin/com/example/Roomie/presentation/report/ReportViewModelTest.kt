package com.example.Roomie.presentation.report

import com.example.Roomie.data.remote.SupabaseService
import com.example.Roomie.data.repository.FakeReportRepository
import com.example.Roomie.domain.model.UrgencyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {
    private lateinit var viewModel: ReportViewModel
    private lateinit var reportRepository: FakeReportRepository
    private val testDispatcher = StandardTestDispatcher()

    private class MockSupabaseService : SupabaseService {
        var urlToReturn: String? = "http://test.com/img.jpg"
        override suspend fun uploadReportImage(imageBytes: ByteArray): String? = urlToReturn
        override suspend fun uploadAvatar(imageBytes: ByteArray): String? = urlToReturn
    }
    
    private val supabaseService = MockSupabaseService()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        reportRepository = FakeReportRepository()
        viewModel = ReportViewModel(reportRepository, supabaseService)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty and submit disabled`() {
        val state = viewModel.state.value
        assertEquals("", state.category)
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `onCategoryChange should update state`() {
        viewModel.onCategoryChange("New Cat")
        assertEquals("New Cat", viewModel.state.value.category)
    }

    @Test
    fun `submitReport should succeed when fields are filled`() = runTest {
        viewModel.onCategoryChange("Category")
        viewModel.onLocationChange("Location")
        viewModel.onDescriptionChange("Description")
        viewModel.onUrgencyChange(UrgencyLevel.HIGH)
        
        assertTrue(viewModel.state.value.isSubmitEnabled)
        
        viewModel.submitReport()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isSubmitted)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, reportRepository.getAllReports().first().size)
    }

    @Test
    fun `submitReport should fail if upload fails`() = runTest {
        viewModel.onCategoryChange("Cat")
        viewModel.onLocationChange("Loc")
        viewModel.onDescriptionChange("Desc")
        viewModel.onImagePicked(byteArrayOf(1, 2, 3))
        
        supabaseService.urlToReturn = null
        
        viewModel.submitReport()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isSubmitted)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `resetState should clear the form`() {
        viewModel.onCategoryChange("Cat")
        viewModel.resetState()
        assertEquals("", viewModel.state.value.category)
    }
}
