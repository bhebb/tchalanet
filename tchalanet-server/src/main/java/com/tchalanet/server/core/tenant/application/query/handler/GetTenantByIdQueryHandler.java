package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenant.application.query.model.TenantDto;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTenantByIdQueryHandler implements QueryHandler<GetTenantByIdQuery, TenantDto> {

    private final TenantReaderPort repo;

    @Override
    public TenantDto handle(GetTenantByIdQuery q) {
        var t = repo.findById(q.tenantId()).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        return new TenantDto(
            t.id().value(),
            t.code(),
            t.name(),
            t.type(),
            t.timezone(),
            t.currency(),
            t.status(),
            t.activeThemeId(),
            t.addressId()
        );
    }
}
