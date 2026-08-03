package com.tchalanet.server.core.sales.api.model.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTraceTest {

  private static final Instant PAID_AT = Instant.parse("2026-08-03T10:00:00Z");
  private static final UserId PAID_BY =
      UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final UserId ADJUSTED_BY =
      UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000002"));

  @Test
  void adjustmentKeepsOriginalPaymentAndRecordsTheEffectiveAmount() {
    var original = PaymentTrace.of(PAID_AT, PAID_BY, money("125.00"));

    var adjusted =
        original.adjusted(
            money("120.00"), Instant.parse("2026-08-03T11:00:00Z"), ADJUSTED_BY, "cash error");

    assertThat(adjusted.paidAt()).isEqualTo(PAID_AT);
    assertThat(adjusted.paidBy()).isEqualTo(PAID_BY);
    assertThat(adjusted.paidAmount().amount()).isEqualByComparingTo("120.00");
    assertThat(adjusted.adjustedBy()).isEqualTo(ADJUSTED_BY);
    assertThat(adjusted.adjustmentReason()).isEqualTo("cash error");
  }

  @Test
  void adjustmentRequiresAnAuditReason() {
    var original = PaymentTrace.of(PAID_AT, PAID_BY, money("125.00"));

    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                original.adjusted(
                    money("120.00"),
                    Instant.parse("2026-08-03T11:00:00Z"),
                    ADJUSTED_BY,
                    " "))
        .withMessage("Payment adjustment reason is required");
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), CurrencyCode.of("HTG"));
  }
}
