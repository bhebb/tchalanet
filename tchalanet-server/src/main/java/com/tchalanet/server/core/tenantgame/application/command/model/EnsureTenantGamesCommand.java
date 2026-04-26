package com.tchalanet.server.core.tenantgame.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnsureTenantGamesCommand {
  private final TenantId tenantId;
  private final String gameCode;
}
