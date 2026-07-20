package com.tchalanet.server.core.analytics.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import java.time.LocalDate;
import org.springframework.util.StringUtils;

/**
 * Validates one tenant's analytics projections against ticket snapshots.
 *
 * <p>{@link AnalyticsReconciliationMode#VALIDATE} is read-only. {@link
 * AnalyticsReconciliationMode#REBUILD_AND_VALIDATE} requires an operational reason and is only
 * successful after the post-rebuild validation has no mismatch.
 */
public record ReconcileAnalyticsCommand(
    TenantId tenantId,
    LocalDate from,
    LocalDate to,
    AnalyticsReconciliationMode mode,
    String repairReason)
    implements Command<AnalyticsReconciliationResult> {

  public ReconcileAnalyticsCommand {
    if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (from.isAfter(to)) throw new IllegalArgumentException("from must be <= to");
    if (from.plusDays(90).isBefore(to)) {
      throw new IllegalArgumentException(
          "Reconciliation window exceeds 90 days; split the request");
    }
    if (mode == null) throw new IllegalArgumentException("mode must not be null");
    if (mode == AnalyticsReconciliationMode.REBUILD_AND_VALIDATE
        && !StringUtils.hasText(repairReason)) {
      throw new IllegalArgumentException("repairReason is required for REBUILD_AND_VALIDATE");
    }
    repairReason = StringUtils.hasText(repairReason) ? repairReason.trim() : null;
  }
}
