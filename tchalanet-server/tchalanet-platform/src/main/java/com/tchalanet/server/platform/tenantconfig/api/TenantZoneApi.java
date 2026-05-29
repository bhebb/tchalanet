package com.tchalanet.server.platform.tenantconfig.api;

import com.tchalanet.server.common.types.id.TenantId;

import java.time.ZoneId;

/** Resolves the effective timezone for a tenant. */
public interface TenantZoneApi {
    ZoneId resolveTenantZone(TenantId tenantId);
}
