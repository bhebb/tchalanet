package com.tchalanet.server.platform.accesscontrol.api.model.view;

import com.tchalanet.server.common.types.id.RoleId;

public record RolePermissionView(RoleId roleId, String permissionCode) {}


