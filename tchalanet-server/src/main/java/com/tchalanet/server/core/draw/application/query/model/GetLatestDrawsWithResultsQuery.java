package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZonedDateTime;

public record GetNextDrawQuery(TenantId tenantId, String channelCode, ZonedDateTime now) {}
