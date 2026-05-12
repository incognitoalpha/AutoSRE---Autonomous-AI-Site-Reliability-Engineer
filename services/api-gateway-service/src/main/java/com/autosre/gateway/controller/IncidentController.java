package com.autosre.gateway.controller;

import com.autosre.common.model.Severity;
import com.autosre.gateway.dto.IncidentDto;
import com.autosre.gateway.entity.IncidentEntity;
import com.autosre.gateway.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for incident lifecycle management.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentController.class);

    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Returns a paginated list of incidents, optionally filtered by severity.
     *
     * @param severity optional severity filter
     * @param page     page number (default 0)
     * @param size     page size (default 20)
     * @return paginated list of incidents
     */
    @GetMapping
    public ResponseEntity<Page<IncidentDto>> listIncidents(
            @RequestParam(required = false) Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.info("Listing incidents: severity={}, page={}, size={}", severity, page, size);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<IncidentEntity> entities;
        if (severity != null) {
            entities = incidentRepository.findBySeverity(severity, pageRequest);
        } else {
            entities = incidentRepository.findAll(pageRequest);
        }
        Page<IncidentDto> dtos = entities.map(this::toDto);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Retrieves a single incident by its UUID.
     *
     * @param id incident UUID
     * @return incident details or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getIncident(@PathVariable UUID id) {
        LOG.info("Fetching incident: id={}", id);
        return incidentRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private IncidentDto toDto(IncidentEntity entity) {
        return new IncidentDto(
                entity.getId(),
                entity.getServiceId(),
                entity.getSeverity(),
                entity.getRootCause(),
                entity.getStatus(),
                entity.getDetectedAt(),
                entity.getResolvedAt()
        );
    }
}