package com.tchalanet.server.core.tenant.application.port.out;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.tenant.domain.model.Tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantReaderPort {
    Optional<Tenant> findById(TenantId id);
    Optional<Tenant> findByCode(String codeLower);
    boolean existsByCode(String codeLower);
    List<TenantId> listActiveTenantIds();
}
