package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.PendingApproval;
import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import com.autosre.recommendation.repository.PendingApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncApprovalGateTest {

    @Mock
    private PendingApprovalRepository repository;

    @Mock
    private RemediationProducer producer;

    @InjectMocks
    private SyncApprovalGate gate;

    private ApprovalGate.RemediationPlanWrapper plan;

    @BeforeEach
    void setUp() {
        plan = new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "agent-1",
                "alert-1",
                "service-1",
                "[]",
                RemediationRecommendation.RiskLevel.HIGH,
                0.50,
                RemediationRecommendation.ApprovalTier.SYNC
        );
    }

    @Test
    void testSupports() {
        assertTrue(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.LOW));
    }

    @Test
    void testProcess_Success() {
        ApprovalGate.ApprovalDecision decision = gate.process(plan);

        assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
        verify(repository).save(any(PendingApproval.class));
    }

    @Test
    void testProcess_Exception() {
        doThrow(new RuntimeException("DB error")).when(repository).save(any(PendingApproval.class));

        ApprovalGate.ApprovalDecision decision = gate.process(plan);

        assertEquals(ApprovalGate.ApprovalDecision.REJECTED, decision);
    }

    @Test
    void testApprove_Success() {
        PendingApproval approval = PendingApproval.create(
                plan.planId(), plan.agentId(), plan.alertId(), plan.serviceId(), plan.actionsJson(),
                plan.riskLevel(), plan.confidenceScore(), plan.tier()
        );

        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.of(approval));

        boolean result = gate.approve(plan.planId(), "user-1");

        assertTrue(result);
        verify(repository).save(approval);
        verify(producer).publishApproved(any(ApprovalGate.RemediationPlanWrapper.class));
    }

    @Test
    void testApprove_NotFound() {
        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.empty());

        boolean result = gate.approve(plan.planId(), "user-1");

        assertFalse(result);
        verify(repository, never()).save(any());
    }

    @Test
    void testApprove_Expired() {
        PendingApproval approval = Mockito.spy(PendingApproval.create(
                plan.planId(), plan.agentId(), plan.alertId(), plan.serviceId(), plan.actionsJson(),
                plan.riskLevel(), plan.confidenceScore(), plan.tier()
        ));
        Mockito.when(approval.isExpired()).thenReturn(true);

        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.of(approval));

        boolean result = gate.approve(plan.planId(), "user-1");

        assertFalse(result);
        verify(repository, never()).save(any());
        verify(producer, never()).publishApproved(any());
    }

    @Test
    void testApprove_PublishException() {
        PendingApproval approval = PendingApproval.create(
                plan.planId(), plan.agentId(), plan.alertId(), plan.serviceId(), plan.actionsJson(),
                plan.riskLevel(), plan.confidenceScore(), plan.tier()
        );

        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.of(approval));
        doThrow(new RuntimeException("Kafka error")).when(producer).publishApproved(any());

        boolean result = gate.approve(plan.planId(), "user-1");

        assertFalse(result);
        verify(repository).save(approval);
    }

    @Test
    void testReject_Success() {
        PendingApproval approval = PendingApproval.create(
                plan.planId(), plan.agentId(), plan.alertId(), plan.serviceId(), plan.actionsJson(),
                plan.riskLevel(), plan.confidenceScore(), plan.tier()
        );

        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.of(approval));

        boolean result = gate.reject(plan.planId(), "user-1", "Too risky");

        assertTrue(result);
        verify(repository).save(approval);
        assertEquals("Too risky", approval.getRejectionReason());
    }

    @Test
    void testReject_NotFound() {
        when(repository.findByPlanId(plan.planId())).thenReturn(Optional.empty());

        boolean result = gate.reject(plan.planId(), "user-1", "Too risky");

        assertFalse(result);
        verify(repository, never()).save(any());
    }
}
