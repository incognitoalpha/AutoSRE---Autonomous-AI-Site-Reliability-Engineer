package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RemediationPlan.ActionType;
import com.autosre.common.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RecoveryAgent")
class RecoveryAgentTest {

    private RecoveryAgent agent;

    @BeforeEach
    void setUp() {
        agent = new RecoveryAgent();
    }

    @Nested
    @DisplayName("getAgentName")
    class GetAgentName {

        @Test
        @DisplayName("returns correct agent name")
        void returnsCorrectAgentName() {
            assertEquals("RecoveryAgent", agent.getAgentName());
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class IsApplicable {

        @Test
        @DisplayName("returns true for crash-related anomalies")
        void returnsTrueForCrashAnomalies() {
            assertTrue(agent.isApplicable("CrashLoopBackOff", "Pod restart loop"));
            assertTrue(agent.isApplicable("Crash detected", "Application crash"));
        }

        @Test
        @DisplayName("returns true for OOM anomalies")
        void returnsTrueForOomAnomalies() {
            assertTrue(agent.isApplicable("OOMKilled", "Memory limit exceeded"));
            assertTrue(agent.isApplicable("Memory exceeded", "OOM prediction"));
        }

        @Test
        @DisplayName("returns true for restart issues")
        void returnsTrueForRestartIssues() {
            assertTrue(agent.isApplicable("Pod restart storm", "Repeated restarts"));
        }

        @Test
        @DisplayName("returns false for non-recovery issues")
        void returnsFalseForNonRecovery() {
            assertFalse(agent.isApplicable("High CPU", "CPU saturation"));
            assertFalse(agent.isApplicable("Security breach", "Auth failure"));
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("includes restart action for OOM")
        void includesRestartForOom() {
            RemediationPlan plan = agent.analyze(
                    "alert-123",
                    "api-service",
                    Severity.HIGH,
                    "Pod OOMKilled - memory limit exceeded",
                    "Memory exhaustion causing OOM"
            );

            boolean hasRestart = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.RESTART_POD);
            assertTrue(hasRestart, "Should include RESTART_POD action");
        }

        @Test
        @DisplayName("includes resource limit adjustment for OOM")
        void includesResourceAdjustmentForOom() {
            RemediationPlan plan = agent.analyze(
                    "alert-456",
                    "payment-service",
                    Severity.HIGH,
                    "Pod killed due to memory pressure",
                    "OOM due to heap exhaustion"
            );

            boolean hasAdjustment = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.ADJUST_RESOURCE_LIMITS);
            assertTrue(hasAdjustment, "Should include ADJUST_RESOURCE_LIMITS action");
        }

        @Test
        @DisplayName("returns low risk for critical severity")
        void returnsLowRiskForCritical() {
            RemediationPlan plan = agent.analyze(
                    "alert-789",
                    "order-service",
                    Severity.CRITICAL,
                    "Multiple pods crashing",
                    "CrashLoopBackOff detected"
            );

            assertEquals(com.autosre.agent.model.RiskLevel.LOW, plan.riskLevel());
        }

        @Test
        @DisplayName("produces valid plan with restart action")
        void producesValidPlan() {
            RemediationPlan plan = agent.analyze(
                    "alert-101",
                    "user-service",
                    Severity.MEDIUM,
                    "Pod restart loop detected",
                    "Application crash loop"
            );

            assertNotNull(plan);
            assertEquals("alert-101", plan.alertId());
            assertTrue(plan.actions().size() >= 1);

            boolean hasRestart = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.RESTART_POD);
            assertTrue(hasRestart);
        }
    }
}