package com.tchalanet.server.tenant.application.ports.in;

import com.tchalanet.server.user.web.dto.UserContextResponse;

public interface GetTenantContextUseCase {
  UserContextResponse execute(String tenantCode, String featureSetId);
}
