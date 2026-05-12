package com.autosre.recommendation.controller;

import com.autosre.recommendation.gate.SyncApprovalGate;
import com.autosre.recommendation.model.PendingApproval;
import com.autosre.recommendation.repository.PendingApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing approval requests.
 * Provides endpoints for approving, rejecting, and listing pending approvals.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalController.class);

    private final SyncApprovalGate syncApprovalGate;
    private final PendingApprovalRepository repository;

    public ApprovalController(
            SyncApprovalGate syncApprovalGate,
            PendingApprovalRepository repository) {
        this.syncApprovalGate = syncApprovalGate;
        this.repository = repository;
    }

    /**
     * Approves a pending remediation plan.
     *
     * @param planId the plan UUID
     * @param approvedBy the identifier of the approver
     * @return success or not found
     */
    @PostMapping("/{planId}/approve")
    public ResponseEntity<String> approve(
            @PathVariable UUID planId,
            @RequestParam(defaultValue = "system") String approvedBy) {

        LOG.info("Approval request: planId={}, approvedBy={}", planId, approvedBy);

        boolean success = syncApprovalGate.approve(planId, approvedBy);

        if (success) {
            return ResponseEntity.ok("Plan " + planId + " approved and published");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Rejects a pending remediation plan.
     *
     * @param planId the plan UUID
     * @param rejectedBy the identifier of the rejector
     * @param reason the rejection reason
     * @return success or not found
     */
    @PostMapping("/{planId}/reject")
    public ResponseEntity<String> reject(
            @PathVariable UUID planId,
            @RequestParam(defaultValue = "system") String rejectedBy,
            @RequestParam(required = false) String reason) {

        LOG.info("Rejection request: planId={}, rejectedBy={}, reason={}",
                planId, rejectedBy, reason);

        boolean success = syncApprovalGate.reject(planId, rejectedBy, reason);

        if (success) {
            return ResponseEntity.ok("Plan " + planId + " rejected");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lists all pending approvals.
     *
     * @return list of pending approvals
     */
    @GetMapping
    public ResponseEntity<List<PendingApproval>> listPending() {
        List<PendingApproval> pending = repository.findAllUnprocessed();
        return ResponseEntity.ok(pending);
    }

    /**
     * Gets a specific pending approval by plan ID.
     *
     * @param planId the plan UUID
     * @return the pending approval or not found
     */
    @GetMapping("/{planId}")
    public ResponseEntity<PendingApproval> getPending(@PathVariable UUID planId) {
        return repository.findByPlanId(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}