package com.tchalanet.server.core.terminal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record AssignUserRequest(@NotNull UserId userId) {}
