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

@DisplayName("DeploymentAgent")
class DeploymentAgentTest {

    private DeploymentAgent agent;

    @BeforeEach
    void setUp() {
        agent = new DeploymentAgent();
    }

    @Nested
    @DisplayName("getAgentName")
    class GetAgentName {

        @Test
        @DisplayName("returns correct agent name")
        void returnsCorrectAgentName() {
            assertEquals("DeploymentAgent", agent.getAgentName());
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class IsApplicable {

        @Test
        @DisplayName("returns true for deployment-related anomalies")
        void returnsTrueForDeployAnomalies() {
            assertTrue(agent.isApplicable("Deployment failed", "Post-deploy regression"));
            assertTrue(agent.isApplicable("Version mismatch", "Rollback needed"));
        }

        @Test
        @DisplayName("returns true for regression anomalies")
        void returnsTrueForRegression() {
            assertTrue(agent.isApplicable("Error rate spike", "Post-deploy regression"));
            assertTrue(agent.isApplicable("Latency increase", "Release regression"));
        }

        @Test
        @DisplayName("returns false for non-deployment issues")
        void returnsFalseForNonDeployment() {
            assertFalse(agent.isApplicable("High CPU", "Resource saturation"));
            assertFalse(agent.isApplicable("Security breach", "Auth failure"));
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("includes rollback action")
        void includesRollbackAction() {
            RemediationPlan plan = agent.analyze(
                    "alert-123",
                    "checkout-service",
                    Severity.HIGH,
                    "Error rate spike after deployment",
                    "Post-deploy regression"
            );

            boolean hasRollback = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.ROLLBACK_DEPLOYMENT);
            assertTrue(hasRollback, "Should include ROLLBACK_DEPLOYMENT action");
        }

        @Test
        @DisplayName("returns low risk for critical severity")
        void returnsLowRiskForCritical() {
            RemediationPlan plan = agent.analyze(
                    "alert-456",
                    "payment-service",
                    Severity.CRITICAL,
                    "Service crashing after deploy",
                    "Deployment caused crash"
            );

            assertEquals(com.autosre.agent.model.RiskLevel.LOW, plan.riskLevel());
        }

        @Test
        @DisplayName("includes on-call notification")
        void includesOncallNotification() {
            RemediationPlan plan = agent.analyze(
                    "alert-789",
                    "order-service",
                    Severity.MEDIUM,
                    "Latency regression post-release",
                    "Version incompatibility"
            );

            boolean hasNotify = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.NOTIFY_ONCALL);
            assertTrue(hasNotify, "Should include NOTIFY_ONCALL action");
        }

        @Test
        @DisplayName("produces valid plan structure")
        void producesValidPlan() {
            RemediationPlan plan = agent.analyze(
                    "alert-101",
                    "inventory-service",
                    Severity.HIGH,
                    "Health check failures post-deploy",
                    "Deployment regression"
            );

            assertNotNull(plan);
            assertEquals("alert-101", plan.alertId());
            assertEquals("inventory-service", plan.serviceId());
            assertFalse(plan.actions().isEmpty());
        }
    }
}