package com.tchalanet.server.core.billing.infra.persistence.repo;

import com.tchalanet.server.core.billing.infra.persistence.PlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {
    List<PlanJpaEntity> findByPublicPlanTrue();
}
