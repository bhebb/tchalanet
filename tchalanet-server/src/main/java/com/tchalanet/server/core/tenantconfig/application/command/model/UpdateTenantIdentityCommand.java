package com.tchalanet.server.core.tenantconfig.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.ZoneId;
import java.util.Currency;

/**
 * Command to update tenant identity fields (name, timezone, currency).
 */
public record UpdateTenantIdentityCommand(
    TenantId tenantId,
    String name,
    ZoneId timezone,
    Currency currency
) implements Command<Void> {
  public UpdateTenantIdentityCommand {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
