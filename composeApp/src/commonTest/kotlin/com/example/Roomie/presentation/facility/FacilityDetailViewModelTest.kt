package com.example.Roomie.presentation.facility

import app.cash.turbine.test
import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.Booking
import com.example.Roomie.domain.model.BookingStatus
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import com.example.Roomie.domain.usecase.GetRoomByIdUseCase
import com.example.Roomie.domain.usecase.UpdateRoomStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FacilityDetailViewModelTest {
    private lateinit var viewModel: FacilityDetailViewModel
    private lateinit var facilityRepository: FakeFacilityRepository
    private lateinit var bookingRepository: FakeBookingRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        facilityRepository = FakeFacilityRepository()
        bookingRepository = FakeBookingRepository()
        viewModel = FacilityDetailViewModel(
            GetRoomByIdUseCase(facilityRepository),
            UpdateRoomStatusUseCase(facilityRepository),
            bookingRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getRoom should return room with dynamic status`() = runTest {
        val dateStr = "2026-06-11"
        val date = LocalDate.parse(dateStr)
        val startOfDay = kotlinx.datetime.LocalDateTime(date, kotlinx.datetime.LocalTime(10, 0)).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        
        val room = Room("1", "GKU2", "101", 1, RoomStatus.AVAILABLE)
        val booking = Booking("B1", "1", "Room1", "GKU2", startOfDay, startOfDay + 3600000, BookingStatus.APPROVED)
        
        facilityRepository.setRooms(listOf(room))
        bookingRepository.setBookings(listOf(booking))
        
        viewModel.getRoom("1", dateStr).test {
            // Initial value is null
            assertEquals(null, awaitItem())
            // First emit from combine
            val item = awaitItem()
            assertEquals(RoomStatus.BOOKED, item?.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRoom should return null if room not found`() = runTest {
        viewModel.getRoom("999").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
