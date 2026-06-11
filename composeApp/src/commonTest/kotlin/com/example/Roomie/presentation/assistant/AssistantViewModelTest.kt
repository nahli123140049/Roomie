package com.example.Roomie.presentation.assistant

import com.example.Roomie.data.remote.ai.ExtractionResult
import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import com.example.Roomie.domain.usecase.ai.AIResult
import com.example.Roomie.domain.usecase.ai.ProcessAICommandUseCase
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class
AssistantViewModelTest {
    private lateinit var viewModel: AssistantViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    private class FakeProcessAICommandUseCase : ProcessAICommandUseCase(
        com.example.Roomie.data.remote.ai.GeminiService(HttpClient(), Json {}),
        FakeFacilityRepository(),
        FakeBookingRepository()
    ) {
        var resultToReturn: AIResult = AIResult.Error("Default")
        override suspend fun invoke(userInput: String): AIResult = resultToReturn
    }
    
    private val fakeUseCase = FakeProcessAICommandUseCase()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AssistantViewModel(fakeUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have greeting message`() {
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertFalse(state.messages[0].isUser)
    }

    @Test
    fun `sendCommand should add user message and then AI response`() = runTest {
        val query = "cari ruangan"
        fakeUseCase.resultToReturn = AIResult.Success(
            extractedData = ExtractionResult(buildingName = "GKU2"),
            suggestedRooms = listOf(Room("1", "GKU2", "101", 1, RoomStatus.AVAILABLE, capacity = 50))
        )
        
        viewModel.onQueryChange(query)
        viewModel.sendCommand()
        
        // Check user message added immediately
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertTrue(viewModel.uiState.value.messages[1].isUser)
        assertTrue(viewModel.uiState.value.isLoading)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check AI message added
        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertFalse(state.messages[2].isUser)
        assertFalse(state.isLoading)
        assertEquals(1, state.suggestedRooms.size)
        assertTrue(state.messages[2].text.contains("nemu 1 ruangan"))
    }

    @Test
    fun `sendCommand should handle error from AI`() = runTest {
        val query = "error query"
        fakeUseCase.resultToReturn = AIResult.Error("Waduh gagal")
        
        viewModel.onQueryChange(query)
        viewModel.sendCommand()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertEquals("Waduh gagal", state.messages[2].text)
    }

    @Test
    fun `resetAssistant should return to initial state`() = runTest {
        viewModel.onQueryChange("test")
        viewModel.sendCommand()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.resetAssistant()
        
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("", state.currentQuery)
        assertTrue(state.suggestedRooms.isEmpty())
    }
}

