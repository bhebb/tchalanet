package com.tchalanet.server.tenant.domain.ports;

import com.tchalanet.server.tenant.domain.model.Tenant;
import com.tchalanet.server.tenant.domain.model.TenantId;
import java.util.Optional;

/** Port de persistance pour le domaine tenant. */
public interface TenantRepository {

  Optional<Tenant> findById(TenantId id);

  Tenant save(Tenant tenant);
}
