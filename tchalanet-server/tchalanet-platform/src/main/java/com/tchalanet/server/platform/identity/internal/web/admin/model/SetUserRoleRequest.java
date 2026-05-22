package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.common.security.TchRole;
import jakarta.validation.constraints.NotNull;

public record SetUserRoleRequest(@NotNull TchRole role) {}
