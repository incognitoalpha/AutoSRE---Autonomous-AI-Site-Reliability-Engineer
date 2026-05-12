package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.PendingApproval;
import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import com.autosre.recommendation.repository.PendingApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncApprovalGate")
class SyncApprovalGateTest {

    @Mock
    private PendingApprovalRepository repository;

    @Mock
    private RemediationProducer producer;

    private SyncApprovalGate gate;

    @BeforeEach
    void setUp() {
        gate = new SyncApprovalGate(repository, producer);
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("returns true for HIGH risk")
        void returnsTrueForHighRisk() {
            assertTrue(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
        }

        @Test
        @DisplayName("returns false for LOW risk")
        void returnsFalseForLowRisk() {
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.LOW));
        }

        @Test
        @DisplayName("returns false for MEDIUM risk")
        void returnsFalseForMediumRisk() {
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        }
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("saves plan to repository and returns PENDING")
        void savesAndReturnsPending() {
            var plan = createPlan();

            var decision = gate.process(plan);

            assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
            verify(repository).save(any(PendingApproval.class));
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("approves and publishes pending plan")
        void approvesAndPublishes() {
            UUID planId = UUID.randomUUID();
            PendingApproval approval = createPendingApproval(planId);
            when(repository.findByPlanId(planId)).thenReturn(Optional.of(approval));

            boolean result = gate.approve(planId, "oncall-engineer");

            assertTrue(result);
            verify(repository).save(approval);
            verify(producer).publishApproved(any());
        }

        @Test
        @DisplayName("returns false when plan not found")
        void returnsFalseWhenNotFound() {
            UUID planId = UUID.randomUUID();
            when(repository.findByPlanId(planId)).thenReturn(Optional.empty());

            boolean result = gate.approve(planId, "oncall-engineer");

            assertFalse(result);
            verify(producer, never()).publishApproved(any());
        }

        @Test
        @DisplayName("returns false when plan expired")
        void returnsFalseWhenExpired() {
            UUID planId = UUID.randomUUID();
            PendingApproval approval = createExpiredApproval(planId);
            when(repository.findByPlanId(planId)).thenReturn(Optional.of(approval));

            boolean result = gate.approve(planId, "oncall-engineer");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("rejects pending plan")
        void rejectsPendingPlan() {
            UUID planId = UUID.randomUUID();
            PendingApproval approval = createPendingApproval(planId);
            when(repository.findByPlanId(planId)).thenReturn(Optional.of(approval));

            boolean result = gate.reject(planId, "oncall-engineer", "Not safe");

            assertTrue(result);
            verify(repository).save(approval);
        }
    }

    private ApprovalGate.RemediationPlanWrapper createPlan() {
        return new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "SecurityAgent",
                "alert-456",
                "auth-service",
                "[]",
                RemediationRecommendation.RiskLevel.HIGH,
                0.75,
                null
        );
    }

    private PendingApproval createPendingApproval(UUID planId) {
        return PendingApproval.create(
                planId,
                "SecurityAgent",
                "alert-456",
                "auth-service",
                "[]",
                RemediationRecommendation.RiskLevel.HIGH,
                0.75,
                RemediationRecommendation.ApprovalTier.SYNC
        );
    }

    private PendingApproval createExpiredApproval(UUID planId) {
        PendingApproval approval = PendingApproval.create(
                planId,
                "SecurityAgent",
                "alert-456",
                "auth-service",
                "[]",
                RemediationRecommendation.RiskLevel.HIGH,
                0.75,
                RemediationRecommendation.ApprovalTier.SYNC
        );
        // Use reflection to set expiresAt to the past
        try {
            java.lang.reflect.Field field = PendingApproval.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(approval, java.time.Instant.now().minusSeconds(60));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return approval;
    }
}