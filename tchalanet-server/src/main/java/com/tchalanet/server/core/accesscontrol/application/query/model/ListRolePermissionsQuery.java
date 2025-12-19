package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.Set;
import java.util.UUID;

public record ListRolePermissionsQuery(UUID roleId) implements Query<Set<String>> {}

