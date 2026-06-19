package com.tchalanet.server.catalog.settings.api.model;

/**
 * Setting Level (Scope)
 *
 * <p>Defines the hierarchy for setting resolution: GLOBAL → TENANT
 */
public enum SettingLevel {
  /** Platform-wide default (no tenant) */
  GLOBAL,

  /** Tenant-specific override */
  TENANT
}
