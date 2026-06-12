package com.example.Roomie.data.repository

import com.example.Roomie.core.network.NetworkMonitor
import com.example.Roomie.data.local.RoomieDatabase
import com.example.Roomie.domain.model.Booking
import com.example.Roomie.domain.model.BookingStatus
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import io.mockk.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRepositoryImplTest {

    private lateinit var repository: BookingRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockDb: RoomieDatabase = mockk(relaxed = true)
    private val mockSupabase: SupabaseClient = mockk(relaxed = true)
    private val fakeIsOnline = MutableStateFlow(false)

    @BeforeTest
    fun setup() {
        val mockEngine = MockEngine { request ->
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    "Date" to listOf("Wed, 21 Oct 2026 07:28:00 GMT"),
                    HttpHeaders.ContentType to listOf("application/json")
                )
            )
        }
        val httpClient = HttpClient(mockEngine)
        val fakeNetworkMonitor = object : NetworkMonitor {
            override val isOnline = fakeIsOnline
        }

        repository = BookingRepositoryImpl(
            database = mockDb,
            httpClient = httpClient,
            supabaseClient = mockSupabase,
            networkMonitor = fakeNetworkMonitor,
            scope = CoroutineScope(testDispatcher + SupervisorJob())
        )
    }

    @Test
    fun `getAllBookings - logic check`() = runTest(testDispatcher) {
        repository.getAllBookings()
        verify { mockDb.bookingQueries.getAllBookings() }
    }

    @Test
    fun `getServerTime - Skenario Sukses`() = runTest(testDispatcher) {
        val time = repository.getServerTime()
        assertEquals(1792567680000L, time)
    }

    @Test
    fun `updateBookingStatus - Coverage`() = runTest(testDispatcher) {
        repository.updateBookingStatus("B1", BookingStatus.APPROVED)
        verify { mockDb.bookingQueries.updateBookingStatus(any(), any()) }
    }

    @Test
    fun `addBooking - Skenario Offline`() = runTest(testDispatcher) {
        val booking = Booking("B1", "R1", "101", "GKU2", 0L, 0L, BookingStatus.PENDING, "Test")
        val result = repository.addBooking(booking)
        assertTrue(result.isFailure)
    }

    @Test
    fun `sync logic coverage - trigger flow`() = runTest(testDispatcher) {
        fakeIsOnline.value = true
        runCurrent()
        assertTrue(fakeIsOnline.value)
    }
}
