package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;

public record DrawChannelSearchCriteria(TenantId tenantId, Boolean activeOnly) {}
