package com.tchalanet.server.repository;

import com.tchalanet.server.model.Plan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
  Optional<Plan> findByCodeAndPublicPlanTrue(String code);

  List<Plan> findByPublicPlanTrueOrderByPriceAmountAsc();
}
