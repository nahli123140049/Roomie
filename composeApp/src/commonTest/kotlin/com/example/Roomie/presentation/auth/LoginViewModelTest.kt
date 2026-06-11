package com.example.Roomie.presentation.auth

import com.example.Roomie.data.repository.FakeAuthRepository
import com.example.Roomie.domain.usecase.LoginUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private lateinit var viewModel: LoginViewModel
    private lateinit var authRepository: FakeAuthRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(LoginUseCase(authRepository))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() {
        val state = viewModel.state.value
        assertEquals("", state.idNumber)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.loginSuccess)
    }

    @Test
    fun `onIdNumberChange should update state`() {
        viewModel.onIdNumberChange("121140019")
        assertEquals("121140019", viewModel.state.value.idNumber)
    }

    @Test
    fun `login with empty NIM should show error`() {
        viewModel.onIdNumberChange("")
        viewModel.login()
        assertEquals("NIM/NIP tidak boleh kosong", viewModel.state.value.error)
    }

    @Test
    fun `login with invalid format should show error`() {
        viewModel.onIdNumberChange("123") // too short for NIM, too short for NIP (actually 3-5 is valid for NIP in VM)
        // Wait, the VM says: val nipRegex = Regex("^[0-9]{3,5}$")
        // So 123 is valid NIP.
        
        viewModel.onIdNumberChange("12") // invalid
        viewModel.login()
        assertEquals("Format NIM (9 digit) atau NIP tidak valid", viewModel.state.value.error)
    }

    @Test
    fun `login with valid NIM should succeed`() = runTest {
        viewModel.onIdNumberChange("121140019")
        viewModel.login()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.loginSuccess)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `login with valid NIP should succeed`() = runTest {
        viewModel.onIdNumberChange("12345")
        viewModel.login()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.state.value.loginSuccess)
    }
}
