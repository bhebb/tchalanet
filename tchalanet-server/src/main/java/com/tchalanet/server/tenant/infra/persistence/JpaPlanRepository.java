package com.tchalanet.server.tenant.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPlanRepository extends JpaRepository<PlanJpaEntity, UUID> {
  Optional<PlanJpaEntity> findByCodeAndPublicPlanTrue(String code);

  List<PlanJpaEntity> findByPublicPlanTrueOrderByPriceAmountAsc();
}
