package com.tchalanet.server.catalog.settings.internal.cache;

/**
 * Cache names for catalog/settings.
 *
 * <p>Defines cache identifiers for the Settings catalog. Cache is managed via Spring annotations
 * (@Cacheable, @CacheEvict) in the read and write services.
 */
public final class SettingsCacheNames {

  private SettingsCacheNames() {}

  /**
   * Cache for resolved settings (hierarchical merge result).
   *
   * <p>Key format: "t={tenantId}|o={outletId}|m={terminalId}|ns={namespaces}"
   */
  public static final String RESOLVED_SETTINGS = "catalog:settings:resolved_settings";
}
