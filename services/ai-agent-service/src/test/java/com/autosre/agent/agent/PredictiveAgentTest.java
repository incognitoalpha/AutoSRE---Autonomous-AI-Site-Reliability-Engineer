package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RemediationPlan.ActionType;
import com.autosre.common.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PredictiveAgent")
class PredictiveAgentTest {

    private PredictiveAgent agent;

    @BeforeEach
    void setUp() {
        agent = new PredictiveAgent();
    }

    @Nested
    @DisplayName("getAgentName")
    class GetAgentName {

        @Test
        @DisplayName("returns correct agent name")
        void returnsCorrectAgentName() {
            assertEquals("PredictiveAgent", agent.getAgentName());
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class IsApplicable {

        @Test
        @DisplayName("returns true for predictive anomalies")
        void returnsTrueForPredictiveAnomalies() {
            assertTrue(agent.isApplicable("Predictive alert - disk", "Capacity exhaustion"));
            assertTrue(agent.isApplicable("Trend analysis", "Growth rate high"));
        }

        @Test
        @DisplayName("returns false for reactive anomalies")
        void returnsFalseForReactive() {
            assertFalse(agent.isApplicable("CPU spike", "Current high CPU"));
            assertFalse(agent.isApplicable("Memory leak", "Current OOM"));
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("produces valid plan")
        void producesValidPlan() {
            RemediationPlan plan = agent.analyze(
                    "alert-123",
                    "api-service",
                    Severity.MEDIUM,
                    "Predictive: Memory usage growing",
                    "Heap exhaustion trend detected"
            );

            assertNotNull(plan);
            assertEquals("alert-123", plan.alertId());
            assertFalse(plan.actions().isEmpty());
        }

        @Test
        @DisplayName("includes preventive actions for memory growth")
        void includesPreventiveForMemoryGrowth() {
            RemediationPlan plan = agent.analyze(
                    "alert-456",
                    "payment-service",
                    Severity.HIGH,
                    "Predictive alert: JVM heap growing",
                    "Memory exhaustion in 20 minutes"
            );

            boolean hasScale = plan.actions().stream()
                    .anyMatch(a -> a.actionType() == ActionType.SCALE_DEPLOYMENT ||
                                   a.actionType() == ActionType.ADJUST_RESOURCE_LIMITS);
            assertTrue(hasScale, "Should include scale or resource adjustment");
        }
    }

    @Nested
    @DisplayName("analyzeTrends")
    class AnalyzeTrends {

        @Test
        @DisplayName("returns empty list for insufficient data")
        void returnsEmptyForInsufficientData() {
            List<RemediationPlan.RemediationAction> actions =
                    agent.analyzeTrends("service-1", List.of(0.5, 0.6), "memory");

            // Less than 5 data points returns empty
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for null input")
        void returnsEmptyForNullInput() {
            List<RemediationPlan.RemediationAction> actions =
                    agent.analyzeTrends("service-2", null, "memory");

            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("returns scale action for growing memory trend")
        void returnsScaleForGrowingTrend() {
            // Create data points showing growth from 0.5 to 0.8
            List<Double> values = List.of(0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80);

            List<RemediationPlan.RemediationAction> actions =
                    agent.analyzeTrends("kafka-broker", values, "memory");

            assertFalse(actions.isEmpty(), "Should return actions for growing trend");
        }

        @Test
        @DisplayName("returns notify for disk exhaustion prediction")
        void returnsNotifyForDiskPrediction() {
            // Create data points showing high disk usage approaching exhaustion
            List<Double> values = List.of(0.85, 0.87, 0.89, 0.91, 0.93, 0.95);

            List<RemediationPlan.RemediationAction> actions =
                    agent.analyzeTrends("database", values, "disk");

            boolean hasNotify = actions.stream()
                    .anyMatch(a -> a.actionType() == ActionType.NOTIFY_ONCALL);
            assertTrue(hasNotify, "Should notify for disk exhaustion prediction");
        }
    }
}