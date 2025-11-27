package com.tchalanet.server.tenant.application.ports.in;

import com.tchalanet.server.common.web.dto.ContextDto;

public interface GetTenantContextUseCase {
  ContextDto execute(String tenantCode, String featureSetId);
}
