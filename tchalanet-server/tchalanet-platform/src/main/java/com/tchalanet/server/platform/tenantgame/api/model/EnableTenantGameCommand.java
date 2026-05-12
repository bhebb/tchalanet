package com.tchalanet.server.platform.tenantgame.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * Command to enable a game for a tenant.
 * Maps to spec requirement TG1 (enable/disable commands).
 * Uses TenantId typed wrapper per typed_ids.md.
 */
@Getter
@Builder
public class EnableTenantGameCommand implements Command<EnableTenantGameCommandResult> {
  private final TenantId tenantId;
  private final String gameCode;
  private final JsonNode policy;
}
