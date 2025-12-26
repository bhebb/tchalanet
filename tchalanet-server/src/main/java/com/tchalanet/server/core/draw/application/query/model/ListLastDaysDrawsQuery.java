package com.tchalanet.server.core.draw.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record ListLastDaysDrawsQuery(TenantId tenantId, String channelCode, int days) {}
