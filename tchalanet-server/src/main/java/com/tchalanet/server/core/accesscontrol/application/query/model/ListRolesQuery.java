package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

public record ListRolesQuery(TenantId tenantId) implements Query<List<TchRole>> {}
