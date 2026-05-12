package com.autosre.gateway.controller;

import com.autosre.gateway.dto.IncidentDto;
import com.autosre.gateway.entity.IncidentEntity;
import com.autosre.gateway.repository.IncidentRepository;
import com.autosre.common.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("List incidents returns empty page when no incidents")
    void listIncidentsEmpty() {
        IncidentController controller = new IncidentController(incidentRepository);
        when(incidentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<?> response = controller.listIncidents(null, 0, 20);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Get incident returns 404 when not found")
    void getIncidentNotFound() {
        IncidentController controller = new IncidentController(incidentRepository);
        UUID id = UUID.randomUUID();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<IncidentDto> response = controller.getIncident(id);
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Get incident returns incident when found")
    void getIncidentFound() {
        IncidentController controller = new IncidentController(incidentRepository);
        UUID id = UUID.randomUUID();
        IncidentEntity entity = IncidentEntity.create("api-service", Severity.HIGH, "High memory usage");
        when(incidentRepository.findById(id)).thenReturn(Optional.of(entity));

        ResponseEntity<IncidentDto> response = controller.getIncident(id);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody() != null);
        assertEquals("api-service", response.getBody().serviceId());
        assertEquals(Severity.HIGH, response.getBody().severity());
    }
}