package com.autosre.gateway.controller;

import com.autosre.gateway.dto.AuditDto;
import com.autosre.gateway.entity.AuditEntity;
import com.autosre.gateway.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for audit log queries.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final Logger LOG = LoggerFactory.getLogger(AuditController.class);

    private final AuditRepository auditRepository;

    public AuditController(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Returns a paginated list of audit entries, optionally filtered by time range.
     *
     * @param planId optional plan ID filter
     * @param start  optional start time (ISO-8601)
     * @param end    optional end time (ISO-8601)
     * @param page   page number (default 0)
     * @param size   page size (default 50)
     * @return paginated audit entries
     */
    @GetMapping
    public ResponseEntity<List<AuditDto>> getAuditLog(
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        LOG.info("Querying audit log: planId={}, start={}, end={}, page={}, size={}", planId, start, end, page, size);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditEntity> entities;
        if (planId != null && !planId.isEmpty()) {
            entities = auditRepository.findAll(pageRequest);
        } else if (start != null && end != null) {
            entities = auditRepository.findByExecutedAtBetweenOrderByExecutedAtDesc(start, end, pageRequest);
        } else {
            entities = auditRepository.findAll(pageRequest);
        }
        List<AuditDto> dtos = entities.map(this::toDto).getContent();
        return ResponseEntity.ok(dtos);
    }

    private AuditDto toDto(AuditEntity entity) {
        return new AuditDto(
                entity.getId(),
                entity.getPlanId(),
                entity.getActionType(),
                entity.getTarget(),
                entity.getExecutor(),
                entity.getOutcome(),
                entity.getDurationMs(),
                entity.getErrorMessage(),
                entity.getExecutedAt()
        );
    }
}