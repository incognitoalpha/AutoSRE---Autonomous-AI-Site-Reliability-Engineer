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

@DisplayName("SecurityAgent")
class SecurityAgentTest {

    private SecurityAgent agent;

    @BeforeEach
    void setUp() {
        agent = new SecurityAgent();
    }

    @Nested
    @DisplayName("getAgentName")
    class GetAgentName {

        @Test
        @DisplayName("returns correct agent name")
        void returnsCorrectAgentName() {
            assertEquals("SecurityAgent", agent.getAgentName());
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class IsApplicable {

        @Test
        @DisplayName("returns true for security anomalies")
        void returnsTrueForSecurityAnomalies() {
            assertTrue(agent.isApplicable("Security breach detected", "Unauthorized access"));
            assertTrue(agent.isApplicable("Auth anomaly", "Authentication failure"));
            assertTrue(agent.isApplicable("Suspicious activity", "Access pattern unusual"));
        }

        @Test
        @DisplayName("returns true for credential issues")
        void returnsTrueForCredentialIssues() {
            assertTrue(agent.isApplicable("Credential exposed", "API key compromised"));
            assertTrue(agent.isApplicable("API key compromised", "Credential exposure"));
            assertTrue(agent.isApplicable("Token leak", "Credential exposure"));
        }

        @Test
        @DisplayName("returns false for non-security anomalies")
        void returnsFalseForNonSecurity() {
            assertFalse(agent.isApplicable("CPU spike", "High CPU utilization"));
            assertFalse(agent.isApplicable("Memory leak", "OOM predicted"));
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("always returns HIGH risk level")
        void alwaysReturnsHighRisk() {
            RemediationPlan plan = agent.analyze(
                    "alert-123",
                    "api-gateway",
                    Severity.HIGH,
                    "Unauthorized access detected",
                    "Security breach"
            );

            assertEquals(com.autosre.agent.model.RiskLevel.HIGH, plan.riskLevel());
        }

        @Test
        @DisplayName("includes quarantine action for unauthorized access")
        void includesQuarantineForUnauthorized() {
            RemediationPlan plan = agent.analyze(
                    "alert-456",
                    "payment-service",
                    Severity.CRITICAL,
                    "SQL injection attempt detected",
                    "Unauthorized access"
            );

            boolean hasQuarantine = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.QUARANTINE_POD);
            assertTrue(hasQuarantine, "Should include QUARANTINE_POD action");
        }

        @Test
        @DisplayName("includes rotate secrets for credential exposure")
        void includesRotateSecretsForCredentialExposure() {
            RemediationPlan plan = agent.analyze(
                    "alert-789",
                    "auth-service",
                    Severity.HIGH,
                    "Credential exposed",
                    "Credential exposure"
            );

            boolean hasRotate = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.ROTATE_SECRETS);
            assertTrue(hasRotate, "Should include ROTATE_SECRETS action");
        }

        @Test
        @DisplayName("produces valid plan with actions")
        void producesValidPlan() {
            RemediationPlan plan = agent.analyze(
                    "alert-101",
                    "user-service",
                    Severity.MEDIUM,
                    "Suspicious login patterns",
                    "Authentication anomaly"
            );

            assertNotNull(plan);
            assertEquals("alert-101", plan.alertId());
            assertEquals("user-service", plan.serviceId());
            assertEquals("SecurityAgent", plan.agentId());
            assertFalse(plan.actions().isEmpty());
        }
    }
}