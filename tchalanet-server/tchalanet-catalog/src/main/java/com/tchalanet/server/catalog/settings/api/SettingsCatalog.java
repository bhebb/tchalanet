package com.tchalanet.server.catalog.settings.api;

import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.api.model.ResolvedSettingView;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.catalog.settings.api.model.SettingsCatalogStatsView;

import java.util.List;

/**
 * Settings Catalog - Read Contract
 *
 * <p>Provides read-only access to application settings with hierarchical resolution:
 * GLOBAL → TENANT → OUTLET → TERMINAL
 *
 * <p>Settings are merged in order of specificity, with later levels overriding earlier ones.
 *
 * <p>This is the ONLY public interface for reading settings. All consumers (core, features) must
 * use this API.
 *
 * @see SettingView
 * @see ResolvedSettingView
 * @see ResolveSettingsCriteria
 */
public interface SettingsCatalog {

  /**
   * Resolve effective settings for the given criteria.
   *
   * <p>Merges settings in order: GLOBAL → TENANT → OUTLET (if provided) → TERMINAL (if provided)
   *
   * @param criteria resolution criteria (tenant, outlet, terminal, namespaces)
   * @return list of resolved settings with effective level
   */
  List<ResolvedSettingView> resolve(ResolveSettingsCriteria criteria);

  /**
   * Global statistics for settings (used by platform admin overview)
   */
  SettingsCatalogStatsView stats();
}
