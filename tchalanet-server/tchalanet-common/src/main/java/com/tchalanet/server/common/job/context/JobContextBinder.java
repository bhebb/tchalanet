package com.tchalanet.server.common.job.context;

import com.tchalanet.server.common.types.id.TenantId;

public interface JobContextBinder {

    void bindPlatform(String actor);

    void bindTenant(TenantId tenantId, String actor);

    void clear();
}
