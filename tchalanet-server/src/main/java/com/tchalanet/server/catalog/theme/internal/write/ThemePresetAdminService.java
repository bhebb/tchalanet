package com.tchalanet.server.catalog.theme.internal.write;

import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.cache.ThemeCacheNames;
import com.tchalanet.server.catalog.theme.internal.mapper.ThemePresetMapper;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaEntity;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThemePresetAdminService {

    private final ThemePresetJpaRepository repo;
    private final ThemePresetMapper mapper;

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE, ThemeCacheNames.BY_CODE}, allEntries = true)
    public ThemePresetView create(ThemePresetCreateRequest req) {
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
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE, ThemeCacheNames.BY_CODE}, allEntries = true)
    public ThemePresetView update(UUID id, ThemePresetUpdateRequest req) {
        var e = repo.findById(id).orElseThrow(() -> new RuntimeException("theme_preset_not_found"));
        if (req.code() != null) e.setCode(req.code());
        if (req.vendor() != null) e.setVendor(req.vendor());
        if (req.configAsString() != null) e.setConfig(req.configAsString());
        if (req.labelKey() != null) e.setLabelKey(req.labelKey());
        if (req.active() != null) e.setActive(req.active());
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {ThemeCacheNames.ACTIVE, ThemeCacheNames.BY_CODE}, allEntries = true)
    public void softDelete(UUID id) {
        var e = repo.findById(id).orElseThrow(() -> new RuntimeException("theme_preset_not_found"));
        e.setDeletedAt(Instant.now());
        repo.save(e);
    }

    public static record ThemePresetCreateRequest(String code, String vendor, String configAsString, String labelKey, Boolean active) {}
    public static record ThemePresetUpdateRequest(String code, String vendor, String configAsString, String labelKey, Boolean active) {}
}
