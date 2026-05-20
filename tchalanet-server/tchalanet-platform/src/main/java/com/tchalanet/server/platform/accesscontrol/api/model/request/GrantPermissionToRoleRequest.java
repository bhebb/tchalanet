package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.RoleId;

public record GrantPermissionToRoleRequest(RoleId roleId, String permissionCode) {}
