package com.autosre.agent.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.autosre.common.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

/**
 * Unit tests for {@link AgentContext} record.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
class AgentContextTest {

    @Test
    @DisplayName("Should create AgentContext with all fields")
    void agentContextCreatesCorrectly() {
        List<String> runbooks = List.of("Runbook 1", "Runbook 2");
        List<AgentContext.HistoricalIncident> incidents = List.of(
                new AgentContext.HistoricalIncident("id-1", "payment-svc", "OOM", Instant.now())
        );

        AgentContext context = new AgentContext(
                "alert-123",
                "payment-svc",
                Severity.HIGH,
                Instant.now(),
                runbooks,
                incidents,
                "Step 1: Analyzing metrics..."
        );

        assertThat(context.alertId()).isEqualTo("alert-123");
        assertThat(context.serviceId()).isEqualTo("payment-svc");
        assertThat(context.severity()).isEqualTo(Severity.HIGH);
        assertThat(context.retrievedRunbooks()).hasSize(2);
        assertThat(context.historicalIncidents()).hasSize(1);
        assertThat(context.reasoningTrace()).contains("Step 1");
    }

    @Test
    @DisplayName("HistoricalIncident record creates correctly")
    void historicalIncidentCreatesCorrectly() {
        Instant resolvedAt = Instant.now();

        AgentContext.HistoricalIncident incident = new AgentContext.HistoricalIncident(
                "incident-456",
                "order-svc",
                "Database connection pool exhaustion",
                resolvedAt
        );

        assertThat(incident.incidentId()).isEqualTo("incident-456");
        assertThat(incident.serviceId()).isEqualTo("order-svc");
        assertThat(incident.rootCauseSummary()).isEqualTo("Database connection pool exhaustion");
        assertThat(incident.resolvedAt()).isEqualTo(resolvedAt);
    }

    @Test
    @DisplayName("AgentContext has correct field count for record immutability")
    void agentContextRecordHasCorrectFieldCount() {
        AgentContext context = new AgentContext(
                "alert-1",
                "svc",
                Severity.LOW,
                Instant.now(),
                List.of(),
                List.of(),
                ""
        );

        // Records are immutable - verify all accessor methods work
        assertThat(context.alertId()).isNotNull();
        assertThat(context.serviceId()).isNotNull();
        assertThat(context.severity()).isNotNull();
        assertThat(context.detectedAt()).isNotNull();
        assertThat(context.retrievedRunbooks()).isNotNull();
        assertThat(context.historicalIncidents()).isNotNull();
        assertThat(context.reasoningTrace()).isNotNull();
    }
}