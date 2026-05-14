package com.tchalanet.server.common.job.context;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public interface JobTenantBootstrapProvider {

    Optional<JobTenantBootstrap> findBootstrapById(TenantId tenantId);
}
