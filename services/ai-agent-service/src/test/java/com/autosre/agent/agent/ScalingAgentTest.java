package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.common.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScalingAgent")
class ScalingAgentTest {

    private ScalingAgent agent;

    @BeforeEach
    void setUp() {
        agent = new ScalingAgent(null);
    }

    @Nested
    @DisplayName("getAgentName")
    class GetAgentName {

        @Test
        @DisplayName("returns correct agent name")
        void returnsCorrectAgentName() {
            assertEquals("ScalingAgent", agent.getAgentName());
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class IsApplicable {

        @Test
        @DisplayName("returns true for CPU-related anomaly")
        void returnsTrueForCpuAnomaly() {
            assertTrue(agent.isApplicable("High CPU utilization", "CPU saturation"));
            assertTrue(agent.isApplicable("cpu_spike", "CPU above threshold"));
        }

        @Test
        @DisplayName("returns true for memory-related anomaly")
        void returnsTrueForMemoryAnomaly() {
            assertTrue(agent.isApplicable("Memory pressure", "Memory exhaustion"));
            assertTrue(agent.isApplicable("memory_leak", "Heap memory growing"));
        }

        @Test
        @DisplayName("returns true for capacity scaling anomaly")
        void returnsTrueForCapacityAnomaly() {
            assertTrue(agent.isApplicable("Capacity issue", "Resource saturation"));
            assertTrue(agent.isApplicable("Scale out needed", "Load exceeds capacity"));
        }

        @Test
        @DisplayName("returns false for unrelated anomaly types")
        void returnsFalseForUnrelatedAnomaly() {
            assertFalse(agent.isApplicable("Security alert", "Authentication failure"));
            assertFalse(agent.isApplicable("Deployment failed", "Rollback needed"));
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("produces valid remediation plan")
        void producesValidRemediationPlan() {
            RemediationPlan plan = agent.analyze(
                    "alert-123",
                    "payment-service",
                    Severity.HIGH,
                    "High CPU utilization detected",
                    "CPU saturation due to high load"
            );

            assertNotNull(plan);
            assertEquals("alert-123", plan.alertId());
            assertEquals("payment-service", plan.serviceId());
            assertEquals("ScalingAgent", plan.agentId());
            assertNotNull(plan.planId());
            assertFalse(plan.actions().isEmpty());
        }

        @Test
        @DisplayName("includes scale action type")
        void includesScaleActionType() {
            RemediationPlan plan = agent.analyze(
                    "alert-456",
                    "api-service",
                    Severity.MEDIUM,
                    "Memory usage increasing",
                    "Memory pressure"
            );

            boolean hasScaleAction = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == RemediationPlan.ActionType.SCALE_DEPLOYMENT ||
                                   a.actionType() == RemediationPlan.ActionType.SCALE_BROKERS);
            assertTrue(hasScaleAction, "Plan should include scale action");
        }

        @Test
        @DisplayName("returns low risk for critical severity")
        void returnsLowRiskForCritical() {
            RemediationPlan plan = agent.analyze(
                    "alert-789",
                    "kafka-broker",
                    Severity.CRITICAL,
                    "Memory exhaustion imminent",
                    "OOM kill predicted"
            );

            assertEquals(com.autosre.agent.model.RiskLevel.LOW, plan.riskLevel());
        }

        @Test
        @DisplayName("returns medium risk for high severity")
        void returnsMediumRiskForHigh() {
            RemediationPlan plan = agent.analyze(
                    "alert-101",
                    "order-service",
                    Severity.HIGH,
                    "Resource pressure increasing",
                    "Capacity constraint"
            );

            assertEquals(com.autosre.agent.model.RiskLevel.MEDIUM, plan.riskLevel());
        }
    }
}