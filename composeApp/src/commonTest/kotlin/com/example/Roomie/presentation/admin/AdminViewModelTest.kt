package com.example.Roomie.presentation.admin

import com.example.Roomie.data.repository.*
import com.example.Roomie.domain.model.*
import com.example.Roomie.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var facilityRepository: FakeFacilityRepository
    private lateinit var bookingRepository: FakeBookingRepository
    private lateinit var announcementRepository: FakeAnnouncementRepository
    
    private lateinit var viewModel: AdminViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        reportRepository = FakeReportRepository()
        facilityRepository = FakeFacilityRepository()
        bookingRepository = FakeBookingRepository()
        announcementRepository = FakeAnnouncementRepository()
        
        val notificationRepository = object : com.example.Roomie.domain.repository.NotificationRepository {
            override fun getAllNotifications() = flowOf(emptyList<Notification>())
            override suspend fun addNotification(notification: Notification) {}
            override suspend fun markAsRead(id: String) {}
        }
        
        val auditRepository = object : com.example.Roomie.domain.repository.AuditRepository {
            private val logs = mutableListOf<AuditLog>()
            override fun getAuditLogs() = flowOf(logs)
            override suspend fun addAuditLog(log: AuditLog): Result<Unit> {
                logs.add(log)
                return Result.success(Unit)
            }
        }

        viewModel = AdminViewModel(
            getAllReportsUseCase = GetAllReportsUseCase(reportRepository),
            updateReportStatusUseCase = UpdateReportStatusUseCase(reportRepository),
            updateRoomStatusUseCase = UpdateRoomStatusUseCase(facilityRepository),
            postAnnouncementUseCase = PostAnnouncementUseCase(announcementRepository),
            getBuildingsUseCase = GetBuildingsUseCase(facilityRepository),
            searchRoomsUseCase = SearchRoomsUseCase(facilityRepository),
            getAllBookingsUseCase = GetAllBookingsUseCase(bookingRepository),
            getAuditLogsUseCase = GetAuditLogsUseCase(auditRepository),
            addAuditLogUseCase = AddAuditLogUseCase(auditRepository),
            bookingRepository = bookingRepository,
            notificationRepository = notificationRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load should set Success state with data`() = runTest {
        val rooms = listOf(Room("1", "GKU2", "101", 1, RoomStatus.AVAILABLE))
        facilityRepository.setRooms(rooms)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is AdminUiState.Success)
        assertEquals(1, (state as AdminUiState.Success).rooms.size)
    }

    @Test
    fun `onReportSearch should filter results correctly`() = runTest {
        val reports = listOf(
            Report("1", "A", "Gedung A", "Rusak AC", UrgencyLevel.HIGH, ReportStatus.PENDING, 0L),
            Report("2", "B", "Gedung B", "Bocor", UrgencyLevel.LOW, ReportStatus.PENDING, 0L)
        )
        reportRepository.setReports(reports)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onReportSearch("AC")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as AdminUiState.Success
        assertEquals(1, state.filteredReports.size)
        assertTrue(state.filteredReports[0].description.contains("AC"))
    }

    @Test
    fun `onReportStatusFilter should update state`() = runTest {
        val reports = listOf(
            Report("1", "A", "L1", "D1", UrgencyLevel.LOW, ReportStatus.PENDING, 0),
            Report("2", "B", "L2", "D2", UrgencyLevel.LOW, ReportStatus.DONE, 0)
        )
        reportRepository.setReports(reports)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onReportStatusFilter(ReportStatus.DONE)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value as AdminUiState.Success
        assertEquals(1, state.filteredReports.size)
        assertEquals(ReportStatus.DONE, state.filteredReports[0].status)
    }

    @Test
    fun `approveBooking should update status and add audit log`() = runTest {
        val booking = Booking("B1", "R1", "Room1", "GKU2", 0, 1000, BookingStatus.PENDING, subject = "Study")
        bookingRepository.setBookings(listOf(booking))
        
        viewModel.approveBooking(booking)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val bookings = bookingRepository.getAllBookings().first()
        assertEquals(BookingStatus.APPROVED, bookings[0].status)
        
        val state = viewModel.uiState.value as AdminUiState.Success
        assertTrue(state.auditLogs.any { it.action == "APPROVED" })
    }

    @Test
    fun `rejectBooking should update status`() = runTest {
        val booking = Booking("B1", "R1", "Room1", "GKU2", 0, 1000, BookingStatus.PENDING, subject = "Study")
        bookingRepository.setBookings(listOf(booking))
        
        viewModel.rejectBooking(booking)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val bookings = bookingRepository.getAllBookings().first()
        assertEquals(BookingStatus.REJECTED, bookings[0].status)
    }

    @Test
    fun `overrideRoomStatus should update room and add audit log`() = runTest {
        val room = Room("1", "GKU2", "101", 1, RoomStatus.AVAILABLE)
        facilityRepository.setRooms(listOf(room))
        
        viewModel.overrideRoomStatus("1", RoomStatus.MAINTENANCE, "Broken door")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val updatedRoom = facilityRepository.getRoomById("1").first()
        assertEquals(RoomStatus.MAINTENANCE, updatedRoom?.status)
        assertEquals("Broken door", updatedRoom?.maintenanceDescription)
        
        val state = viewModel.uiState.value as AdminUiState.Success
        assertTrue(state.auditLogs.any { it.action == "OVERRIDE_MAINTENANCE" })
    }

    @Test
    fun `broadcastMessage should trigger use case`() = runTest {
        viewModel.broadcastMessage("Info", "Test Pesan")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val announcements = announcementRepository.getAllAnnouncements().first()
        assertTrue(announcements.any { it.title == "Info" })
    }
}
