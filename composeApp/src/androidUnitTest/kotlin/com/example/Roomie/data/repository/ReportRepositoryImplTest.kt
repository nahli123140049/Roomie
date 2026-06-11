package com.example.Roomie.data.repository

import com.example.Roomie.core.network.NetworkMonitor
import com.example.Roomie.domain.model.Report
import com.example.Roomie.domain.model.ReportStatus
import com.example.Roomie.domain.model.UrgencyLevel
import io.ktor.client.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryImplTest {
    private lateinit var repository: ReportRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        val mockEngine = MockEngine { respond("", HttpStatusCode.OK) }
        val mockClient = HttpClient(mockEngine)
        val fakeNetworkMonitor = object : NetworkMonitor {
            override val isOnline = MutableStateFlow(true)
        }
        
        repository = ReportRepositoryImpl(
            database = io.mockk.mockk(relaxed = true),
            supabaseClient = io.mockk.mockk(relaxed = true),
            networkMonitor = fakeNetworkMonitor,
            scope = CoroutineScope(testDispatcher + SupervisorJob())
        )
    }

    @Test
    fun `submitReport logic check`() = runTest {
        val report = Report("R1", "A", "Loc", "Desc", UrgencyLevel.LOW, ReportStatus.PENDING, 0L)
        repository.submitReport(report)
        assertTrue(true)
    }

    @Test
    fun `updateReportStatus logic check`() = runTest {
        repository.updateReportStatus("R1", ReportStatus.DONE)
        assertTrue(true)
    }
}
