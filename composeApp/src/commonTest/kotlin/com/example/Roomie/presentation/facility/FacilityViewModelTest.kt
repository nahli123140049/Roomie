package com.example.Roomie.presentation.facility

import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.Booking
import com.example.Roomie.domain.model.BookingStatus
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FacilityViewModelTest {
    private lateinit var viewModel: FacilityViewModel
    private lateinit var facilityRepository: FakeFacilityRepository
    private lateinit var bookingRepository: FakeBookingRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        facilityRepository = FakeFacilityRepository()
        bookingRepository = FakeBookingRepository()
        viewModel = FacilityViewModel(facilityRepository, bookingRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initBuilding should load rooms and update state to Success`() = runTest {
        val buildingId = "GKU2"
        val rooms = listOf(
            Room("1", buildingId, "101", 1, RoomStatus.AVAILABLE),
            Room("2", buildingId, "201", 2, RoomStatus.AVAILABLE)
        )
        facilityRepository.setRooms(rooms)
        
        viewModel.initBuilding(buildingId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is FacilityUiState.Success)
        assertEquals(2, state.rooms.size)
        assertEquals(1, state.filteredRooms.size) // default floor is 1
    }

    @Test
    fun `selectFloor should update filteredRooms`() = runTest {
        val buildingId = "GKU2"
        val rooms = listOf(
            Room("1", buildingId, "101", 1, RoomStatus.AVAILABLE),
            Room("2", buildingId, "201", 2, RoomStatus.AVAILABLE)
        )
        facilityRepository.setRooms(rooms)
        
        viewModel.initBuilding(buildingId)
        viewModel.selectFloor(2)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as FacilityUiState.Success
        assertEquals(2, state.selectedFloor)
        assertEquals(1, state.filteredRooms.size)
        assertEquals("2", state.filteredRooms[0].id)
    }

    @Test
    fun `room status should be BOOKED if there is an approved booking for selected date`() = runTest {
        val buildingId = "GKU2"
        val date = LocalDate(2026, 6, 11)
        val startOfDay = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        val rooms = listOf(Room("1", buildingId, "101", 1, RoomStatus.AVAILABLE))
        val bookings = listOf(
            Booking("B1", "1", "User1", "Study", startOfDay, startOfDay + 3600000, BookingStatus.APPROVED)
        )
        
        facilityRepository.setRooms(rooms)
        bookingRepository.setBookings(bookings)
        
        viewModel.initBuilding(buildingId)
        viewModel.selectDate(date)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as FacilityUiState.Success
        assertEquals(RoomStatus.BOOKED, state.rooms[0].status)
    }
}

// Extension to help with date
private fun LocalDate.atStartOfDayIn(tz: TimeZone) = 
    kotlinx.datetime.LocalDateTime(this, kotlinx.datetime.LocalTime(0, 0)).toInstant(tz)
