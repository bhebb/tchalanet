package com.tchalanet.server.core.tenantgame.application.command.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;

/**
 * Command to update a tenant game policy (limits, flags, etc).
 * Maps to spec requirement TG3 (policies per tenant game).
 * Uses TenantId typed wrapper per typed_ids.md.
 */
@Getter
@Builder
public class UpdateTenantGamePolicyCommand implements Command<Void> {
  private final TenantId tenantId;
  private final String gameCode;
  private final JsonNode policy;
}
