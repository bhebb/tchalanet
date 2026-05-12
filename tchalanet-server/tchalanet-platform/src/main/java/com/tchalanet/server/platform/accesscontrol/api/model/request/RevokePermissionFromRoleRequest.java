package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.RoleId;

public record RevokePermissionFromRoleRequest(RoleId roleId, String permissionCode) {}
