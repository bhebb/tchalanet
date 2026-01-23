package com.tchalanet.server.catalog.theme.api;

import java.util.List;
import java.util.Optional;

/** Public contract for accessing ThemePreset catalog (read-only). */
public interface ThemePresetCatalog {

    List<ThemePresetView> listActive();

    Optional<ThemePresetView> findById(ThemePresetId id);

    Optional<ThemePresetView> findByCode(String code);
}
