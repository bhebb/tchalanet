package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import org.junit.jupiter.api.Test;

class SalePreparationOrchestratorTest {

  @Test
  void v0BlocksBothBlockedAndApprovalRequiredLimitOutcomes() {
    assertThat(SalePreparationOrchestrator.isBlockedInV0(BreachOutcome.ALLOW)).isFalse();
    assertThat(SalePreparationOrchestrator.isBlockedInV0(BreachOutcome.WARN)).isFalse();
    assertThat(SalePreparationOrchestrator.isBlockedInV0(BreachOutcome.BLOCK)).isTrue();
    assertThat(SalePreparationOrchestrator.isBlockedInV0(BreachOutcome.REQUIRE_APPROVAL)).isTrue();
  }
}
