package com.tchalanet.server.core.tenant.domain.ports;

import com.tchalanet.server.core.tenant.domain.model.Plan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {
  Optional<Plan> findById(UUID id);

  Optional<Plan> findPublicByCode(String code);

  List<Plan> findPublicPlans();

  // added methods
  Plan save(Plan plan);

  void delete(Plan plan);

  List<Plan> findAll();

  Optional<Plan> findByCode(String code);

  // soft-delete: mark deleted_at or similar
  void softDeleteByCode(String code);
}
