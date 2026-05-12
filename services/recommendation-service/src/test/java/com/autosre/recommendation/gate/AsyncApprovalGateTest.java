package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsyncApprovalGate")
class AsyncApprovalGateTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private RemediationProducer producer;

    @Mock
    private AsyncVetoChecker vetoChecker;

    private AsyncApprovalGate createGate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return new AsyncApprovalGate(redisTemplate, producer, vetoChecker);
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("returns true for MEDIUM risk")
        void returnsTrueForMediumRisk() {
            AsyncApprovalGate gate = createGate();
            assertTrue(gate.supports(RemediationRecommendation.RiskLevel.MEDIUM));
        }

        @Test
        @DisplayName("returns false for LOW risk")
        void returnsFalseForLowRisk() {
            AsyncApprovalGate gate = createGate();
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.LOW));
        }

        @Test
        @DisplayName("returns false for HIGH risk")
        void returnsFalseForHighRisk() {
            AsyncApprovalGate gate = createGate();
            assertFalse(gate.supports(RemediationRecommendation.RiskLevel.HIGH));
        }
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("stores plan in Redis with TTL")
        void storesPlanInRedis() {
            AsyncApprovalGate gate = createGate();
            var plan = createPlan();

            var decision = gate.process(plan);

            assertEquals(ApprovalGate.ApprovalDecision.PENDING, decision);
            verify(valueOps).set(
                    eq("autosre:pending:async:" + plan.planId()),
                    eq(plan),
                    eq(Duration.ofMinutes(5))
            );
        }
    }

    private ApprovalGate.RemediationPlanWrapper createPlan() {
        return new ApprovalGate.RemediationPlanWrapper(
                UUID.randomUUID(),
                "ScalingAgent",
                "alert-123",
                "api-service",
                "[]",
                RemediationRecommendation.RiskLevel.MEDIUM,
                0.80,
                null
        );
    }
}