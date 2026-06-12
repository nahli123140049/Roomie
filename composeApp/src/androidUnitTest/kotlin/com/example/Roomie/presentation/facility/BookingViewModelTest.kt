package com.example.Roomie.presentation.facility

import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.*
import com.example.Roomie.domain.usecase.GetCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import io.mockk.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: BookingViewModel
    
    private val facilityRepository = FakeFacilityRepository()
    private val bookingRepository = FakeBookingRepository()
    private val mockGetCurrentUserUseCase = mockk<GetCurrentUserUseCase>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockGetCurrentUserUseCase() } returns flowOf(User("U1", "User", "121", UserRole.STUDENT))
        
        viewModel = BookingViewModel(
            bookingRepository,
            facilityRepository,
            mockGetCurrentUserUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRooms - coverage logic`() = runTest {
        val room = Room("R1", "G1", "101", 1, RoomStatus.AVAILABLE)
        facilityRepository.setRooms(listOf(room))
        
        viewModel.loadRooms(listOf("R1"))
        assertEquals(1, viewModel.state.value.rooms.size)
        assertEquals("101", viewModel.state.value.rooms.first().name)
    }

    @Test
    fun `onInputChanges - coverage logic`() {
        viewModel.onDateChange("20/10/2026")
        viewModel.onStartTimeChange("08:00")
        viewModel.onEndTimeChange("10:00")
        viewModel.onPurposeChange("Testing")
        
        val state = viewModel.state.value
        assertEquals("20/10/2026", state.date)
        assertEquals("08:00", state.startTime)
        assertEquals("Testing", state.purpose)
    }

    @Test
    fun `submitBooking - scenario validation error`() = runTest {
        viewModel.onDateChange("01/01/2020") // Past date
        viewModel.submitBooking()
        // With no rooms loaded, it just returns. Let's load room.
        val room = Room("R1", "G1", "101", 1, RoomStatus.AVAILABLE)
        facilityRepository.setRooms(listOf(room))
        viewModel.loadRooms(listOf("R1"))
        
        viewModel.onDateChange("01/01/2020")
        viewModel.submitBooking()
        assertNotNull(viewModel.state.value.error)
    }
}
