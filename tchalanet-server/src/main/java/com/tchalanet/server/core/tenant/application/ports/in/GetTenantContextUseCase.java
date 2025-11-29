package com.tchalanet.server.core.tenant.application.ports.in;

import com.tchalanet.server.core.user.web.dto.UserContextResponse;

public interface GetTenantContextUseCase {
  UserContextResponse execute(String tenantCode, String featureSetId);
}
