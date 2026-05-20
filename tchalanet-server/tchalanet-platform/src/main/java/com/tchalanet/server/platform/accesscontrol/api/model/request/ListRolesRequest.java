package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;

public record ListRolesRequest(TenantId tenantId) {}
