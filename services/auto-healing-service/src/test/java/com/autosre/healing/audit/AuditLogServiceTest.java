package com.autosre.healing.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository repository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(repository);
    }

    @Test
    @DisplayName("Logs successful action")
    void logsSuccessfulAction() {
        // When
        auditLogService.logAction("plan-001", "SCALE_DEPLOYMENT", "api-service", "SUCCESS", 1500, null);

        // Then
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertEquals("plan-001", entry.getPlanId());
        assertEquals("SCALE_DEPLOYMENT", entry.getActionType());
        assertEquals("api-service", entry.getTarget());
        assertEquals("auto-healing-service", entry.getExecutor());
        assertEquals("SUCCESS", entry.getOutcome());
        assertEquals(1500, entry.getDurationMs());
        assertEquals(null, entry.getErrorMessage());
    }

    @Test
    @DisplayName("Logs failed action with error message")
    void logsFailedAction() {
        // When
        auditLogService.logAction("plan-002", "RESTART_POD", "cache-pod", "FAILURE", 500, "Pod not found");

        // Then
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repository).save(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertEquals("FAILURE", entry.getOutcome());
        assertEquals("Pod not found", entry.getErrorMessage());
    }

    @Test
    @DisplayName("Finds by plan ID")
    void findsByPlanId() {
        // Given
        AuditLogEntry entry1 = AuditLogEntry.create("plan-001", "SCALE", "svc1", "svc", "SUCCESS", 100, null);
        AuditLogEntry entry2 = AuditLogEntry.create("plan-001", "RESTART", "pod1", "svc", "SUCCESS", 200, null);
        when(repository.findByPlanId("plan-001")).thenReturn(List.of(entry1, entry2));

        // When
        List<AuditLogEntry> results = auditLogService.findByPlanId("plan-001");

        // Then
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Finds by time range")
    void findsByTimeRange() {
        // Given
        long start = System.currentTimeMillis() - 3600000;
        long end = System.currentTimeMillis();
        when(repository.findByExecutedAtBetween(start, end)).thenReturn(List.of());

        // When
        List<AuditLogEntry> results = auditLogService.findByTimeRange(start, end);

        // Then
        verify(repository).findByExecutedAtBetween(start, end);
        assertNotNull(results);
    }
}