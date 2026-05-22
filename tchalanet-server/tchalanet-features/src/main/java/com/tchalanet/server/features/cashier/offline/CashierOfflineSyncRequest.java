package com.tchalanet.server.features.cashier.offline;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Cashier-facing payload for {@code POST /tenant/cashier/offline/submissions}. Mirrors
 * {@link SyncOfflineSalesCommand} minus {@code tenantId} (taken from request context) and
 * {@code trustedOperationalContext} (derived server-side).
 */
public record CashierOfflineSyncRequest(
    @NotNull OfflineGrantId grantId,
    @NotBlank String clientBatchId,
    @NotBlank String batchPayloadHash,
    @NotEmpty List<SyncOfflineSalesCommand.Submission> submissions
) {}
