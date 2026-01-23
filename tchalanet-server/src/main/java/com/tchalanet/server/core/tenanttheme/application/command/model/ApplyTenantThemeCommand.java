package com.tchalanet.server.core.tenanttheme.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Command to apply a Theme Preset to a tenant.
 * Maps to spec requirement T1.
 */
public record ApplyTenantThemeCommand(
    TenantId tenantId,
    String presetCode
) implements Command<Void> {
  public ApplyTenantThemeCommand {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
    if (presetCode == null || presetCode.isBlank()) {
      throw new IllegalArgumentException("presetCode is required");
    }
  }
}
