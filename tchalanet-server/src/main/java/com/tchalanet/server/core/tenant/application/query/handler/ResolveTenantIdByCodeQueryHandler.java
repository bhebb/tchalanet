package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.query.model.ResolveTenantIdByCodeQuery;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import com.tchalanet.server.core.tenant.infra.cache.TenantCache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolveTenantIdByCodeQueryHandler implements QueryHandler<ResolveTenantIdByCodeQuery, Optional<UUID>> {

    private final TenantReaderPort repo;
    private final TenantCache cache;

    @Override
    @Cacheable(value = "tenantCodeToId", unless = "#result==null || #result.isEmpty()")
    public Optional<UUID> handle(ResolveTenantIdByCodeQuery tenantCodeQuery) {
        String codeLower = tenantCodeQuery.code().trim().toLowerCase();

        Optional<TenantId> cached = cache.findTenantIdByCode(codeLower);
        if (cached.isPresent()) return Optional.of(cached.get().value());

        var tenant = repo.findByCode(codeLower).orElseThrow(() -> new IllegalArgumentException("Tenant not found for code=" + codeLower));
        return Optional.of(tenant.id().value());
    }
}


