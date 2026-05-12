package com.autosre.gateway.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DtoTest {

    @Test
    @DisplayName("IncidentDto can be created with all fields")
    void incidentDtoCreation() {
        Instant now = Instant.now();
        IncidentDto dto = new IncidentDto(
                UUID.randomUUID(),
                "api-service",
                com.autosre.common.model.Severity.HIGH,
                "High memory usage",
                "OPEN",
                now,
                null
        );

        assertNotNull(dto.id());
        assertEquals("api-service", dto.serviceId());
        assertEquals(com.autosre.common.model.Severity.HIGH, dto.severity());
        assertEquals("OPEN", dto.status());
    }

    @Test
    @DisplayName("RecommendationDto can be created with actions")
    void recommendationDtoCreation() {
        RecommendationDto.ActionDto action = new RecommendationDto.ActionDto(
                "SCALE_DEPLOYMENT", "api-service", "{\"replicas\":\"5\"}");

        RecommendationDto dto = new RecommendationDto(
                "plan-001",
                "scaling-agent",
                List.of(action),
                "AUTO",
                0.92,
                System.currentTimeMillis(),
                "auto-system"
        );

        assertEquals("plan-001", dto.planId());
        assertEquals("AUTO", dto.approvalTier());
        assertEquals(0.92, dto.confidenceScore());
        assertEquals(1, dto.actions().size());
    }

    @Test
    @DisplayName("AuditDto can be created")
    void auditDtoCreation() {
        Instant now = Instant.now();
        AuditDto dto = new AuditDto(
                1L,
                "plan-001",
                "SCALE_DEPLOYMENT",
                "api-service",
                "auto-healing-service",
                "SUCCESS",
                1500,
                null,
                now
        );

        assertEquals(1L, dto.id());
        assertEquals("SUCCESS", dto.outcome());
        assertEquals(1500, dto.durationMs());
    }

    @Test
    @DisplayName("ErrorResponse can be created with factory method")
    void errorResponseCreation() {
        ErrorResponse response = ErrorResponse.of("Something went wrong", "INTERNAL_ERROR");

        assertNotNull(response.traceId());
        assertEquals("Something went wrong", response.error());
        assertEquals("INTERNAL_ERROR", response.code());
        assertNotNull(response.timestamp());
    }
}