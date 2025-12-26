package com.tchalanet.server.core.tenant.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

/** Command to archive (soft-delete) a tenant. */
public record ArchiveTenantCommand(TenantId tenantId, String reason) implements Command<Void> {}
