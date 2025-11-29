package com.tchalanet.server.core.tenant.domain.ports;

import com.tchalanet.server.core.tenant.domain.model.Tenant;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.util.Optional;

/** Port de persistance pour le domaine tenant. */
public interface TenantRepository {

  Optional<Tenant> findById(TenantId id);

  Tenant save(Tenant tenant);
}
