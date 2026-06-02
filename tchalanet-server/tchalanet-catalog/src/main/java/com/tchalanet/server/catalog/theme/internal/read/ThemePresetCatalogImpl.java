package com.tchalanet.server.catalog.theme.internal.read;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.api.ThemePresetStatsView;
import com.tchalanet.server.catalog.theme.internal.cache.ThemeCacheNames;
import com.tchalanet.server.catalog.theme.internal.mapper.ThemePresetMapper;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaRepository;
import com.tchalanet.server.common.types.id.ThemePresetId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ThemePresetCatalogImpl implements ThemeCatalog {

    private final ThemePresetJpaRepository repo;
    private final ThemePresetMapper mapper;

    @Override
    @Cacheable(cacheNames = ThemeCacheNames.ACTIVE_PRESETS)
    public List<ThemePresetView> listActive() {
        var entities = repo.findAll().stream().filter(e -> e.isActive() && e.getDeletedAt() == null).toList();
        return entities.stream().map(mapper::toView).toList();
    }

    @Override
    @Cacheable(cacheNames = ThemeCacheNames.PRESET_BY_CODE, key = "#code == null ? '' : #code.toLowerCase()")
    public Optional<ThemePresetView> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return repo.findFirstByCodeIgnoreCaseAndDeletedAtIsNull(code).map(mapper::toView);
    }

    @Override
    public Optional<ThemePresetView> findById(ThemePresetId id) {
        if (id == null) return Optional.empty();
        UUID uuid = id.value();
        return repo.findById(uuid).filter(e -> e.getDeletedAt() == null).map(mapper::toView);
    }

    @Override
    public Optional<ThemePresetView> findDefault() {
        return repo.findAll().stream()
            .filter(e -> e.isDefaultPreset() && e.isActive() && e.getDeletedAt() == null)
            .findFirst()
            .map(mapper::toView);
    }

    @Override
    public ThemePresetStatsView stats() {
        long total = repo.count();
        long active = repo.findAll().stream().filter(e -> e.isActive() && e.getDeletedAt() == null).count();
        return new ThemePresetStatsView((int) total, (int) active);
    }
}
