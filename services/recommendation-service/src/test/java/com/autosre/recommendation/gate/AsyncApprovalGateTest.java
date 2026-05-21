package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncApprovalGateTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RemediationProducer producer;

    @Mock
    private AsyncVetoChecker vetoChecker;

    @InjectMocks
    private AsyncApprovalGate gate;

    private ApprovalGate.RemediationPlanWrapper plan;

    @BeforeEach
    void setUp() {
        plan = new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "agent-1",
                "alert-1",
                "service-1",
                "[]",
                RemediationRecommendation.RiskLevel.MEDIUM,
                0.85,
                RemediationRecommendation.ApprovalTier.ASYNC
        );
    }

    @Test
    void testSupports() {
        assertTrue(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.LOW));
        assertFalse(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
    }

    @Test
    void testProcess_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ApprovalGate.ApprovalDecision decision = gate.process(plan);

        assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
        verify(valueOperations).set(eq("autosre:pending:async:" + plan.planId()), eq(plan), any(Duration.class));
    }

    @Test
    void testProcess_RedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis down")).when(valueOperations).set(any(), any(), any(Duration.class));

        ApprovalGate.ApprovalDecision decision = gate.process(plan);

        assertEquals(ApprovalGate.ApprovalDecision.REJECTED, decision);
    }

    @Test
    void testIsVetoed_True() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(true);
        assertTrue(gate.isVetoed(plan.planId()));
    }

    @Test
    void testIsVetoed_False() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(false);
        assertFalse(gate.isVetoed(plan.planId()));
    }

    @Test
    void testRegisterVeto() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        gate.registerVeto(plan.planId(), "Risk too high");

        verify(valueOperations).set(eq("autosre:pending:async:veto:" + plan.planId()), eq("Risk too high"), any(Duration.class));
    }

    @Test
    void testGetPendingPlan_Found() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autosre:pending:async:" + plan.planId())).thenReturn(plan);

        ApprovalGate.RemediationPlanWrapper retrieved = gate.getPendingPlan(plan.planId());

        assertEquals(plan, retrieved);
    }

    @Test
    void testGetPendingPlan_NotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autosre:pending:async:" + plan.planId())).thenReturn(null);

        ApprovalGate.RemediationPlanWrapper retrieved = gate.getPendingPlan(plan.planId());

        assertNull(retrieved);
    }

    @Test
    void testCheckAndPublish_Vetoed() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(true);

        gate.checkAndPublish(plan.planId());

        verifyNoInteractions(producer);
    }

    @Test
    void testCheckAndPublish_NotFound() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autosre:pending:async:" + plan.planId())).thenReturn(null);

        gate.checkAndPublish(plan.planId());

        verifyNoInteractions(producer);
    }

    @Test
    void testCheckAndPublish_Success() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autosre:pending:async:" + plan.planId())).thenReturn(plan);

        gate.checkAndPublish(plan.planId());

        verify(producer).publishApproved(plan);
        verify(redisTemplate).delete("autosre:pending:async:" + plan.planId());
    }

    @Test
    void testCheckAndPublish_Exception() {
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + plan.planId())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("autosre:pending:async:" + plan.planId())).thenReturn(plan);
        doThrow(new RuntimeException("Kafka error")).when(producer).publishApproved(plan);

        gate.checkAndPublish(plan.planId());

        // Assuming it catches the exception and does not throw further
        verify(producer).publishApproved(plan);
    }
}
