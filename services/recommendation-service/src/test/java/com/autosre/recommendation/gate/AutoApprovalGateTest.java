package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoApprovalGate")
class AutoApprovalGateTest {

    @Mock
    private RemediationProducer producer;

    private AutoApprovalGate gate;

    @BeforeEach
    void setUp() {
        gate = new AutoApprovalGate(producer);
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("returns true for LOW risk")
        void returnsTrueForLowRisk() {
            assertTrue(gate.supports(RemediationRecommendation.RiskLevel.LOW));
        }

        @Test
        @DisplayName("returns false for MEDIUM risk")
        void returnsFalseForMediumRisk() {
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        }

        @Test
        @DisplayName("returns false for HIGH risk")
        void returnsFalseForHighRisk() {
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
        }
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("approves and publishes plan with high confidence")
        void approvesHighConfidence() {
            var plan = createPlan(0.95);

            var decision = gate.process(plan);

            assertEquals(ApprovalGate.ApprovalDecision.APPROVED, decision);
            verify(producer).publishApproved(any());
        }

        @Test
        @DisplayName("pends plan with low confidence")
        void pendsLowConfidence() {
            var plan = createPlan(0.85);

            var decision = gate.process(plan);

            assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
        }
    }

    private ApprovalGate.RemediationPlanWrapper createPlan(double confidence) {
        return new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "ScalingAgent",
                "alert-123",
                "api-service",
                "[]",
                RemediationRecommendation.RiskLevel.LOW,
                confidence,
                null
        );
    }
}