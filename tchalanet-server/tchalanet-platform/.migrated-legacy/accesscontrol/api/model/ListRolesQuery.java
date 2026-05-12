package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

public record ListRolesRequest(TenantId tenantId) implements Query<List<RoleView>> {}

