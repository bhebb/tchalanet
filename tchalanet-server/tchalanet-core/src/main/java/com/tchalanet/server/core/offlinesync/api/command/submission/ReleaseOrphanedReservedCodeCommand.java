package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Forensic recovery: a code stayed RESERVED past the SLA without a return event. Transition
 * it to {@code CONSUMED_REJECTED} (never back to AVAILABLE — invariant of the spec v2.1)
 * and mark the linked submission {@code SYNC_FAILED} if any.
 */
public record ReleaseOrphanedReservedCodeCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineCodeId codeId
) implements Command<Void> {}
