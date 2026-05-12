package com.autosre.gateway.controller;

import com.autosre.gateway.entity.PendingApprovalEntity;
import com.autosre.gateway.repository.PendingApprovalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerTest {

    @Mock
    private PendingApprovalRepository approvalRepository;

    @Test
    @DisplayName("Approve returns 404 when plan not found")
    void approveNotFound() {
        ApprovalController controller = new ApprovalController(approvalRepository);
        when(approvalRepository.findByPlanId("unknown-plan")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.approve("unknown-plan", "admin@example.com");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Approve updates entity and returns success")
    void approveSuccess() {
        ApprovalController controller = new ApprovalController(approvalRepository);
        PendingApprovalEntity entity = PendingApprovalEntity.create(
                "plan-001", "scaling-agent", "[]", "ASYNC", 0.85);
        when(approvalRepository.findByPlanId("plan-001")).thenReturn(Optional.of(entity));

        ResponseEntity<?> response = controller.approve("plan-001", "admin@example.com");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(approvalRepository).save(entity);
    }

    @Test
    @DisplayName("Reject returns 404 when plan not found")
    void rejectNotFound() {
        ApprovalController controller = new ApprovalController(approvalRepository);
        when(approvalRepository.findByPlanId("unknown-plan")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.reject("unknown-plan", "admin", "Not approved");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Reject updates entity and returns success")
    void rejectSuccess() {
        ApprovalController controller = new ApprovalController(approvalRepository);
        PendingApprovalEntity entity = PendingApprovalEntity.create(
                "plan-002", "security-agent", "[]", "SYNC", 0.60);
        when(approvalRepository.findByPlanId("plan-002")).thenReturn(Optional.of(entity));

        ResponseEntity<?> response = controller.reject("plan-002", "admin@example.com", "Needs review");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(approvalRepository).save(entity);
    }
}