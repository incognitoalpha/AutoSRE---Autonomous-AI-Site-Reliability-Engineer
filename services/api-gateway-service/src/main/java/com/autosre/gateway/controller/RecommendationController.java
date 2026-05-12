package com.autosre.gateway.controller;

import com.autosre.gateway.dto.RecommendationDto;
import com.autosre.gateway.entity.PendingApprovalEntity;
import com.autosre.gateway.repository.PendingApprovalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for recommendation management.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationController.class);

    private final PendingApprovalRepository approvalRepository;
    private final ObjectMapper objectMapper;

    public RecommendationController(PendingApprovalRepository approvalRepository, ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns paginated recommendations, optionally filtered by status.
     *
     * @param status optional status filter (PENDING, APPROVED, REJECTED)
     * @param page   page number
     * @param size   page size
     * @return paginated recommendations
     */
    @GetMapping
    public ResponseEntity<Page<RecommendationDto>> listRecommendations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.info("Listing recommendations: status={}, page={}, size={}", status, page, size);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<PendingApprovalEntity> entities;
        if (status != null && !status.isEmpty()) {
            entities = approvalRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        } else {
            entities = approvalRepository.findAll(pageRequest);
        }
        Page<RecommendationDto> dtos = entities.map(this::toDto);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Retrieves a specific recommendation by plan ID.
     *
     * @param planId the plan identifier
     * @return recommendation details or 404 if not found
     */
    @GetMapping("/{planId}")
    public ResponseEntity<RecommendationDto> getRecommendation(@PathVariable String planId) {
        LOG.info("Fetching recommendation: planId={}", planId);
        return approvalRepository.findByPlanId(planId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private RecommendationDto toDto(PendingApprovalEntity entity) {
        List<RecommendationDto.ActionDto> actions = parseActions(entity.getActionsJson());
        return new RecommendationDto(
                entity.getPlanId(),
                entity.getAgentId(),
                actions,
                entity.getApprovalTier(),
                entity.getConfidenceScore(),
                entity.getCreatedAt(),
                entity.getDecidedBy() != null ? entity.getDecidedBy() : "pending"
        );
    }

    private List<RecommendationDto.ActionDto> parseActions(String actionsJson) {
        try {
            List<Map<String, String>> rawActions = objectMapper.readValue(actionsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() { });
            return rawActions.stream().map(m ->
                    new RecommendationDto.ActionDto(
                            m.getOrDefault("type", ""),
                            m.getOrDefault("target", ""),
                            m.getOrDefault("parameters", "{}")
                    )).toList();
        } catch (Exception e) {
            LOG.warn("Failed to parse actions JSON: {}", actionsJson);
            return List.of();
        }
    }
}