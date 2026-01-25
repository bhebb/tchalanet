package com.tchalanet.server.catalog.settings.api;

/**
 * Setting Level (Scope)
 *
 * <p>Defines the hierarchy for setting resolution: GLOBAL → TENANT → OUTLET → TERMINAL
 */
public enum SettingLevel {
  /** Platform-wide default (no tenant/outlet/terminal) */
  GLOBAL,

  /** Tenant-specific override */
  TENANT,

  /** Outlet-specific override within tenant */
  OUTLET,

  /** Terminal-specific override within tenant */
  TERMINAL
}
