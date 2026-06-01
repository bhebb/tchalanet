package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public interface TenantUserRoleWriterPort {
    void setUserRole(TenantId tenantId, UserId userId, RoleId roleId);
}
