package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.platform.tenant.api.cache.TenantCacheNames;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.internal.mapper.TenantMapper;
import com.tchalanet.server.platform.tenant.internal.persistence.TenantJpaRepository;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence adapter for TenantConfig.
 * Implements write port with separate create/update methods.
 * Per DOMAIN_TENANT_CONFIG.md:
 * - No RLS on tenant table (platform registry)
 * - Uses typed IDs (TenantId)
 * - Maps domain ↔ entity via TenantMapper
 * - create() for INSERT only
 * - update() for UPDATE only (throws EntityNotFoundException if not found)
 * - getRequiredByIdActive() for direct required reads in application services
 * - Evicts catalog/tenant caches on write (per user request)
 */
@Component
@RequiredArgsConstructor
public class TenantPersistenceAdapter {

    private final TenantJpaRepository repository;
    private final TenantMapper mapper;

    @CacheEvict(cacheNames = {
        TenantCacheNames.REGISTRY_BY_ID,
        TenantCacheNames.REGISTRY_BY_CODE,
        TenantCacheNames.ACTIVE_TENANT_IDS
    }, allEntries = true)
    public TenantConfig create(TenantConfig tenant) {
        var entity = mapper.toEntity(tenant);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @CacheEvict(cacheNames = {
        TenantCacheNames.REGISTRY_BY_ID,
        TenantCacheNames.REGISTRY_BY_CODE,
        TenantCacheNames.ACTIVE_TENANT_IDS
    }, allEntries = true)
    public TenantConfig update(TenantConfig tenant) {
        // UPDATE: fetch existing entity, update it, then save
        var existing = repository.findById(tenant.id().value())
            .orElseThrow(() -> new EntityNotFoundException(
                "Tenant not found with id: " + tenant.id().value()));

        mapper.updateEntity(tenant, existing);
        var saved = repository.save(existing);
        return mapper.toDomain(saved);
    }

    public Optional<TenantConfig> findByIdActive(TenantId tenantId) {
        return repository.findByIdActive(tenantId.value()).map(mapper::toDomain);
    }

    public Optional<TenantConfig> findByCodeActive(String code) {
        return repository.findByCodeActive(code).map(mapper::toDomain);
    }

    public TenantConfig getRequiredByIdActive(TenantId tenantId) {
        return mapper.toDomain(repository.getRequiredByIdActive(tenantId.value()));
    }

    @Transactional
    @CacheEvict(cacheNames = {
        TenantCacheNames.REGISTRY_BY_ID,
        TenantCacheNames.REGISTRY_BY_CODE,
        TenantCacheNames.ACTIVE_TENANT_IDS
    }, allEntries = true)
    public void updateDefaultCommissionRate(TenantId tenantId, BigDecimal rate) {
        int updated = repository.updateDefaultCommissionRate(tenantId.value(), rate);
        if (updated == 0) {
            throw new EntityNotFoundException("Tenant not found: " + tenantId.value());
        }
    }
}
