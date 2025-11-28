package com.tchalanet.server.tenant.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.tenant.domain.model.Plan;
import com.tchalanet.server.tenant.domain.ports.PlanRepository;
import com.tchalanet.server.tenant.domain.usecase.ListPublicPlansUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPublicPlansUseCaseImpl implements ListPublicPlansUseCase {

  private final PlanRepository planRepository;

  @Override
  public List<Plan> listForTenant(java.util.UUID tenantId) {
    // For now public plans are global; tenantId can be used later for tenant-specific filtering
    return planRepository.findPublicPlans();
  }
}
