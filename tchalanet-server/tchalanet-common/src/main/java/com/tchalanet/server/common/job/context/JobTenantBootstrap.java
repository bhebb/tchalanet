package com.tchalanet.server.common.job.context;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

public record JobTenantBootstrap(
    TenantId tenantId,
    String code,
    Currency currency,
    ZoneId timezone
) {}
