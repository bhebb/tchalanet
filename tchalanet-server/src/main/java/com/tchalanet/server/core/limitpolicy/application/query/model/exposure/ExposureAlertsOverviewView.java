package com.tchalanet.server.core.limitpolicy.application.query.model.exposure;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;

public record ExposureAlertsOverviewView(
    TenantId tenantId,
    DrawId drawId,
    String scopeKey,
    List<ExposureAlertItemView> items
) {}
