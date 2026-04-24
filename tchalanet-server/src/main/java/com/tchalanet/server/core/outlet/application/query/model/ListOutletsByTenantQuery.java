package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

// Returns list of OutletView for the tenant
public record ListOutletsByTenantQuery(TenantId tenantId) implements Query<List<OutletView>> {}
