package com.example.Roomie.data.repository

import com.example.Roomie.core.network.NetworkMonitor
import com.example.Roomie.domain.model.Booking
import com.example.Roomie.domain.model.BookingStatus
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRepositoryImplTest {

    private lateinit var repository: BookingRepositoryImpl
    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        // 1. Mock Ktor Engine buat tes NTP Sync
        mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf("Date", "Wed, 21 Oct 2026 07:28:00 GMT")
            )
        }

        httpClient = HttpClient(mockEngine)

        // 2. Fake Network Monitor
        val fakeNetworkMonitor = object : NetworkMonitor {
            override val isOnline = MutableStateFlow(true)
        }

        // 3. Kita pake repository asli tapi dependensi mock
        // Note: mockDb dan supabaseClient dipassing as mockk(relaxed=true) atau fake manual
        repository = BookingRepositoryImpl(
            database = io.mockk.mockk(relaxed = true),
            httpClient = httpClient,
            supabaseClient = io.mockk.mockk(relaxed = true),
            networkMonitor = fakeNetworkMonitor,
            scope = CoroutineScope(testDispatcher + SupervisorJob())
        )
    }

    @Test
    fun `getServerTime - Skenario Sukses - Parse Header Date`() = runTest {
        val time = repository.getServerTime()
        // Hasil parse dari "Wed, 21 Oct 2026 07:28:00 GMT"
        assertEquals(1792567680000L, time)
    }

    @Test
    fun `getServerTime - Skenario Gagal - Fallback ke Local Time`() = runTest {
        // Buat engine yang error
        val errorEngine = MockEngine { throw Exception("Network Fail") }
        val errorClient = HttpClient(errorEngine)
        
        val repoError = BookingRepositoryImpl(
            io.mockk.mockk(relaxed = true),
            errorClient,
            io.mockk.mockk(relaxed = true),
            io.mockk.mockk(relaxed = true),
            CoroutineScope(testDispatcher)
        )

        val time = repoError.getServerTime()
        assertTrue(time > 0) // Harusnya tetep dapet waktu (local fallback)
    }
}
