package com.tchalanet.server.core.accesscontrol.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.security.TchRole;
import java.util.List;
import java.util.UUID;

public record ListRolesQuery(TenantId tenantId) implements Query<List<TchRole>> {}
