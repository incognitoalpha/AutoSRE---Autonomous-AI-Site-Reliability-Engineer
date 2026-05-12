package com.autosre.gateway.repository;

import com.autosre.common.model.Severity;
import com.autosre.gateway.entity.IncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for IncidentEntity persistence.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Page<IncidentEntity> findByServiceId(String serviceId, Pageable pageable);

    Page<IncidentEntity> findByStatus(String status, Pageable pageable);

    Page<IncidentEntity> findBySeverity(Severity severity, Pageable pageable);
}