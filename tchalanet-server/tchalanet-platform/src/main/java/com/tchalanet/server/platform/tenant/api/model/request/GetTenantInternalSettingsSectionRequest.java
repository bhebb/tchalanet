package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsSectionRequest.Section;

public record GetTenantInternalSettingsSectionRequest(TenantId tenantId, Section section) {
  public GetTenantInternalSettingsSectionRequest {
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId is required");
    }
    if (section == null) {
      throw new IllegalArgumentException("section is required");
    }
  }
}
