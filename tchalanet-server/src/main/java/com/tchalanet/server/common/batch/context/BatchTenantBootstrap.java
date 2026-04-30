package com.tchalanet.server.common.batch.context;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

public record BatchTenantBootstrap(
    TenantId tenantId, String code, ZoneId timezone, Currency currency) {}
