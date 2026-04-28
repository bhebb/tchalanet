package com.tchalanet.server.core.tenantuser.infra.web.admin.model;

import com.tchalanet.server.common.types.enums.TchRole;

public record SetUserRoleRequest(TchRole role) {}
