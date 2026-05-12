package com.tchalanet.server.platform.tenantconfig.internal.adapter;

import com.tchalanet.server.catalog.tenant.api.cache.TenantCacheNames;
import com.tchalanet.server.platform.tenantconfig.internal.mapper.TenantMapper;
import com.tchalanet.server.platform.tenantconfig.internal.persistence.TenantJpaRepository;
import com.tchalanet.server.platform.tenantconfig.internal.service.TenantConfig;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for TenantConfig.
 * Implements write port with separate create/update methods.
 * Per DOMAIN_TENANT_CONFIG.md:
 * - No RLS on tenant table (platform registry)
 * - Uses typed IDs (TenantId)
 * - Maps domain ↔ entity via TenantMapper
 * - create() for INSERT only
 * - update() for UPDATE only (throws EntityNotFoundException if not found)
 * - Evicts catalog/tenant caches on write (per user request)
 */
@Component
@RequiredArgsConstructor
public class TenantPersistenceAdapter {

    private final TenantJpaRepository repository;
    private final TenantMapper mapper;

    @CacheEvict(cacheNames = {
        TenantCacheNames.TENANT_BY_ID,
        TenantCacheNames.TENANT_BY_CODE,
        TenantCacheNames.BOOTSTRAP_BY_ID,
        TenantCacheNames.BOOTSTRAP_BY_CODE,
        TenantCacheNames.ACTIVE_TENANT_IDS
    }, allEntries = true)
    public TenantConfig create(TenantConfig tenant) {
        // CREATE: convert domain to new entity and save
        // Cache eviction: clear all tenant catalog caches
        var entity = mapper.toEntity(tenant);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @CacheEvict(cacheNames = {
        TenantCacheNames.TENANT_BY_ID,
        TenantCacheNames.TENANT_BY_CODE,
        TenantCacheNames.BOOTSTRAP_BY_ID,
        TenantCacheNames.BOOTSTRAP_BY_CODE,
        TenantCacheNames.ACTIVE_TENANT_IDS
    }, allEntries = true)
    public TenantConfig update(TenantConfig tenant) {
        // UPDATE: fetch existing entity, update it, then save
        // Cache eviction: clear all tenant catalog caches
        var existing = repository.findById(tenant.id().value())
            .orElseThrow(() -> new EntityNotFoundException(
                "Tenant not found with id: " + tenant.id().value()));

        mapper.updateEntity(tenant, existing);
        var saved = repository.save(existing);
        return mapper.toDomain(saved);
    }

}
