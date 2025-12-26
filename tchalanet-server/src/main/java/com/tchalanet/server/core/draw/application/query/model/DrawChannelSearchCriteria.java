package com.tchalanet.server.core.draw.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record DrawChannelSearchCriteria(TenantId tenantId, Boolean activeOnly) {}
