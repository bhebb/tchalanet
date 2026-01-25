package com.tchalanet.server.core.tenantconfig.infra.persistence;

import com.tchalanet.server.catalog.tenant.api.cache.TenantCacheNames;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import com.tchalanet.server.core.tenantconfig.infra.persistence.mapper.TenantMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
public class TenantPersistenceAdapter implements TenantConfigWriterPort {

    private final TenantRepository repository;
    private final TenantMapper mapper;

    @Override
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

    @Override
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

