package com.tchalanet.server.app.batch.context;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

public record BatchTenantBootstrap(
    TenantId tenantId, String code, ZoneId timezone, Currency currency) {}
