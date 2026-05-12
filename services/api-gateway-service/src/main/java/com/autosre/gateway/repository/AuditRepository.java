package com.autosre.gateway.repository;

import com.autosre.gateway.entity.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for AuditEntity persistence.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    List<AuditEntity> findByPlanId(String planId);

    Page<AuditEntity> findByExecutedAtBetweenOrderByExecutedAtDesc(
            Instant startTime, Instant endTime, Pageable pageable);
}