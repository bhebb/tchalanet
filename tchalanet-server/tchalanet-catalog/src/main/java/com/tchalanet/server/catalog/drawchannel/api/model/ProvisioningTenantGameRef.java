package com.tchalanet.server.catalog.drawchannel.api.model;

import com.tchalanet.server.common.types.id.TenantGameId;

public record ProvisioningTenantGameRef(
    TenantGameId tenantGameId,
    String gameCode) {}
