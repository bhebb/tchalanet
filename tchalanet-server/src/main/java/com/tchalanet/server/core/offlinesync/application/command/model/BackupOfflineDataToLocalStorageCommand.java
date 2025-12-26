package com.tchalanet.server.core.offlinesync.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record BackupOfflineDataToLocalStorageCommand(TenantId tenantId, UUID deviceId) {}

