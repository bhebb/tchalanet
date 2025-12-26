package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.RoleId;
import java.util.Set;

public record ListRolePermissionsQuery(RoleId roleId) implements Query<Set<String>> {}
