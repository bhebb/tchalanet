package com.tchalanet.server.platform.tenantgame.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnsureTenantGamesRequest {
  private final TenantId tenantId;
  private final String gameCode;
}
