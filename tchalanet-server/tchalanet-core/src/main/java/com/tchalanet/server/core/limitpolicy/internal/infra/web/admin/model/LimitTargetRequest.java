package com.tchalanet.server.core.limitpolicy.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record LimitTargetRequest(@NotNull Target targetType, OutletId outletId, UserId userId) {
  public enum Target { TENANT, OUTLET, USER }
}
