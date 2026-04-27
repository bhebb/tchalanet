package com.tchalanet.server.core.tenanttheme.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenanttheme.domain.model.TenantTheme;

/**
 * Port for persisting tenant theme (write).
 * Maps to spec requirement T6.
 */
public interface TenantThemePersistencePort {

  /**
   * Save or update tenant theme.
   * Must enforce RLS at the adapter level.
   */
  TenantTheme save(TenantTheme tenantTheme);

  /**
   * Remove or deactivate tenant theme.
   */
  void deactivate(TenantId tenantId);
}
