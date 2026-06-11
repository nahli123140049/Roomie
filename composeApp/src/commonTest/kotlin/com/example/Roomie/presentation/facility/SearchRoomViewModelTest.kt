package com.example.Roomie.presentation.facility

import app.cash.turbine.test
import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.Booking
import com.example.Roomie.domain.model.BookingStatus
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import com.example.Roomie.domain.usecase.SearchRoomsFilteredUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRoomViewModelTest {
    private lateinit var viewModel: SearchRoomViewModel
    private lateinit var facilityRepository: FakeFacilityRepository
    private lateinit var bookingRepository: FakeBookingRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        facilityRepository = FakeFacilityRepository()
        bookingRepository = FakeBookingRepository()
        viewModel = SearchRoomViewModel(
            SearchRoomsFilteredUseCase(facilityRepository),
            bookingRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchResults should filter by query after debounce`() = runTest {
        val rooms = listOf(
            Room("1", "GKU2", "A101", 1, RoomStatus.AVAILABLE),
            Room("2", "GKU2", "B201", 1, RoomStatus.AVAILABLE)
        )
        facilityRepository.setRooms(rooms)
        
        viewModel.searchResults.test {
            assertEquals(emptyList(), awaitItem()) // Initial value
            
            viewModel.onQueryChange("A1")
            
            // Advance time for debounce (300ms)
            testDispatcher.scheduler.advanceTimeBy(400)
            testDispatcher.scheduler.runCurrent()
            
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("1", results[0].id)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCapacityFilterChange should update results`() = runTest {
        val rooms = listOf(
            Room("1", "GKU2", "A101", 1, RoomStatus.AVAILABLE, capacity = 10),
            Room("2", "GKU2", "B201", 1, RoomStatus.AVAILABLE, capacity = 100)
        )
        facilityRepository.setRooms(rooms)
        
        viewModel.searchResults.test {
            awaitItem() // emptyList
            
            // Advance for initial combine (debounce 300ms)
            testDispatcher.scheduler.advanceTimeBy(400)
            assertEquals(2, awaitItem().size)
            
            viewModel.onCapacityFilterChange(50, 200)
            
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("2", results[0].id)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
