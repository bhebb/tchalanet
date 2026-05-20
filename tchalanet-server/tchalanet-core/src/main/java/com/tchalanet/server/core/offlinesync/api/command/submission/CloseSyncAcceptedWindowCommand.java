package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Scheduler command that closes the sync-accepted window for stuck RECEIVED submissions. */
public record CloseSyncAcceptedWindowCommand(
    @NotNull TenantId tenantId,
    @NotNull Instant now
) implements Command<Void> {}

