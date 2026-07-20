package com.tchalanet.server.core.analytics.api.command;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReconcileAnalyticsCommandTest {

  private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
  private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
  private static final LocalDate TO = LocalDate.of(2026, 7, 20);

  @Test
  void validateDoesNotRequireARemediationReason() {
    new ReconcileAnalyticsCommand(TENANT_ID, FROM, TO, AnalyticsReconciliationMode.VALIDATE, null);
  }

  @Test
  void rebuildRequiresAnOperationalReason() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new ReconcileAnalyticsCommand(
                    TENANT_ID, FROM, TO, AnalyticsReconciliationMode.REBUILD_AND_VALIDATE, " "))
        .withMessage("repairReason is required for REBUILD_AND_VALIDATE");
  }

  @Test
  void reconciliationIsAlwaysTenantScoped() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new ReconcileAnalyticsCommand(
                    null, FROM, TO, AnalyticsReconciliationMode.VALIDATE, null))
        .withMessage("tenantId must not be null");
  }

  @Test
  void reconciliationWindowIsBoundedToNinetyDays() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new ReconcileAnalyticsCommand(
                    TENANT_ID, FROM, FROM.plusDays(91), AnalyticsReconciliationMode.VALIDATE, null))
        .withMessage("Reconciliation window exceeds 90 days; split the request");
  }
}
