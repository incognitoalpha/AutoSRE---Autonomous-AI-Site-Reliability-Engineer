package com.autosre.gateway.controller;

import com.autosre.gateway.repository.PendingApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for approval management of pending remediation plans.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalController.class);

    private final PendingApprovalRepository approvalRepository;

    public ApprovalController(PendingApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    /**
     * Approves a pending remediation plan, triggering execution.
     *
     * @param planId     the plan identifier to approve
     * @param decidedBy  who approved (user email or system identifier)
     * @return success or not found response
     */
    @PostMapping("/{planId}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable String planId,
            @RequestParam(defaultValue = "user") String decidedBy) {
        LOG.info("Approving plan: planId={}, decidedBy={}", planId, decidedBy);
        return approvalRepository.findByPlanId(planId)
                .map(entity -> {
                    entity.approve(decidedBy);
                    approvalRepository.save(entity);
                    LOG.info("Plan approved: planId={}", planId);
                    return ResponseEntity.ok(Map.of(
                            "status", "APPROVED",
                            "planId", planId,
                            "decidedBy", decidedBy
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rejects a pending remediation plan, preventing execution.
     *
     * @param planId     the plan identifier to reject
     * @param decidedBy  who rejected
     * @param reason     optional rejection reason
     * @return success or not found response
     */
    @PostMapping("/{planId}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable String planId,
            @RequestParam(defaultValue = "user") String decidedBy,
            @RequestParam(required = false) String reason) {
        LOG.info("Rejecting plan: planId={}, decidedBy={}, reason={}", planId, decidedBy, reason);
        return approvalRepository.findByPlanId(planId)
                .map(entity -> {
                    entity.reject(decidedBy);
                    approvalRepository.save(entity);
                    LOG.info("Plan rejected: planId={}", planId);
                    return ResponseEntity.ok(Map.of(
                            "status", "REJECTED",
                            "planId", planId,
                            "decidedBy", decidedBy,
                            "reason", reason != null ? reason : "no reason provided"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}