package com.autosre.gateway.repository;

import com.autosre.gateway.entity.PendingApprovalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PendingApprovalEntity persistence.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApprovalEntity, Long> {

    Optional<PendingApprovalEntity> findByPlanId(String planId);

    List<PendingApprovalEntity> findByStatus(String status);

    Page<PendingApprovalEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}