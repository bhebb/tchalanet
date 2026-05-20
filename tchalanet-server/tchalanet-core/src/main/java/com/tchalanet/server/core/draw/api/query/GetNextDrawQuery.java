package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZonedDateTime;

public record GetNextDrawQuery(TenantId tenantId, String channelCode, ZonedDateTime now) {}
