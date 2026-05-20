package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.security.TchRole;

public record SetUserRoleRequest(TchRole role) {}
