package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.Plan;
import java.util.List;
import java.util.Optional;

public interface PlanCrudUseCase {
  Plan create(Plan p);

  Optional<Plan> getByCode(String code);

  List<Plan> listAll();

  Plan update(String code, Plan p);

  void delete(String code);
}
