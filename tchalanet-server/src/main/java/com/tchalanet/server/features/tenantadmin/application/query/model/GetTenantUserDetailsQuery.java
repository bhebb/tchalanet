package com.tchalanet.server.features.tenantadmin.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetTenantUserDetailsQuery(TenantId tenantId, UserId userId) implements Query<TenantUserDetails> {}
