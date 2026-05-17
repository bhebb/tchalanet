package com.tchalanet.server.platform.tenantconfig.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import tools.jackson.databind.JsonNode;

public record UpdateTenantInternalSettingsRequest(TenantId tenantId, JsonNode settings) {

    public UpdateTenantInternalSettingsRequest {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (settings == null || settings.isNull()) {
            throw new IllegalArgumentException("settings is required");
        }
    }
}

