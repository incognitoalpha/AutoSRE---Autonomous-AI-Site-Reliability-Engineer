package com.autosre.agent.model;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RemediationPlan")
class RemediationPlanTest {

    private RemediationPlan.RemediationAction action(RemediationPlan.ActionType type, String target, List<RemediationPlan.RemediationAction.Parameter> params, String reason) {
        return new RemediationPlan.RemediationAction(type, target, params, reason);
    }

    private RemediationPlan.RemediationAction.Parameter param(String name, String value) {
        return new RemediationPlan.RemediationAction.Parameter(name, value);
    }

    @Nested
    @DisplayName("create factory")
    class CreateFactory {

        @Test
        @DisplayName("generates UUID and sets timestamp")
        void generatesUuidAndTimestamp() {
            RemediationPlan plan = RemediationPlan.create(
                    "scaling",
                    "alert-123",
                    "payment-service",
                    List.of(
                            action(RemediationPlan.ActionType.SCALE_DEPLOYMENT,
                                    "payment-deployment",
                                    List.of(param("replicas", "5")),
                                    "High CPU utilization")
                    ),
                    RiskLevel.MEDIUM,
                    "Reduce latency by 40%"
            );

            assertNotNull(plan.planId());
            assertNotNull(plan.createdAt());
            assertEquals("scaling", plan.agentId());
            assertEquals("alert-123", plan.alertId());
            assertEquals("payment-service", plan.serviceId());
            assertEquals(1, plan.actions().size());
            assertEquals(RiskLevel.MEDIUM, plan.riskLevel());
        }

        @Test
        @DisplayName("creates plan with multiple actions")
        void createsPlanWithMultipleActions() {
            List<RemediationPlan.RemediationAction> actions = List.of(
                    action(RemediationPlan.ActionType.ROLLBACK_DEPLOYMENT,
                            "api-deployment",
                            List.of(param("revision", "v1.2.3")),
                            "Error rate spike after v1.3.0 deploy"),
                    action(RemediationPlan.ActionType.NOTIFY_ONCALL,
                            "pagerduty",
                            List.of(),
                            "Human should be aware of rollback")
            );

            RemediationPlan plan = RemediationPlan.create(
                    "deployment",
                    "alert-456",
                    "api-gateway",
                    actions,
                    RiskLevel.LOW,
                    "Restore error rate to baseline"
            );

            assertEquals(2, plan.actions().size());
            assertEquals(RemediationPlan.ActionType.ROLLBACK_DEPLOYMENT,
                    plan.actions().get(0).actionType());
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("throws when riskLevel is null")
        void throwsWhenRiskLevelNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RemediationPlan(
                            UUID.randomUUID(),
                            "agent",
                            "alert-1",
                            "svc",
                            List.of(),
                            null,
                            "impact",
                            null
                    )
            );
        }

        @Test
        @DisplayName("allows null actions list")
        void allowsNullActions() {
            RemediationPlan plan = new RemediationPlan(
                    UUID.randomUUID(), "agent", "alert-1", "svc", null,
                    RiskLevel.HIGH, "impact", null
            );
            assertTrue(plan.actions().isEmpty());
        }
    }

    @Nested
    @DisplayName("isAutoApprovable")
    class IsAutoApprovable {

        @Test
        @DisplayName("returns true for LOW risk")
        void returnsTrueForLowRisk() {
            RemediationPlan plan = RemediationPlan.create(
                    "scaling", "alert-1", "svc", List.of(), RiskLevel.LOW, "low impact"
            );
            assertTrue(plan.isAutoApprovable());
        }

        @Test
        @DisplayName("returns false for MEDIUM risk")
        void returnsFalseForMediumRisk() {
            RemediationPlan plan = RemediationPlan.create(
                    "scaling", "alert-1", "svc", List.of(), RiskLevel.MEDIUM, "medium impact"
            );
            assertFalse(plan.isAutoApprovable());
        }

        @Test
        @DisplayName("returns false for HIGH risk")
        void returnsFalseForHighRisk() {
            RemediationPlan plan = RemediationPlan.create(
                    "security", "alert-1", "svc", List.of(), RiskLevel.HIGH, "high impact"
            );
            assertFalse(plan.isAutoApprovable());
        }
    }

    @Nested
    @DisplayName("RemediationAction validation")
    class ActionValidation {

        @Test
        @DisplayName("throws when actionType is null")
        void throwsWhenActionTypeNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    action(null, "target", List.of(), "reason")
            );
        }

        @Test
        @DisplayName("trims target and reason whitespace")
        void trimsWhitespace() {
            RemediationPlan.RemediationAction act = action(
                    RemediationPlan.ActionType.RESTART_POD,
                    "  payment-pod-xyz  ",
                    List.of(),
                    "  OOM killed  "
            );
            assertEquals("payment-pod-xyz", act.target());
            assertEquals("OOM killed", act.reason());
        }

        @Test
        @DisplayName("defaults null parameters to empty list")
        void defaultsNullParameters() {
            RemediationPlan.RemediationAction act = action(
                    RemediationPlan.ActionType.NOTIFY_ONCALL, "pagerduty", null, "Critical alert"
            );
            assertTrue(act.parameters().isEmpty());
        }
    }

    @Nested
    @DisplayName("ActionType enum values")
    class ActionTypeValues {

        @Test
        @DisplayName("contains all expected action types")
        void containsAllExpectedTypes() {
            RemediationPlan.ActionType[] types = RemediationPlan.ActionType.values();
            List<String> names = java.util.Arrays.stream(types)
                    .map(Enum::name)
                    .toList();

            assertTrue(names.contains("SCALE_DEPLOYMENT"));
            assertTrue(names.contains("SCALE_BROKERS"));
            assertTrue(names.contains("RESTART_POD"));
            assertTrue(names.contains("ROLLBACK_DEPLOYMENT"));
            assertTrue(names.contains("QUARANTINE_POD"));
            assertTrue(names.contains("ROTATE_SECRETS"));
            assertTrue(names.contains("ADJUST_RESOURCE_LIMITS"));
            assertTrue(names.contains("DRAIN_NODE"));
            assertTrue(names.contains("NOTIFY_ONCALL"));
        }
    }
}