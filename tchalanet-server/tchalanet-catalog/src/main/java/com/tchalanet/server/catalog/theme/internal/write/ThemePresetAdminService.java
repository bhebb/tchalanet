package com.tchalanet.server.catalog.theme.internal.write;

import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.cache.ThemeCacheNames;
import com.tchalanet.server.catalog.theme.internal.mapper.ThemePresetMapper;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaEntity;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaRepository;
import com.tchalanet.server.common.types.id.ThemePresetId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ThemePresetAdminService {

    private final ThemePresetJpaRepository repo;
    private final ThemePresetMapper mapper;
    private final ThemePresetConfigValidator configValidator;

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE_PRESETS, ThemeCacheNames.PRESET_BY_CODE}, allEntries = true)
    public ThemePresetView create(ThemePresetCreateRequest req) {
        configValidator.validate(req.configAsString());
        var e = new ThemePresetJpaEntity();
        e.setCode(req.code());
        e.setVendor(req.vendor());
        e.setConfig(req.configAsString());
        e.setLabelKey(req.labelKey());
        e.setActive(req.active() == null || req.active());
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE_PRESETS, ThemeCacheNames.PRESET_BY_CODE}, allEntries = true)
    public ThemePresetView update(ThemePresetId id, ThemePresetUpdateRequest req) {
        var e = repo.findById(id.value()).orElseThrow(() -> new RuntimeException("theme_preset_not_found"));
        if (req.configAsString() != null) configValidator.validate(req.configAsString());
        if (req.code() != null) e.setCode(req.code());
        if (req.vendor() != null) e.setVendor(req.vendor());
        if (req.configAsString() != null) e.setConfig(req.configAsString());
        if (req.labelKey() != null) e.setLabelKey(req.labelKey());
        if (req.active() != null) e.setActive(req.active());
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE_PRESETS, ThemeCacheNames.PRESET_BY_CODE}, allEntries = true)
    public void softDelete(ThemePresetId id) {
        var e = repo.findById(id.value()).orElseThrow(() -> new RuntimeException("theme_preset_not_found"));
        e.setDeletedAt(Instant.now());
        e.setActive(false); // archive: mark inactive as well
        repo.save(e);
    }

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE_PRESETS, ThemeCacheNames.PRESET_BY_CODE}, allEntries = true)
    public void deactivate(ThemePresetId id) {
        var e = repo.findById(id.value()).orElseThrow(() -> new RuntimeException("theme_preset_not_found"));
        e.setActive(false);
        repo.save(e);
    }



    public record ThemePresetCreateRequest(String code, String vendor, String configAsString, String labelKey, Boolean active) {}
    public record ThemePresetUpdateRequest(String code, String vendor, String configAsString, String labelKey, Boolean active) {}
}
