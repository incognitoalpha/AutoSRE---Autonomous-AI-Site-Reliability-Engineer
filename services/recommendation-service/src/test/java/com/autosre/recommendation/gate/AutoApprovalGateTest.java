package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AutoApprovalGateTest {

    @Mock
    private RemediationProducer producer;

    @InjectMocks
    private AutoApprovalGate gate;

    private ApprovalGate.RemediationPlanWrapper plan;

    @BeforeEach
    void setUp() {
        plan = new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "agent-1",
                "alert-1",
                "service-1",
                "[]",
                RemediationRecommendation.RiskLevel.LOW,
                0.95,
                RemediationRecommendation.ApprovalTier.AUTO
        );
    }

    @Test
    void testSupports() {
        assertTrue(gate.supports(RemediationRecommendation.RiskLevel.LOW));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
    }

    @Test
    void testProcess_Approved() {
        ApprovalGate.ApprovalDecision decision = gate.process(plan);
        assertEquals(ApprovalGate.ApprovalDecision.APPROVED, decision);
        verify(producer).publishApproved(plan);
    }

    @Test
    void testProcess_LowConfidence() {
        ApprovalGate.RemediationPlanWrapper lowConfPlan = new ApprovalGate.RemediationPlanWrapper(
                plan.planId(), plan.agentId(), plan.alertId(), plan.serviceId(),
                plan.actionsJson(), plan.riskLevel(), 0.85, plan.tier()
        );

        ApprovalGate.ApprovalDecision decision = gate.process(lowConfPlan);
        assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
        verifyNoInteractions(producer);
    }

    @Test
    void testProcess_Exception() {
        doThrow(new RuntimeException("Kafka error")).when(producer).publishApproved(any());

        ApprovalGate.ApprovalDecision decision = gate.process(plan);
        assertEquals(ApprovalGate.ApprovalDecision.REJECTED, decision);
    }
}
