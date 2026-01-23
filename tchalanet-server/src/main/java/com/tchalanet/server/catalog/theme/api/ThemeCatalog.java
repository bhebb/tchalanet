package com.tchalanet.server.catalog.theme.api;

import java.util.List;
import java.util.Optional;

/**
 * Backwards-compatible alias matching the spec name `ThemeCatalog`.
 * The implementation may use ThemePresetCatalog internally; this interface
 * is the public contract referenced by the spec.
 */
public interface ThemeCatalog {

    List<ThemePresetView> listActive();

    Optional<ThemePresetView> findById(ThemePresetId id);

    Optional<ThemePresetView> findByCode(String code);
}
