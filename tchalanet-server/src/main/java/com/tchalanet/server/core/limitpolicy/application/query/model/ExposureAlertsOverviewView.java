package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;

public record ExposureAlertsOverviewView(
    TenantId tenantId,
    DrawId drawId,
    String scopeKey,
    List<com.tchalanet.server.core.limitpolicy.application.query.model.ExposureAlertItemView> items
) {}
