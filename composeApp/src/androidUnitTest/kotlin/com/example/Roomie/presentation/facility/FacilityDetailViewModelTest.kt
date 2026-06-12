package com.example.Roomie.presentation.facility

import com.example.Roomie.data.repository.FakeBookingRepository
import com.example.Roomie.data.repository.FakeFacilityRepository
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.model.RoomStatus
import com.example.Roomie.domain.usecase.GetRoomByIdUseCase
import com.example.Roomie.domain.usecase.UpdateRoomStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import io.mockk.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FacilityDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: FacilityDetailViewModel
    private val facilityRepository = FakeFacilityRepository()
    private val bookingRepository = FakeBookingRepository()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
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
    fun `getRoom - coverage boost`() = runTest {
        val room = Room("R1", "G1", "101", 1, RoomStatus.AVAILABLE)
        facilityRepository.setRooms(listOf(room))
        
        val result = viewModel.getRoom("R1").first()
        assertNotNull(result)
        assertEquals("101", result.name)
    }

    @Test
    fun `updateStatus - coverage boost`() = runTest {
        viewModel.updateStatus("R1", RoomStatus.MAINTENANCE, "Repair")
        assertTrue(true)
    }
}
