package com.example.Roomie.domain.usecase.ai

import com.example.Roomie.data.remote.ai.ExtractionResult
import com.example.Roomie.data.remote.ai.GeminiService
import com.example.Roomie.domain.model.Room
import com.example.Roomie.domain.repository.FacilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * UseCase untuk memproses input natural language menjadi hasil pencarian ruangan
 */
class ProcessAICommandUseCase(
    private val geminiService: GeminiService,
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(userInput: String): AIResult {
        // 1. Ekstrak data pake Gemini
        val extraction = geminiService.processUserCommand(userInput) ?: return AIResult.Error("Gagal memproses permintaan lo, bre. Coba lagi ya!")

        // 2. Validasi minimal data (Harus ada kapasitas atau tanggal)
        if (extraction.capacity == null && extraction.date == null) {
            return AIResult.Error("Datanya kurang lengkap nih, bre. Sebutin kapasitas atau tanggalnya ya.")
        }

        // 3. Search di database berdasarkan hasil AI
        // Kita pake filter kapasitas yang diekstrak AI
        val minCap = extraction.capacity ?: 0
        val maxCap = 100 // Default max
        
        val roomsFlow = facilityRepository.searchRoomsFiltered(
            query = extraction.buildingName ?: "",
            minCapacity = minCap,
            maxCapacity = maxCap
        )
        
        val rooms = roomsFlow.first()

        return AIResult.Success(
            extractedData = extraction,
            suggestedRooms = rooms
        )
    }
}

sealed interface AIResult {
    data class Success(
        val extractedData: ExtractionResult,
        val suggestedRooms: List<Room>
    ) : AIResult
    data class Error(val message: String) : AIResult
}
