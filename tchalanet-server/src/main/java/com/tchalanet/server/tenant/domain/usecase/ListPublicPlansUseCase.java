package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.Plan;
import java.util.List;
import java.util.UUID;

public interface ListPublicPlansUseCase {
  List<Plan> listForTenant(UUID tenantId);
}
