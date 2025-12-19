package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.security.TchRole;
import java.util.List;
import java.util.UUID;

public record ListRolesQuery(UUID tenantId) implements Query<List<TchRole>> {}
