package com.tchalanet.server.core.sales.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CustomerTicketStatusResolver")
class CustomerTicketStatusResolverTest {

  private final CustomerTicketStatusResolver resolver = new CustomerTicketStatusResolver();

  @Nested
  @DisplayName("Sale-level statuses take precedence")
  class SalePrecedence {

    @Test
    @DisplayName("CANCELLED sale → CANCELLED regardless of result")
    void cancelled() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.CANCELLED, TicketResultStatus.WON, TicketSettlementStatus.PAID))
          .isEqualTo(CustomerTicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("VOIDED sale → VOIDED")
    void voided() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.VOIDED,
                  TicketResultStatus.NOT_RESULTED,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.VOIDED);
    }

    @Test
    @DisplayName("REJECTED sale → VOIDED")
    void rejected() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.REJECTED,
                  TicketResultStatus.NOT_RESULTED,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.VOIDED);
    }
  }

  @Nested
  @DisplayName("Result-level statuses")
  class ResultStatuses {

    @Test
    @DisplayName("OVERRIDDEN → CORRECTED")
    void overridden() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.OVERRIDDEN,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.CORRECTED);
    }

    @Test
    @DisplayName("NOT_RESULTED → AWAITING_RESULT")
    void notResulted() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.NOT_RESULTED,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.AWAITING_RESULT);
    }

    @Test
    @DisplayName("PENDING → AWAITING_RESULT")
    void pending() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.PENDING,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.AWAITING_RESULT);
    }

    @Test
    @DisplayName("LOST → LOST")
    void lost() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.LOST,
                  TicketSettlementStatus.NO_PAYOUT))
          .isEqualTo(CustomerTicketStatus.LOST);
    }

    @Test
    @DisplayName("VOID result → LOST")
    void voidResult() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.VOID,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.LOST);
    }
  }

  @Nested
  @DisplayName("Won + settlement statuses")
  class WonSettlement {

    @Test
    @DisplayName("WON + PAID → WON_PAID")
    void wonPaid() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED, TicketResultStatus.WON, TicketSettlementStatus.PAID))
          .isEqualTo(CustomerTicketStatus.WON_PAID);
    }

    @Test
    @DisplayName("WON + SETTLED → WON_PAID")
    void wonSettled() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.WON,
                  TicketSettlementStatus.SETTLED))
          .isEqualTo(CustomerTicketStatus.WON_PAID);
    }

    @Test
    @DisplayName("WON + PAYOUT_PENDING → WON_CLAIMABLE")
    void wonPayoutPending() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.WON,
                  TicketSettlementStatus.PAYOUT_PENDING))
          .isEqualTo(CustomerTicketStatus.WON_CLAIMABLE);
    }

    @Test
    @DisplayName("WON + NOT_SETTLED → WON_CLAIMABLE")
    void wonNotSettled() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.WON,
                  TicketSettlementStatus.NOT_SETTLED))
          .isEqualTo(CustomerTicketStatus.WON_CLAIMABLE);
    }

    @Test
    @DisplayName("WON + REVERSED → WON_CLAIMABLE")
    void wonReversed() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.WON,
                  TicketSettlementStatus.REVERSED))
          .isEqualTo(CustomerTicketStatus.WON_CLAIMABLE);
    }

    @Test
    @DisplayName("WON + NO_PAYOUT → WON_CLAIMABLE")
    void wonNoPayout() {
      assertThat(
              resolver.resolve(
                  TicketSaleStatus.APPROVED,
                  TicketResultStatus.WON,
                  TicketSettlementStatus.NO_PAYOUT))
          .isEqualTo(CustomerTicketStatus.WON_CLAIMABLE);
    }
  }

  @Test
  @DisplayName("PENDING_APPROVAL + WON resolves through the WON path")
  void pendingApprovalWonResolvesNormally() {
    assertThat(
            resolver.resolve(
                TicketSaleStatus.PENDING_APPROVAL,
                TicketResultStatus.WON,
                TicketSettlementStatus.PAID))
        .isEqualTo(CustomerTicketStatus.WON_PAID);
  }
}
