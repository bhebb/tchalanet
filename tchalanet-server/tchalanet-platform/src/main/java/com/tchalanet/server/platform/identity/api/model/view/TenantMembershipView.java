package com.tchalanet.server.platform.identity.api.model.view;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;

public record TenantMembershipView(
    TenantId tenantId, UserId userId, RoleId roleId, TenantUserStatus status, boolean owner) {}
