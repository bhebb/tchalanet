package com.tchalanet.server.core.theme.application.query.handler;

import com.tchalanet.server.core.tenant.domain.model.Plan;
import java.util.List;
import java.util.UUID;

public interface ListPublicPlansUseCase {
  List<Plan> listForTenant(UUID tenantId);
}
