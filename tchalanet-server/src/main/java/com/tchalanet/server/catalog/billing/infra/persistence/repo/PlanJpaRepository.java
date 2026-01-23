package com.tchalanet.server.catalog.billing.infra.persistence.repo;

import com.tchalanet.server.catalog.billing.infra.persistence.PlanJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {
  List<PlanJpaEntity> findByPublicPlanTrue();
}
