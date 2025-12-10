package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.tenant.infra.persistence.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolveTenantIdByCodeQueryHandler implements QueryHandler<String, Optional<UUID>> {

    private final TenantJpaRepository tenantRepo;


    @Override
    @Cacheable(value = "tenantCodeToId", unless = "#result==null || #result.isEmpty()")
    public Optional<UUID> handle(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) return Optional.empty();
        return tenantRepo.findByCode(tenantCode).map(BaseEntity::getId);
    }
}

