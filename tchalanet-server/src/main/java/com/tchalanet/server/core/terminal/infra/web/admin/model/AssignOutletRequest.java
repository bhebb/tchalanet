package com.tchalanet.server.core.terminal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import jakarta.validation.constraints.NotNull;

public record AssignOutletRequest(@NotNull OutletId outletId) {}
