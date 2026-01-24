package com.tchalanet.server.core.tenantgame.application.command.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnsureTenantGamesCommand {
  private final UUID tenantId;
  private final String gameCode;
}
