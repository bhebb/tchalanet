package com.tchalanet.server.core.draw.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record GetNextDrawsQuery(TenantId tenantId, ZonedDateTime now, int limit) {}
