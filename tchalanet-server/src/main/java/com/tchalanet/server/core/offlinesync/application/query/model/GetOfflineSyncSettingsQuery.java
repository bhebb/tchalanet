package com.tchalanet.server.core.offlinesync.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record GetOfflineSyncSettingsQuery(TenantId tenantId, UUID deviceId) {}

