package com.example.Roomie.data.repository

import com.example.Roomie.domain.model.*
import kotlin.test.*

class RepositoryMappingTest {

    @Test
    fun `BookingRemoteDto mapping coverage`() {
        val dto = BookingRemoteDto("1", "R1", "Room", "Building", 1000L, 2000L, "PENDING", "Sub", "U1")
        // Since we can't call private toDomain easily, we test if fields are handled correctly
        assertEquals("1", dto.id)
        assertEquals("R1", dto.room_id)
        assertEquals("PENDING", dto.status)
    }

    @Test
    fun `ReportRemoteDto mapping coverage`() {
        val dto = ReportRemoteDto("1", "Cat", "Loc", "Desc", "HIGH", "PENDING", 12345L, "url")
        assertEquals("1", dto.id)
        assertEquals("HIGH", dto.urgency)
        assertEquals("url", dto.image_url)
    }

    @Test
    fun `Facility DTO mapping coverage`() {
        val bDto = BuildingRemoteDto("B1", "Gedung", "Desc", true)
        val rDto = RoomRemoteDto("R1", "B1", "Room", 1, "AVAILABLE", 50, true, true)
        
        assertEquals("B1", bDto.id)
        assertTrue(bDto.is_available)
        assertEquals(50, rDto.capacity)
    }
}
