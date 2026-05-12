package com.autosre.recommendation.repository;

import com.autosre.recommendation.model.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for PendingApproval entities.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, UUID> {

    /**
     * Finds a pending approval by plan ID.
     */
    Optional<PendingApproval> findByPlanId(UUID planId);

    /**
     * Finds all unprocessed pending approvals.
     */
    @Query("SELECT p FROM PendingApproval p WHERE p.approvedAt IS NULL AND p.rejectedAt IS NULL")
    List<PendingApproval> findAllUnprocessed();

    /**
     * Finds pending approvals that have expired.
     */
    @Query("SELECT p FROM PendingApproval p WHERE p.expiresAt < :now AND p.approvedAt IS NULL AND p.rejectedAt IS NULL")
    List<PendingApproval> findExpired(Instant now);

    /**
     * Finds pending approvals for a specific service.
     */
    List<PendingApproval> findByServiceId(String serviceId);

    /**
     * Finds pending approvals created after a certain time.
     */
    List<PendingApproval> findByCreatedAtAfter(Instant createdAt);
}