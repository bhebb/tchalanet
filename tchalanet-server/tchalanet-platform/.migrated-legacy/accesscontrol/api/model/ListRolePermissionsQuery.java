package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.RoleId;
import java.util.List;

public record ListRolePermissionsRequest(RoleId roleId) implements Query<List<RolePermissionView>> {}

